package com.codex.apk.CodeEditor.highlighters;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.codex.apk.CodeEditor.SyntaxColorProvider;
import com.codex.apk.CodeEditor.patterns.CssPatterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern; // Import Pattern

/**
 * Handles syntax highlighting for CSS.
 * Enhanced to include more specific patterns for at-rules and functions.
 */
public class CssHighlighter extends BaseHighlighter {

    public CssHighlighter(SyntaxColorProvider colorProvider) {
        super(colorProvider);
    }

    @Override
    public void highlight(SpannableStringBuilder spannable) {
        // Comments
        highlightPattern(spannable, CssPatterns.CSS_COMMENT_PATTERN, 3);
        // At-rules (@media, @import, @keyframes, @font-face, etc.)
        highlightPattern(spannable, CssPatterns.CSS_AT_RULE_PATTERN, 7);
        // Selectors (element, class, ID, pseudo)
        highlightPattern(spannable, CssPatterns.CSS_SELECTOR_PATTERN, 0);
        highlightPattern(spannable, CssPatterns.CSS_PSEUDO_PATTERN, 8); // Pseudo-classes and pseudo-elements
        // Properties
        highlightPattern(spannable, CssPatterns.CSS_PROPERTY_PATTERN, 1);
        // Values, including specific types like colors and units, and functions
        highlightCssValues(spannable); // Handles string values and general values
        highlightPattern(spannable, CssPatterns.CSS_COLOR_PATTERN, 5); // Hex colors
        highlightPattern(spannable, CssPatterns.CSS_UNIT_PATTERN, 6);   // Units (px, em, %, etc.)
        highlightPattern(spannable, CssPatterns.CSS_FUNCTION_PATTERN, 9); // CSS functions like url(), calc(), var()
        // Important keyword
        highlightPattern(spannable, CssPatterns.CSS_IMPORTANT_PATTERN, 4);
    }

    /**
     * Highlight CSS values that are strings or general values.
     * This method focuses on the value part of a property: value; pair.
     */
    private void highlightCssValues(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        Matcher matcher = CssPatterns.CSS_VALUE_PATTERN.matcher(text);
        int[] colors = colorProvider.getSyntaxColors();
        // Color index 2 for general values and string values
        int valueColor = colors[2];

        while (matcher.find()) {
            // Group 1 captures the value itself (excluding the colon and leading space)
            int valueStart = matcher.start(1);
            int valueEnd = matcher.end(1);

            // Apply span only if the value part is not empty
            if (valueStart < valueEnd) {
                spannable.setSpan(
                        new ForegroundColorSpan(valueColor),
                        valueStart,
                        valueEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Additionally highlight string values within CSS
        Matcher stringMatcher = CssPatterns.CSS_STRING_PATTERN.matcher(text);
        while (stringMatcher.find()) {
            spannable.setSpan(
                    new ForegroundColorSpan(valueColor), // Use the same string color as general values
                    stringMatcher.start(),
                    stringMatcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
