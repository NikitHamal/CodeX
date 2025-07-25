package com.codex.apk.CodeEditor.highlighters;

import android.text.SpannableStringBuilder;

import com.codex.apk.CodeEditor.SyntaxColorProvider;
import com.codex.apk.CodeEditor.patterns.JsonPatterns;

/**
 * Handles syntax highlighting for JSON.
 * Provides distinct highlighting for keys, strings, numbers, booleans, and null values.
 */
public class JsonHighlighter extends BaseHighlighter {

    public JsonHighlighter(SyntaxColorProvider colorProvider) {
        super(colorProvider);
    }

    @Override
    public void highlight(SpannableStringBuilder spannable) {
        // JSON Keys (e.g., "key":)
        highlightPattern(spannable, JsonPatterns.JSON_KEY_PATTERN, 0);
        // String Values (e.g., "value")
        highlightPattern(spannable, JsonPatterns.JSON_STRING_VALUE_PATTERN, 1); // Renamed for clarity
        // Number Values (integers, floats, scientific)
        highlightPattern(spannable, JsonPatterns.JSON_NUMBER_PATTERN, 2);
        // Boolean Values (true, false)
        highlightPattern(spannable, JsonPatterns.JSON_BOOLEAN_PATTERN, 3);
        // Null Value
        highlightPattern(spannable, JsonPatterns.JSON_NULL_PATTERN, 4);
    }
}
