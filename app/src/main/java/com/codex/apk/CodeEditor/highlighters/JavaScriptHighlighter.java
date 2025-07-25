package com.codex.apk.CodeEditor.highlighters;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.codex.apk.CodeEditor.SyntaxColorProvider;
import com.codex.apk.CodeEditor.patterns.JavaScriptPatterns;

import java.util.regex.Matcher;

/**
 * Handles syntax highlighting for JavaScript.
 * Enhanced to cover more modern JS features and improve pattern accuracy.
 */
public class JavaScriptHighlighter extends BaseHighlighter {

    public JavaScriptHighlighter(SyntaxColorProvider colorProvider) {
        super(colorProvider);
    }

    @Override
    public void highlight(SpannableStringBuilder spannable) {
        // Comments (Multi-line and Single-line)
        highlightPattern(spannable, JavaScriptPatterns.JS_MULTI_LINE_COMMENT_PATTERN, 5);
        highlightPattern(spannable, JavaScriptPatterns.JS_SINGLE_LINE_COMMENT_PATTERN, 5);

        // Strings (Single, Double, and Template Literals)
        highlightPattern(spannable, JavaScriptPatterns.JS_STRING_PATTERN, 2);
        highlightPattern(spannable, JavaScriptPatterns.JS_TEMPLATE_STRING_PATTERN, 6);

        // Regular Expressions
        highlightPattern(spannable, JavaScriptPatterns.JS_REGEX_PATTERN, 7);

        // Keywords (const, let, function, return, if, else, async, await, class, import, export, etc.)
        highlightPattern(spannable, JavaScriptPatterns.JS_KEYWORD_PATTERN, 0);

        // Built-in Objects/Globals (document, window, console, Math, Array, etc.)
        highlightPattern(spannable, JavaScriptPatterns.JS_BUILT_IN_PATTERN, 1);

        // Numbers (Integers, Decimals, Scientific notation)
        highlightPattern(spannable, JavaScriptPatterns.JS_NUMBER_PATTERN, 3);

        // Function names (declarations and calls)
        highlightJsFunctions(spannable);

        // Boolean and Null literals
        highlightPattern(spannable, JavaScriptPatterns.JS_BOOLEAN_NULL_PATTERN, 3); // Often colored like numbers or distinct
    }

    /**
     * Highlights JavaScript function names.
     * This targets function declarations, named function expressions, and method names.
     */
    private void highlightJsFunctions(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        Matcher matcher = JavaScriptPatterns.JS_FUNCTION_PATTERN.matcher(text);
        int[] colors = colorProvider.getSyntaxColors();
        int functionColor = colors[4]; // Color for functions

        while (matcher.find()) {
            // Group 1 captures the function name
            if (matcher.group(1) != null) {
                spannable.setSpan(
                        new ForegroundColorSpan(functionColor),
                        matcher.start(1),
                        matcher.end(1),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
