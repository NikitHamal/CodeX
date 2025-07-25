package com.codex.apk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

// Updated import for OptimizedSyntaxHighlighter
import com.codex.apk.CodeEditor.OptimizedSyntaxHighlighter;
import com.codex.apk.CodeEditor.SyntaxHighlightingUtils; // Import for detectSyntaxType

/**
 * Enhanced CodeEditorView with fixed line numbers on the left,
 * independent of horizontal code scrolling.
 * Now also supports displaying and highlighting diff content.
 */
public class CodeEditorView extends FrameLayout {

	// Constants
	private static final int LINE_NUMBERS_MIN_WIDTH_DP = 40;
	private static final int DEBOUNCE_DELAY_MS = 100;
	private static final float MIN_TEXT_SIZE_SP = 8f;
	private static final float MAX_TEXT_SIZE_SP = 30f;
	private static final float DEFAULT_TEXT_SIZE_SP = 14f;

	// UI Components
	private CodeEditText codeEditor;
	private LineNumbersView lineNumbersView;
	private NestedScrollView verticalScrollView; // Scrolls the code editor part
	private HorizontalScrollView horizontalScrollView; // Scrolls the code editor horizontally

	// State
	private final Handler updateHandler = new Handler(Looper.getMainLooper());
	private final Runnable updateRunnable = this::updateLineNumbersViewMetrics;
	private float textSize = DEFAULT_TEXT_SIZE_SP;
	private boolean isDarkTheme = false;
	private ScaleGestureDetector scaleGestureDetector;
	private GestureDetector gestureDetector;
	private boolean isZooming = false;
    private boolean isDiffView = false; // Flag to indicate if it's displaying a diff

	// Listeners
	private OnTextChangedListener onTextChangedListener;
    private OptimizedSyntaxHighlighter attachedHighlighter; // Reference to the attached highlighter

	public interface OnTextChangedListener {
		void onTextChanged(String text);
	}

	public CodeEditorView(Context context) {
		super(context);
		init(context);
	}

	public CodeEditorView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CodeEditorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
		isDarkTheme = (typedValue.data == 0);

		// 1. LineNumbersView (fixed on the left)
		lineNumbersView = new LineNumbersView(context);
		LayoutParams lineNumbersParams = new LayoutParams(
		dpToPx(LINE_NUMBERS_MIN_WIDTH_DP),
		LayoutParams.MATCH_PARENT);
		lineNumbersView.setLayoutParams(lineNumbersParams);
		addView(lineNumbersView); // Add directly to CodeEditorView

		// 2. Vertical ScrollView for the code editing area
		verticalScrollView = new NestedScrollView(context);
		LayoutParams verticalScrollParams = new LayoutParams(
		LayoutParams.MATCH_PARENT,
		LayoutParams.MATCH_PARENT);
		verticalScrollParams.leftMargin = dpToPx(LINE_NUMBERS_MIN_WIDTH_DP); // Make space for lineNumbersView
		verticalScrollParams.rightMargin = dpToPx(16); // Add some right margin to avoid scrollbar overlap
		verticalScrollView.setLayoutParams(verticalScrollParams);
		verticalScrollView.setFillViewport(true);
		verticalScrollView.setVerticalScrollBarEnabled(true);
		verticalScrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
		ViewCompat.setNestedScrollingEnabled(verticalScrollView, true);
		addView(verticalScrollView); // Add directly to CodeEditorView

		// 3. Horizontal ScrollView inside Vertical ScrollView
		horizontalScrollView = new HorizontalScrollView(context);
		LayoutParams horizontalScrollParams = new LayoutParams(
		LayoutParams.MATCH_PARENT, // Width should allow content to scroll
		LayoutParams.MATCH_PARENT); // Height matches vertical scroll view
		horizontalScrollView.setLayoutParams(horizontalScrollParams);
		horizontalScrollView.setFillViewport(true);
		horizontalScrollView.setHorizontalScrollBarEnabled(true);
		horizontalScrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
		verticalScrollView.addView(horizontalScrollView);

		// 4. CodeEditText inside HorizontalScrollView
		codeEditor = new CodeEditText(context) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
                // If it's a diff view, consume touch events to prevent editing
                if (isDiffView) {
                    return true;
                }
				if (scaleGestureDetector != null) {
					boolean handledByScale = scaleGestureDetector.onTouchEvent(event);
					if (isZooming) {
						getParent().requestDisallowInterceptTouchEvent(true);
						return true;
					}
					if (handledByScale && scaleGestureDetector.isInProgress()) {
						getParent().requestDisallowInterceptTouchEvent(true);
						return true;
					}
				}
				if (gestureDetector != null) {
					gestureDetector.onTouchEvent(event);
				}
				return super.onTouchEvent(event);
			}

            // Override onTextChanged to notify listener and trigger highlighting
			@Override
			protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
				super.onTextChanged(text, start, lengthBefore, lengthAfter);
				if (onTextChangedListener != null) {
					onTextChangedListener.onTextChanged(text.toString());
				}
                // Trigger highlighting update when text changes, only if not a diff view
                // The highlighter's TextWatcher will handle scheduling the highlight.
                // No direct call to attachedHighlighter.highlightSyntax() here.
			}

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                // The WRAP_CONTENT for width in LayoutParams allows the EditText to measure its content
                // and the HorizontalScrollView will handle the actual scrolling if content is wider.
            }
		};
		LayoutParams editorParams = new FrameLayout.LayoutParams(
		LayoutParams.WRAP_CONTENT, // Crucial for HorizontalScrollView to scroll content
		LayoutParams.MATCH_PARENT); // Match height of its container
		codeEditor.setLayoutParams(editorParams);
		codeEditor.setBackground(null);
		codeEditor.setGravity(Gravity.TOP | Gravity.START);
		codeEditor.setHorizontallyScrolling(true); // Enable internal horizontal scrolling capability
		codeEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI); // Prevent full-screen keyboard on landscape
        codeEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);


		int paddingHorizontal = dpToPx(8);
		int paddingTop = dpToPx(8);
		int paddingBottom = dpToPx(8);
		codeEditor.setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom);
		horizontalScrollView.addView(codeEditor);

		// Setup
		lineNumbersView.setEditText(codeEditor);
		setupListeners();
		setupGestureDetectors();

		getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				getViewTreeObserver().removeOnGlobalLayoutListener(this);
				updateLineNumbersViewMetrics();
			}
		});

        // In the init method, disable parent ViewPager2's user input when editor is focused or being scrolled
        setOnTouchListener((v, event) -> {
            View parent = (View) getParent();
            while (parent != null && !(parent instanceof androidx.viewpager2.widget.ViewPager2)) {
                parent = (View) parent.getParent();
            }
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });
	}

	private void setupGestureDetectors() {
		scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				isZooming = true;
				return true;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				float scaleFactor = detector.getScaleFactor();
				float newTextSize = textSize * scaleFactor;
				newTextSize = Math.max(MIN_TEXT_SIZE_SP, Math.min(newTextSize, MAX_TEXT_SIZE_SP));

				if (Float.compare(textSize, newTextSize) != 0) {
					textSize = newTextSize;
					codeEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
					lineNumbersView.setTextSize(textSize);
					updateLineNumbersViewMetrics();
                    // Re-highlight on text size change to adjust span positions if necessary
                    if (attachedHighlighter != null && !isDiffView) {
                        attachedHighlighter.highlightSyntax(true); // Force re-highlight
                    }
				}
				return true;
			}

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
				isZooming = false;
			}
		});

		gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (Float.compare(textSize, DEFAULT_TEXT_SIZE_SP) != 0) {
					textSize = DEFAULT_TEXT_SIZE_SP;
					codeEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
					lineNumbersView.setTextSize(textSize);
					updateLineNumbersViewMetrics();
                    // Re-highlight on text size change
                    if (attachedHighlighter != null && !isDiffView) {
                        attachedHighlighter.highlightSyntax(true); // Force re-highlight
                    }
				}
				return true;
			}
		});
	}

	private void setupListeners() {
		// Listener to sync LineNumbersView's vertical drawing with CodeEditText's scroll
		verticalScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
		(v, scrollX, scrollY, oldScrollX, oldOldY) -> {
			lineNumbersView.setEditorScrollY(scrollY);
		});
	}

	private void updateLineNumbersViewMetrics() {
		lineNumbersView.updateMetrics();
	}

	public CodeEditText getCodeEditor() { // Changed return type to CodeEditText
		return codeEditor;
	}

    /**
     * Sets the text content for a regular code file and makes the editor editable.
     * Applies syntax highlighting based on file extension.
     * @param text The code content to display.
     * @param fileName The name of the file, used to detect syntax type.
     */
	public void setText(String text, String fileName) {
        // Save current selection before setting text
        int selectionStart = codeEditor.getSelectionStart();
        int selectionEnd = codeEditor.getSelectionEnd();

        isDiffView = false; // Not a diff view
        codeEditor.setFocusableInTouchMode(true); // Make editable
        codeEditor.setCursorVisible(true);
        codeEditor.setText(text); // Set text

        // Restore selection after setting text
        // Ensure selection is within bounds of the new text length
        codeEditor.setSelection(
                Math.min(selectionStart, codeEditor.length()),
                Math.min(selectionEnd, codeEditor.length()));


		// Ensure scroll position is reset for new text if necessary
        verticalScrollView.scrollTo(0, 0);
        horizontalScrollView.scrollTo(0, 0);
        lineNumbersView.setEditorScrollY(0); // Reset scroll for line numbers too
		updateHandler.removeCallbacks(updateRunnable);
		updateHandler.post(updateRunnable);

        // Trigger full highlight after text is set and layout is stable
        if (attachedHighlighter != null) {
            // Detect syntax type based on the provided fileName
            attachedHighlighter.setSyntaxType(SyntaxHighlightingUtils.detectSyntaxType(fileName));
            attachedHighlighter.highlightSyntax(true); // Force immediate highlight
        }
	}

    /**
     * Sets the text content for a diff view and makes the editor read-only.
     * Applies diff highlighting.
     * @param diffContent The diff content to display.
     */
    public void setDiffContent(String diffContent) {
        // Save current selection before setting text
        int selectionStart = codeEditor.getSelectionStart();
        int selectionEnd = codeEditor.getSelectionEnd();

        isDiffView = true; // This is a diff view
        codeEditor.setFocusable(false); // Make read-only
        codeEditor.setCursorVisible(false);
        codeEditor.setText(diffContent); // Set text

        // Restore selection after setting text
        codeEditor.setSelection(
                Math.min(selectionStart, codeEditor.length()),
                Math.min(selectionEnd, codeEditor.length()));


        // Apply diff highlighting directly
        if (attachedHighlighter != null) {
            attachedHighlighter.setSyntaxType(OptimizedSyntaxHighlighter.SyntaxType.DIFF);
            attachedHighlighter.highlightDiff(codeEditor.getEditableText());
        }

        // Ensure scroll position is reset
        verticalScrollView.scrollTo(0, 0);
        horizontalScrollView.scrollTo(0, 0);
        lineNumbersView.setEditorScrollY(0);
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.post(updateRunnable);
    }

    /**
     * Attaches an OptimizedSyntaxHighlighter to this CodeEditorView.
     * This method should be called once the highlighter is ready.
     * @param highlighter The instance of OptimizedSyntaxHighlighter.
     */
    public void setHighlighter(OptimizedSyntaxHighlighter highlighter) {
        this.attachedHighlighter = highlighter;
        // The highlighter will attach its own TextWatcher to codeEditor
        // and handle initial highlighting.
        attachedHighlighter.attachToEditor(codeEditor);
    }

	public String getText() {
		return codeEditor.getText().toString();
	}

	public void setDarkTheme(boolean darkTheme) {
		if (this.isDarkTheme != darkTheme) {
			this.isDarkTheme = darkTheme;
			lineNumbersView.setDarkTheme(darkTheme);
            if (attachedHighlighter != null) {
                attachedHighlighter.setDarkTheme(darkTheme);
            }
		}
	}

	public void setTextSize(float sp) {
		float newTextSize = Math.max(MIN_TEXT_SIZE_SP, Math.min(sp, MAX_TEXT_SIZE_SP));
		if (Float.compare(this.textSize, newTextSize) != 0) {
			this.textSize = newTextSize;
			codeEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			lineNumbersView.setTextSize(textSize);
			updateLineNumbersViewMetrics();
		}
	}

	public void setOnTextChangedListener(OnTextChangedListener listener) {
		this.onTextChangedListener = listener;
	}

    /**
     * Returns true if the view is currently displaying a diff.
     */
    public boolean isDiffView() {
        return isDiffView;
    }

	private int dpToPx(int dp) {
		return (int) TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_DIP,
		dp,
		getResources().getDisplayMetrics());
	}

	/**
	* LineNumbersView: Draws line numbers and current line highlight.
	* It is fixed on the left and simulates vertical scrolling based on CodeEditText's scroll.
	*/
	private static class LineNumbersView extends View {
		private final Object drawingLock = new Object();
		private Paint textPaint;
		private Paint currentLineTextPaint;
		private Paint currentLineBackgroundPaint;
		private Paint gutterBackgroundPaint;
		private Paint dividerPaint;

		private int lineNumberColor;
		private int currentLineNumberColor;
		private int currentLineHighlightBackgroundColor;
		private int gutterBackgroundColor;
		private int dividerColorValue;
		private boolean isViewDarkTheme = false;

		private EditText editText;
		private int lineCount = 1;
		private int currentLine = 0; // 0-indexed
		private int estimatedLineHeight = 0;

		private float currentTextSizeSp = DEFAULT_TEXT_SIZE_SP;

		private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
		private final Rect tempRect = new Rect();

		private int editorScrollY = 0; // Vertical scroll position of the CodeEditText

		public LineNumbersView(Context context) {
			super(context);
			initView(context);
		}

		private void initView(Context context) {
			TypedValue typedValue = new TypedValue();
			context.getTheme().resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
			isViewDarkTheme = (typedValue.data == 0);

			initializeColors(context);

			textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
			textPaint.setTextSize(spToPx(currentTextSizeSp));
			textPaint.setColor(lineNumberColor);
			textPaint.setTypeface(Typeface.MONOSPACE);
			textPaint.setTextAlign(Paint.Align.RIGHT);

			currentLineTextPaint = new Paint(textPaint);
			currentLineTextPaint.setColor(currentLineNumberColor);
			currentLineTextPaint.setFakeBoldText(true);

			gutterBackgroundPaint = new Paint();
			gutterBackgroundPaint.setColor(gutterBackgroundColor);
			gutterBackgroundPaint.setStyle(Paint.Style.FILL);

			currentLineBackgroundPaint = new Paint();
			currentLineBackgroundPaint.setColor(currentLineHighlightBackgroundColor);
			currentLineBackgroundPaint.setStyle(Paint.Style.FILL);

			dividerPaint = new Paint();
			dividerPaint.setColor(dividerColorValue);
			dividerPaint.setStyle(Paint.Style.STROKE);
			dividerPaint.setStrokeWidth(dpToPx(1));

			setPadding(dpToPx(8), dpToPx(8), dpToPx(4), dpToPx(8));
		}

		private void initializeColors(Context context) {
			if (isViewDarkTheme) {
				lineNumberColor = ContextCompat.getColor(context, R.color.on_surface_variant);
				currentLineNumberColor = ContextCompat.getColor(context, R.color.white);
				gutterBackgroundColor = ContextCompat.getColor(context, R.color.black);
				currentLineHighlightBackgroundColor = ContextCompat.getColor(context, R.color.inverse_surface);
				dividerColorValue = ContextCompat.getColor(context, R.color.primary_light);
			} else {
				lineNumberColor = ContextCompat.getColor(context, R.color.secondary_text);
				currentLineNumberColor = ContextCompat.getColor(context, R.color.black);
				gutterBackgroundColor = ContextCompat.getColor(context, R.color.surface_container_lowest);
				currentLineHighlightBackgroundColor = ContextCompat.getColor(context, R.color.surface_container_highest);
				dividerColorValue = ContextCompat.getColor(context, R.color.divider);
			}
		}

		public void setEditText(EditText editText) {
			this.editText = editText;
			if (editText != null) {
				setTextSize(this.editText.getTextSize() / getResources().getDisplayMetrics().scaledDensity);
				updateMetrics();

				editText.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
					if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
						updateMetrics();
					}
				});

				if (editText instanceof CodeEditText) {
					((CodeEditText) editText).setOnSelectionChangedListener((selStart, selEnd) -> {
						updateCurrentLineFromEditor();
					});
				}
			}
		}

		// To receive scroll updates from the VerticalScrollView
		public void setEditorScrollY(int scrollY) {
			if (this.editorScrollY != scrollY) {
				this.editorScrollY = scrollY;
				invalidate(); // Redraw when scroll position changes
			}
		}

		public void updateMetrics() {
			synchronized (drawingLock) {
				if (editText == null) return;

				boolean needsInvalidate = false;
				Layout layout = editText.getLayout();

				int newEstimatedLineHeight = estimatedLineHeight;
				if (layout != null && layout.getLineCount() > 0) {
					newEstimatedLineHeight = editText.getLineHeight();
					if (newEstimatedLineHeight <= 0) {
						newEstimatedLineHeight = layout.getLineBottom(0) - layout.getLineTop(0);
					}
				}
				if (newEstimatedLineHeight <= 0 && editText.getPaint() != null) {
					Paint.FontMetrics edPaintMetrics = editText.getPaint().getFontMetrics();
					newEstimatedLineHeight = (int) (Math.abs(edPaintMetrics.ascent) + Math.abs(edPaintMetrics.descent) + edPaintMetrics.leading);
					newEstimatedLineHeight = (int) (newEstimatedLineHeight * editText.getLineSpacingMultiplier() + editText.getLineSpacingExtra());
				}
				if (estimatedLineHeight != newEstimatedLineHeight && newEstimatedLineHeight > 0) {
					estimatedLineHeight = newEstimatedLineHeight;
					needsInvalidate = true;
				}

				int newCount;
				if (layout != null) {
					newCount = Math.max(1, layout.getLineCount());
				} else {
					String textContent = editText.getText().toString();
					newCount = Math.max(1, countLinesManually(textContent));
				}
				if (lineCount != newCount) {
					lineCount = newCount;
					needsInvalidate = true;
				}

				if (updateCurrentLineFromEditorInternal()) {
					needsInvalidate = true;
				}

				if (needsInvalidate) {
					invalidate();
				}
			}
		}

		private boolean updateCurrentLineFromEditorInternal() {
			if (editText == null) return false;
			Layout layout = editText.getLayout();
			if (layout != null) {
				int selectionStart = editText.getSelectionStart();
				if (selectionStart < 0) selectionStart = 0;
                if (selectionStart > editText.getText().length()) selectionStart = editText.getText().length();

				int newLine = layout.getLineForOffset(selectionStart);
				if (currentLine != newLine) {
					currentLine = newLine;
					return true;
				}
			}
			return false;
		}

		public void updateCurrentLineFromEditor() {
			if (updateCurrentLineFromEditorInternal()) {
				invalidate();
			}
		}

		private int countLinesManually(String text) {
			if (text == null || text.isEmpty()) return 1;
			int count = 1;
			int index = -1;
			while ((index = text.indexOf('\n', index + 1)) != -1) {
				count++;
			}
			return count;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			synchronized (drawingLock) {
				canvas.drawRect(0, 0, getWidth(), getHeight(), gutterBackgroundPaint);
				canvas.drawLine(getWidth() - dividerPaint.getStrokeWidth() / 2, 0, getWidth() - dividerPaint.getStrokeWidth() / 2, getHeight(), dividerPaint);

				if (editText == null || editText.getLayout() == null || estimatedLineHeight <= 0) {
					return;
				}
				Layout editorLayout = editText.getLayout();

				int viewPaddingTop = getPaddingTop();
				int viewPaddingRight = getPaddingRight();

				// Culling: Determine the range of visible lines based on editorScrollY
				int firstVisibleLineIndex = Math.max(0, editorScrollY / estimatedLineHeight);
                int visibleContentHeight = getHeight() - viewPaddingTop - getPaddingBottom();
                if (visibleContentHeight < 0) visibleContentHeight = 0;
                int lastVisibleLineIndex = Math.min(lineCount - 1, firstVisibleLineIndex + (visibleContentHeight / estimatedLineHeight) + 2);


				// Draw current line's background highlight
				if (currentLine >= 0 && currentLine < lineCount && currentLine >= firstVisibleLineIndex && currentLine <= lastVisibleLineIndex) {
					// Y positions are relative to this View's top, adjusted by editorScrollY
					float highlightRectTop = editorLayout.getLineTop(currentLine) - editorScrollY + viewPaddingTop;
					float highlightRectBottom = editorLayout.getLineBottom(currentLine) - editorScrollY + viewPaddingTop;

					tempRect.set(
					0,
					(int) highlightRectTop,
					getWidth() - (int)dividerPaint.getStrokeWidth(),
					(int) highlightRectBottom
					);
                    // Clip the highlight to the view's bounds
                    if (tempRect.intersect(0,0,getWidth(),getHeight())) {
                        canvas.drawRect(tempRect, currentLineBackgroundPaint);
                    }
				}

				// Draw the line numbers
				for (int i = firstVisibleLineIndex; i <= lastVisibleLineIndex; i++) {
					if (i >= lineCount || i < 0) continue;

					Paint paintToUse = (i == currentLine) ? currentLineTextPaint : textPaint;
					String lineNumberText = String.valueOf(i + 1);

					// editorLayout.getLineBaseline(i) is relative to EditText's content.
					// Adjust by editorScrollY and add viewPaddingTop.
					float yDrawBaseline = editorLayout.getLineBaseline(i) - editorScrollY + viewPaddingTop;
                    
                    // Only draw if the line number is actually within the visible part of the view
                    paintToUse.getFontMetrics(fontMetrics);
                    if (yDrawBaseline + fontMetrics.descent < 0 || yDrawBaseline + fontMetrics.ascent > getHeight()){
                         continue;
                    }
					canvas.drawText(lineNumberText, getWidth() - viewPaddingRight, yDrawBaseline, paintToUse);
				}
			}
		}


		public void setDarkTheme(boolean darkTheme) {
			if (this.isViewDarkTheme != darkTheme) {
				this.isViewDarkTheme = darkTheme;
				initializeColors(getContext());
				textPaint.setColor(lineNumberColor);
				currentLineTextPaint.setColor(currentLineNumberColor);
				gutterBackgroundPaint.setColor(gutterBackgroundColor);
				currentLineBackgroundPaint.setColor(currentLineHighlightBackgroundColor);
				dividerPaint.setColor(dividerColorValue);
				invalidate();
			}
		}

		public void setTextSize(float sp) {
			if (Float.compare(this.currentTextSizeSp, sp) != 0) {
				this.currentTextSizeSp = sp;
				float pixelSize = spToPx(sp);
				textPaint.setTextSize(pixelSize);
				currentLineTextPaint.setTextSize(pixelSize);
				invalidate();
			}
		}

		private float spToPx(float sp) {
			return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_SP,
			sp,
			getResources().getDisplayMetrics());
		}

		private int dpToPx(int dp) {
			return (int) TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			dp,
			getResources().getDisplayMetrics());
		}
	}

	public interface OnSelectionChangedListener {
		void onSelectionChanged(int selStart, int selEnd);
	}

	public static class CodeEditText extends androidx.appcompat.widget.AppCompatEditText {
		private OnSelectionChangedListener onSelectionChangedListener;

		public CodeEditText(Context context) {
			super(context);
		}

		public CodeEditText(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public CodeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
		}

		@Override
		protected void onSelectionChanged(int selStart, int selEnd) {
			super.onSelectionChanged(selStart, selEnd);
			if (onSelectionChangedListener != null) {
				onSelectionChangedListener.onSelectionChanged(selStart, selEnd);
			}
		}

		public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
			this.onSelectionChangedListener = listener;
		}
	}
}