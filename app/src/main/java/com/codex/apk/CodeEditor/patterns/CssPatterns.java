package com.codex.apk.CodeEditor.patterns;

import java.util.regex.Pattern;

/**
 * Defines regex patterns for CSS syntax highlighting.
 * Enhanced with more specific patterns for at-rules and functions.
 */
public class CssPatterns {
    // Matches a CSS selector (e.g., .class, #id, element, div p, :hover, ::before)
    // Excludes content within curly braces.
    public static final Pattern CSS_SELECTOR_PATTERN = Pattern.compile("([^{};]+)(?=\\s*\\{)");

    // Matches a CSS property name (e.g., "color" in color: blue;)
    public static final Pattern CSS_PROPERTY_PATTERN = Pattern.compile("\\b([a-zA-Z-]+)\\s*:");

    // Matches the entire value part after a colon, up to a semicolon or end of block.
    // Group 1 captures the content of the value.
    public static final Pattern CSS_VALUE_PATTERN = Pattern.compile(":\\s*([^;\\}]+)");

    // Matches CSS multi-line comments (/* ... */)
    public static final Pattern CSS_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/");

    // Matches the !important keyword
    public static final Pattern CSS_IMPORTANT_PATTERN = Pattern.compile("!important");

    // Matches hex color codes (e.g., #RRGGBB, #RGB)
    public static final Pattern CSS_COLOR_PATTERN = Pattern.compile("#([a-fA-F0-9]{3}|[a-fA-F0-9]{6})\\b");

    // Matches CSS units (e.g., 10px, 2.5em, 50%, 100vw)
    public static final Pattern CSS_UNIT_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?(px|em|rem|vh|vw|%|pt|pc|in|cm|mm|ex|ch|vmin|vmax)\\b");

    // Matches CSS at-rules (e.g., @media, @import, @keyframes, @font-face, @charset)
    // Covers the rule name and its initial condition/selector.
    public static final Pattern CSS_AT_RULE_PATTERN = Pattern.compile("@(media|import|charset|keyframes|font-face|supports|container|layer|scope|property|custom-media|viewport)\\b[^;{]*");

    // Matches CSS pseudo-classes and pseudo-elements (e.g., :hover, ::before)
    public static final Pattern CSS_PSEUDO_PATTERN = Pattern.compile("::?[a-zA-Z-]+(?:\\([^)]*\\))?");

    // Matches CSS function calls (e.g., url(), calc(), rgb(), var())
    // Group 1 captures the function name.
    public static final Pattern CSS_FUNCTION_PATTERN = Pattern.compile("\\b([a-zA-Z-]+)\\([^)]*\\)");

    // Matches CSS string values (e.g., 'hello', "world") within property values
    public static final Pattern CSS_STRING_PATTERN = Pattern.compile("([\"'])(\\\\\\\\?\\1|.)*?\\1");
}
