package com.codex.apk.CodeEditor.patterns;

import java.util.regex.Pattern;

/**
 * Defines regex patterns for JSON syntax highlighting.
 * Patterns are designed for accurate JSON structure highlighting.
 */
public class JsonPatterns {
    // Matches JSON object keys, which are always double-quoted strings followed by a colon.
    // Group 1 captures the key name itself.
    public static final Pattern JSON_KEY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:");

    // Matches JSON string values. This is distinct from keys.
    // It looks for a double-quoted string not immediately preceded by a quote-colon pattern (which would be a key).
    // Or, more simply, any double-quoted string that is a value (after a colon or in an array).
    public static final Pattern JSON_STRING_VALUE_PATTERN = Pattern.compile("(?<!\"\\s*):\\s*\"([^\"]*)\"|(?<=\\[\\s*)\"([^\"]*)\"");


    // Matches JSON number values (integers, decimals, and scientific notation).
    public static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("\\b-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b");

    // Matches JSON boolean literals (true, false).
    public static final Pattern JSON_BOOLEAN_PATTERN = Pattern.compile("\\b(true|false)\\b");

    // Matches JSON null literal.
    public static final Pattern JSON_NULL_PATTERN = Pattern.compile("\\bnull\\b");
}
