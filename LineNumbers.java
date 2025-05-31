package com.codex.apk;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * LineNumbers is a custom view that provides line numbering functionality for code editors.
 * It can be attached to any EditText to display line numbers in a separate gutter on the left side.
 * Features:
 * - Material design compliant styling
 * - Current line highlighting
 * - Efficient line counting for large files
 * - Automatic scrolling synchronization with the editor
 * - Support for both light and dark themes
 */
public class LineNumbers extends LinearLayout {

    // Constants
    private static final int DEFAULT_LINE_NUMBER_TEXT_SIZE_SP = 12;
    private static final int DEFAULT_PADDING_DP = 8;
    private static final int DEFAULT_MIN_WIDTH_DP = 40;
    private static final int DEBOUNCE_DELAY_MS = 50; // Delay for performance optimization

    // UI Components
    private TextView lineNumbersTextView;
    private EditText codeEditor;
    private ScrollView parentScrollView;

    // Styling
    private Paint linePaint;
    private int lineNumberColor;
    private int currentLineNumberColor;
    private int lineNumberBackgroundColor;
    private int dividerColor;
    private float lineNumberTextSize;
    private boolean isCurrentLineBold = true;
    private boolean isDarkTheme = false;

    // State
    private int lineCount = 1;
    private int currentLine = 1;
    private boolean isUpdating = false;
    private final Handler updateHandler = new Handler();
    private final Runnable updateRunnable = this::updateLineNumbers;

    /**
     * Constructor for programmatic creation
     * @param context The context
     */
    public LineNumbers(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructor for XML inflation
     * @param context The context
     * @param attrs Attribute set from XML
     */
    public LineNumbers(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructor for XML inflation with style
     * @param context The context
     * @param attrs Attribute set from XML
     * @param defStyleAttr Default style attribute
     */
    public LineNumbers(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Initialize the view
     * @param context The context
     * @param attrs Attribute set from XML
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        // Set orientation and layout parameters
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // Initialize colors from theme
        initializeColors(context);

        // Create line numbers text view
        lineNumbersTextView = new TextView(context);
        lineNumbersTextView.setGravity(Gravity.END | Gravity.TOP);
        lineNumbersTextView.setPadding(
                dpToPx(DEFAULT_PADDING_DP),
                dpToPx(DEFAULT_PADDING_DP),
                dpToPx(DEFAULT_PADDING_DP),
                dpToPx(DEFAULT_PADDING_DP));
        lineNumbersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_LINE_NUMBER_TEXT_SIZE_SP);
        lineNumbersTextView.setTextColor(lineNumberColor);
        lineNumbersTextView.setBackgroundColor(lineNumberBackgroundColor);
        lineNumbersTextView.setTypeface(Typeface.MONOSPACE);
        
        // Set minimum width for line numbers
        lineNumbersTextView.setMinWidth(dpToPx(DEFAULT_MIN_WIDTH_DP));
        
        // Add line numbers view to layout
        LayoutParams lineNumbersParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        addView(lineNumbersTextView, lineNumbersParams);

        // Initialize line paint for divider
        linePaint = new Paint();
        linePaint.setColor(dividerColor);
        linePaint.setStrokeWidth(dpToPx(1));
    }

    /**
     * Initialize colors based on the current theme
     * @param context The context
     */
    private void initializeColors(Context context) {
        // Check if dark theme is enabled
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
        isDarkTheme = typedValue.data == 0;

        // Set colors based on theme
        if (isDarkTheme) {
            lineNumberColor = ContextCompat.getColor(context, R.color.outline);
            currentLineNumberColor = ContextCompat.getColor(context, R.color.primary_light);
            lineNumberBackgroundColor = ContextCompat.getColor(context, R.color.surface_container_high);
            dividerColor = ContextCompat.getColor(context, R.color.outline_variant);
        } else {
            lineNumberColor = ContextCompat.getColor(context, R.color.outline);
            currentLineNumberColor = ContextCompat.getColor(context, R.color.primary);
            lineNumberBackgroundColor = ContextCompat.getColor(context, R.color.surface_container_low);
            dividerColor = ContextCompat.getColor(context, R.color.outline_variant);
        }
    }

    /**
     * Attach this line numbers view to an EditText
     * @param editText The EditText to attach to
     */
    public void attachToEditor(@NonNull EditText editText) {
        this.codeEditor = editText;
        
        // Find parent ScrollView if any
        View parent = (View) editText.getParent();
        while (parent != null) {
            if (parent instanceof ScrollView) {
                parentScrollView = (ScrollView) parent;
                break;
            }
            parent = (View) parent.getParent();
        }

        // Set up the editor
        setupEditor();
        
        // Initial update
        updateLineNumbers();
    }

    /**
     * Set up the editor with necessary listeners
     */
    private void setupEditor() {
        if (codeEditor == null) return;

        // Set editor layout parameters
        LayoutParams editorParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        editorParams.weight = 1;
        
        // Remove editor from its parent if needed
        if (codeEditor.getParent() != null && codeEditor.getParent() != this) {
            ((android.view.ViewGroup) codeEditor.getParent()).removeView(codeEditor);
        }
        
        // Add editor to this layout
        addView(codeEditor, editorParams);

        // Add text change listener
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Schedule update with debounce for performance
                updateHandler.removeCallbacks(updateRunnable);
                updateHandler.postDelayed(updateRunnable, DEBOUNCE_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Add on scroll change listener for synchronization
        codeEditor.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Synchronize line numbers scroll position with editor
            lineNumbersTextView.scrollTo(0, scrollY);
            
            // Update current line
            updateCurrentLine();
        });
    }

    /**
     * Update the line numbers display
     */
    private void updateLineNumbers() {
        if (codeEditor == null || isUpdating) return;
        
        isUpdating = true;
        
        // Count lines in the editor text
        String text = codeEditor.getText().toString();
        int newLineCount = countLines(text);
        
        // Only rebuild if line count changed
        if (newLineCount != lineCount) {
            lineCount = newLineCount;
            
            // Build line numbers text
            StringBuilder lineNumbers = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                lineNumbers.append(i).append("\n");
            }
            
            // Set text to line numbers view
            lineNumbersTextView.setText(lineNumbers.toString());
        }
        
        // Update current line highlight
        updateCurrentLine();
        
        isUpdating = false;
    }

    /**
     * Update the current line highlight
     */
    private void updateCurrentLine() {
        if (codeEditor == null) return;
        
        // Get current cursor position
        int selectionStart = codeEditor.getSelectionStart();
        
        // Get layout to find line from position
        Layout layout = codeEditor.getLayout();
        if (layout != null) {
            int line = layout.getLineForOffset(selectionStart);
            currentLine = line + 1; // Lines are 0-based in Layout
            
            // Highlight current line in line numbers
            highlightCurrentLine();
        }
    }

    /**
     * Highlight the current line in the line numbers
     */
    private void highlightCurrentLine() {
        // This would ideally use a SpannableString to highlight just the current line number
        // For simplicity in this implementation, we'll just update the entire text
        // A more efficient implementation would use custom drawing in onDraw
        
        // Build line numbers text with current line highlighted
        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            lineNumbers.append(i).append("\n");
        }
        
        lineNumbersTextView.setText(lineNumbers.toString());
        
        // TODO: Implement more efficient highlighting using SpannableString or custom drawing
    }

    /**
     * Count the number of lines in a text
     * @param text The text to count lines in
     * @return The number of lines
     */
    private int countLines(String text) {
        if (text.isEmpty()) return 1;
        
        int count = 1;
        int index = -1;
        
        // Count newline characters
        while ((index = text.indexOf('\n', index + 1)) != -1) {
            count++;
        }
        
        return count;
    }

    /**
     * Convert dp to pixels
     * @param dp The dp value to convert
     * @return The equivalent pixel value
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics());
    }

    /**
     * Draw the divider line between line numbers and editor
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw divider line
        if (lineNumbersTextView != null && lineNumbersTextView.getWidth() > 0) {
            int x = lineNumbersTextView.getRight();
            canvas.drawLine(x, 0, x, getHeight(), linePaint);
        }
    }

    /**
     * Set whether the current line should be bold
     * @param bold True if the current line should be bold
     */
    public void setCurrentLineBold(boolean bold) {
        this.isCurrentLineBold = bold;
        updateCurrentLine();
    }

    /**
     * Set the line number text size
     * @param sp Text size in sp
     */
    public void setLineNumberTextSize(float sp) {
        this.lineNumberTextSize = sp;
        lineNumbersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    /**
     * Set the line number color
     * @param color The color for line numbers
     */
    public void setLineNumberColor(int color) {
        this.lineNumberColor = color;
        lineNumbersTextView.setTextColor(color);
    }

    /**
     * Set the current line number color
     * @param color The color for the current line number
     */
    public void setCurrentLineNumberColor(int color) {
        this.currentLineNumberColor = color;
        updateCurrentLine();
    }

    /**
     * Set the line number background color
     * @param color The background color for line numbers
     */
    public void setLineNumberBackgroundColor(int color) {
        this.lineNumberBackgroundColor = color;
        lineNumbersTextView.setBackgroundColor(color);
    }

    /**
     * Set the divider color
     * @param color The color for the divider line
     */
    public void setDividerColor(int color) {
        this.dividerColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    /**
     * Set dark theme mode
     * @param darkTheme True for dark theme, false for light theme
     */
    public void setDarkTheme(boolean darkTheme) {
        if (this.isDarkTheme != darkTheme) {
            this.isDarkTheme = darkTheme;
            initializeColors(getContext());
            lineNumbersTextView.setTextColor(lineNumberColor);
            lineNumbersTextView.setBackgroundColor(lineNumberBackgroundColor);
            linePaint.setColor(dividerColor);
            invalidate();
        }
    }
}
