package com.codex.apk.CodeEditor;

import android.content.Context;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.LruCache;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.codex.apk.CodeEditor.highlighters.CssHighlighter;
import com.codex.apk.CodeEditor.highlighters.DiffHighlighter;
import com.codex.apk.CodeEditor.highlighters.HtmlHighlighter;
import com.codex.apk.CodeEditor.highlighters.JavaScriptHighlighter;
import com.codex.apk.CodeEditor.highlighters.JsonHighlighter;

import com.codex.apk.CodeEditorView; // Import CodeEditorView from its original location

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced OptimizedSyntaxHighlighter provides efficient syntax highlighting for code editors.
 * It uses background threads, caching, and only highlights visible text to improve performance.
 * Now modularized into smaller components, specifically supporting HTML, CSS, JavaScript, and JSON.
 */
public class OptimizedSyntaxHighlighter {

    // Constants
    private static final int DEBOUNCE_DELAY_MS = 100;
    private static final int CACHE_SIZE = 100;
    private static final int VISIBLE_WINDOW_EXPANSION = 2000;
    private static final String TAG = "OptimizedSyntaxHighlighter";

    // Syntax type enum - Reduced to supported languages only
    public enum SyntaxType {
        HTML, CSS, JAVASCRIPT, JSON, DIFF
    }

    // UI Components
    private CodeEditorView.CodeEditText codeEditor;
    private boolean isDiffView;

    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Runnable updateRunnable;
    private boolean isHighlighting = false;

    // State
    private int lastProcessedHash = 0;
    private int lastVisibleStart = 0;
    private int lastVisibleEnd = 0;

    // Caching for full document highlighting
    private final LruCache<String, SpannableStringBuilder> highlightCache;

    // TextWatcher instance to be managed per attachment
    private TextWatcher textWatcher;

    // Modularized components - Only highlighters for supported languages
    private final SyntaxColorProvider colorProvider;
    private final DiffHighlighter diffHighlighter;
    private HtmlHighlighter htmlHighlighter;
    private CssHighlighter cssHighlighter;
    private JavaScriptHighlighter javaScriptHighlighter;
    private JsonHighlighter jsonHighlighter;

    private SyntaxType currentSyntaxType;

    /**
     * Constructor
     * @param context The application context.
     * @param syntaxType The initial syntax type to highlight.
     * @param isDiffView A boolean to indicate if the highlighter is for a diff view.
     */
    public OptimizedSyntaxHighlighter(@NonNull Context context, SyntaxType syntaxType, boolean isDiffView) {
        this.isDiffView = isDiffView;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.updateRunnable = this::highlightSyntax;
        this.highlightCache = new LruCache<>(CACHE_SIZE);

        this.colorProvider = new SyntaxColorProvider(context);
        this.diffHighlighter = new DiffHighlighter(colorProvider);
        this.currentSyntaxType = syntaxType;
        this.colorProvider.initializeSyntaxColors(currentSyntaxType); // Initialize colors for the initial syntax type
        initializeHighlighters();
    }

    /**
     * Initializes all language-specific highlighter instances.
     * Only initializes highlighters for HTML, CSS, JavaScript, and JSON.
     */
    private void initializeHighlighters() {
        htmlHighlighter = new HtmlHighlighter(colorProvider);
        cssHighlighter = new CssHighlighter(colorProvider);
        javaScriptHighlighter = new JavaScriptHighlighter(colorProvider);
        jsonHighlighter = new JsonHighlighter(colorProvider);
    }

    /**
     * Attach this highlighter to an EditText.
     * This method will decide whether to apply syntax highlighting or diff highlighting.
     * @param editText The CodeEditText to attach the highlighter to.
     */
    public void attachToEditor(@NonNull CodeEditorView.CodeEditText editText) {
        this.codeEditor = editText;

        // Remove any existing TextWatcher from previous attachments to prevent duplicates
        if (textWatcher != null) {
            codeEditor.removeTextChangedListener(textWatcher);
        }

        // Add a new TextWatcher for this attachment
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Schedule update with debounce for performance
                mainHandler.removeCallbacks(updateRunnable);
                mainHandler.postDelayed(updateRunnable, DEBOUNCE_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        };
        codeEditor.addTextChangedListener(textWatcher);

        // Initial highlighting based on current syntax type
        if (currentSyntaxType == SyntaxType.DIFF) {
            diffHighlighter.highlight(codeEditor.getEditableText());
        } else {
            highlightSyntax(true); // Force immediate highlight on attachment
        }

        // Add scroll listener to prioritize visible text highlighting
        codeEditor.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Only update visible window if it's not a diff view
            if (currentSyntaxType != SyntaxType.DIFF) {
                updateVisibleWindow();
            }
        });
    }

    /**
     * Update the visible text window for prioritized highlighting.
     */
    private void updateVisibleWindow() {
        if (codeEditor == null) return;

        Layout layout = codeEditor.getLayout();
        if (layout == null) return;

        int scrollY = codeEditor.getScrollY();
        int height = codeEditor.getHeight();

        int firstVisibleLine = layout.getLineForVertical(scrollY);
        int lastVisibleLine = layout.getLineForVertical(scrollY + height);

        final int finalVisibleStart = layout.getLineStart(firstVisibleLine);
        final int finalVisibleEnd = layout.getLineEnd(lastVisibleLine);

        // Only update if visible window has changed significantly
        if (Math.abs(finalVisibleStart - lastVisibleStart) > 100 || Math.abs(finalVisibleEnd - lastVisibleEnd) > 100) {
            lastVisibleStart = finalVisibleStart;
            lastVisibleEnd = finalVisibleEnd;

            // Prioritize highlighting of visible text
            highlightVisibleText();
        }
    }

    /**
     * Highlight only the visible text for immediate feedback.
     */
    private void highlightInternal(final String text, final int start, final int end, final String cacheKey, final boolean isVisible) {
        if (codeEditor == null || isHighlighting || currentSyntaxType == SyntaxType.DIFF) return;

        SpannableStringBuilder cachedSpannable = highlightCache.get(cacheKey);
        if (cachedSpannable != null) {
            applySpansToEditable(codeEditor.getEditableText(), cachedSpannable, start, end);
            return;
        }

        isHighlighting = true;

        executorService.execute(() -> {
            try {
                final String subText = text.substring(start, end);
                SpannableStringBuilder spannable = new SpannableStringBuilder(subText);
                applySyntaxHighlightingInternal(spannable, currentSyntaxType);

                highlightCache.put(cacheKey, spannable);

                mainHandler.post(() -> {
                    if (codeEditor != null) {
                        applySpansToEditable(codeEditor.getEditableText(), spannable, start, end);
                    }
                    isHighlighting = false;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error highlighting text: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    isHighlighting = false;
                });
            }
        });
    }

    private void highlightVisibleText() {
        if (codeEditor == null || isHighlighting || currentSyntaxType == SyntaxType.DIFF) return;

        // Expand visible window for smoother scrolling
        final int expandedStart = Math.max(0, lastVisibleStart - VISIBLE_WINDOW_EXPANSION);
        final int expandedEnd = Math.min(codeEditor.length(), lastVisibleEnd + VISIBLE_WINDOW_EXPANSION);

        final String text = codeEditor.getText().toString();
        // Ensure the substring range is valid
        final int actualExpandedStart = expandedStart;
        final int actualExpandedEnd = expandedEnd;

        // Defensive check: if range is invalid, return.
        if (actualExpandedStart > actualExpandedEnd || actualExpandedStart < 0 || actualExpandedEnd > text.length()) {
            return;
        }

        final String visibleText = text.substring(actualExpandedStart, actualExpandedEnd);
        final int visibleTextHash = visibleText.hashCode();
        final String cacheKey = currentSyntaxType.name() + "_visible_" + visibleTextHash;

        highlightInternal(text, actualExpandedStart, actualExpandedEnd, cacheKey, true);
    }

    /**
     * Applies spans from a source SpannableStringBuilder to a target Editable,
     * within a specified range, preserving cursor position.
     * @param targetEditable The Editable to apply spans to.
     * @param sourceSpannable The SpannableStringBuilder containing the spans.
     * @param startOffset The starting offset in the targetEditable where spans should be applied.
     * @param endOffset The ending offset in the targetEditable.
     */
    private void applySpansToEditable(Editable targetEditable, SpannableStringBuilder sourceSpannable, int startOffset, int endOffset) {
        if (targetEditable == null) return;

        int selectionStart = codeEditor.getSelectionStart();
        int selectionEnd = codeEditor.getSelectionEnd();

        // Remove existing ForegroundColorSpans in the target range to prevent overlap/duplicates
        ForegroundColorSpan[] existingSpans = targetEditable.getSpans(startOffset, endOffset, ForegroundColorSpan.class);
        for (ForegroundColorSpan span : existingSpans) {
            targetEditable.removeSpan(span);
        }

        // Apply new spans from the source spannable to the target editable with the correct offset
        ForegroundColorSpan[] newSpans = sourceSpannable.getSpans(0, sourceSpannable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : newSpans) {
            int spanStart = sourceSpannable.getSpanStart(span);
            int spanEnd = sourceSpannable.getSpanEnd(span);

            // Ensure the span application is within the bounds of the targetEditable
            int applyStart = startOffset + spanStart;
            int applyEnd = startOffset + spanEnd;

            // Clip the apply range to the actual bounds of the targetEditable
            applyStart = Math.max(0, Math.min(applyStart, targetEditable.length()));
            applyEnd = Math.max(0, Math.min(applyEnd, targetEditable.length()));

            if (applyStart < applyEnd) { // Only apply if the span has a valid length
                targetEditable.setSpan(
                        new ForegroundColorSpan(span.getForegroundColor()),
                        applyStart,
                        applyEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Restore selection, ensuring it's within the new bounds of the text
        codeEditor.setSelection(
                Math.min(selectionStart, targetEditable.length()),
                Math.min(selectionEnd, targetEditable.length()));
    }


    /**
     * Highlight syntax in the editor (full document).
     * @param forceHighlight If true, forces a re-highlight even if text hash hasn't changed.
     */
    public void highlightSyntax(boolean forceHighlight) {
        if (codeEditor == null || isHighlighting || currentSyntaxType == SyntaxType.DIFF) return;

        final String text = codeEditor.getText().toString();
        final int textHash = text.hashCode();

        // If not forced and content hasn't changed, no need to re-highlight
        if (!forceHighlight && textHash == lastProcessedHash) return;

        lastProcessedHash = textHash;
        final String cacheKey = currentSyntaxType.name() + "_full_" + textHash;
        highlightInternal(text, 0, text.length(), cacheKey, false);
    }

    /**
     * Overloaded method for convenience, defaults to not forcing a highlight.
     */
    public void highlightSyntax() {
        highlightSyntax(false);
    }

    /**
     * Internal method to apply syntax highlighting based on the given syntax type.
     * Delegates to the appropriate language-specific highlighter.
     * @param spannable The SpannableStringBuilder to apply highlighting to.
     * @param type The SyntaxType to use for highlighting.
     */
    private void applySyntaxHighlightingInternal(SpannableStringBuilder spannable, SyntaxType type) {
        // Clear existing spans from the spannableBuilder before applying new ones
        ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            spannable.removeSpan(span);
        }

        // Delegate highlighting to the specific highlighter based on syntax type
        switch (type) {
            case HTML:
                if (htmlHighlighter != null) htmlHighlighter.highlight(spannable);
                break;
            case CSS:
                if (cssHighlighter != null) cssHighlighter.highlight(spannable);
                break;
            case JAVASCRIPT:
                if (javaScriptHighlighter != null) javaScriptHighlighter.highlight(spannable);
                break;
            case JSON:
                if (jsonHighlighter != null) jsonHighlighter.highlight(spannable);
                break;
            case DIFF:
                // Diff highlighting is handled by highlightDiff method, not here
                break;
            // No cases for other languages as they are removed
        }
    }

    /**
     * Applies diff highlighting to the given Editable text.
     * @param editable The Editable text representing a unified diff.
     */
    public void highlightDiff(Editable editable) {
        if (diffHighlighter != null) {
            diffHighlighter.highlight(editable);
        }
    }

    /**
     * Set dark theme mode for the highlighter and trigger re-highlighting.
     * @param darkTheme True for dark theme, false for light theme.
     */
    public void setDarkTheme(boolean darkTheme) {
        // Propagate theme change to color provider
        colorProvider.setDarkTheme(darkTheme);
        // Re-initialize syntax colors for the current syntax type
        colorProvider.initializeSyntaxColors(currentSyntaxType);

        // Re-highlight based on current syntax type (syntax or diff)
        if (isDiffView) {
            highlightDiff(codeEditor.getEditableText());
        } else if (codeEditor != null) {
            highlightSyntax(true); // Force re-highlight
        }
    }

    /**
     * Set syntax type and trigger re-highlighting.
     * @param syntaxType The new SyntaxType.
     */
    public void setSyntaxType(SyntaxType syntaxType) {
        if (this.currentSyntaxType != syntaxType) {
            this.currentSyntaxType = syntaxType;
            // Initialize colors for the new syntax type
            colorProvider.initializeSyntaxColors(currentSyntaxType);

            // Re-highlight based on new syntax type
            if (isDiffView && syntaxType == SyntaxType.DIFF) {
                highlightDiff(codeEditor.getEditableText());
            } else if (codeEditor != null) {
                highlightSyntax(true); // Force re-highlight
            }
        }
    }

    /**
     * Get current syntax type.
     * @return The current SyntaxType.
     */
    public SyntaxType getSyntaxType() {
        return currentSyntaxType;
    }

    /**
     * Detects syntax type from file extension.
     * This method delegates to SyntaxHighlightingUtils.
     * @param fileName The name of the file.
     * @return The detected SyntaxType.
     */
    public static SyntaxType detectSyntaxType(String fileName) {
        return SyntaxHighlightingUtils.detectSyntaxType(fileName);
    }

    /**
     * Clean up resources held by the highlighter.
     */
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Shuts down immediately
        }
        if (codeEditor != null && textWatcher != null) {
            codeEditor.removeTextChangedListener(textWatcher);
        }
        highlightCache.evictAll(); // Clear the cache
    }
}
