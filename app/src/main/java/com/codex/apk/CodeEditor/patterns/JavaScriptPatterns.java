package com.codex.apk.CodeEditor.patterns;

import java.util.regex.Pattern;

/**
 * Defines regex patterns for JavaScript syntax highlighting.
 * Enhanced patterns to better cover modern JS features.
 */
public class JavaScriptPatterns {
    // Matches common JavaScript keywords including ES6+ features
    public static final Pattern JS_KEYWORD_PATTERN = Pattern.compile(
            "\\b(var|let|const|function|return|if|else|for|while|do|break|continue|switch|case|default|try|catch|finally|throw|new|this|typeof|instanceof|in|of|class|extends|super|import|export|from|as|async|await|yield|debugger|delete|void|with)\\b");

    // Matches common built-in global objects and constructors
    public static final Pattern JS_BUILT_IN_PATTERN = Pattern.compile(
            "\\b(document|window|console|Math|Array|Object|String|Number|Boolean|RegExp|Date|JSON|Promise|Map|Set|Symbol|Proxy|Reflect|Error|HTMLElement|Node|Event|XMLHttpRequest|localStorage|sessionStorage)\\b");

    // Matches single-quoted or double-quoted strings, handling escaped quotes
    public static final Pattern JS_STRING_PATTERN = Pattern.compile("([\"'])(?:\\\\\\1|.)*?\\1");

    // Matches numbers, including integers, decimals, and scientific notation
    public static final Pattern JS_NUMBER_PATTERN = Pattern.compile("\\b(\\d+(\\.\\d+)?([eE][+-]?\\d+)?|0x[0-9a-fA-F]+\\b)");

    // Matches function declarations, named function expressions, and method names
    // Group 1 captures the function name.
    public static final Pattern JS_FUNCTION_PATTERN = Pattern.compile("\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(");

    // Matches single-line comments (//...)
    public static final Pattern JS_SINGLE_LINE_COMMENT_PATTERN = Pattern.compile("//.*");

    // Matches multi-line comments (/* ... */)
    public static final Pattern JS_MULTI_LINE_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/");

    // Matches template literals (backticks `...`)
    public static final Pattern JS_TEMPLATE_STRING_PATTERN = Pattern.compile("`[^`]*`");

    // Matches regular expression literals (e.g., /pattern/flags)
    public static final Pattern JS_REGEX_PATTERN = Pattern.compile("/[^/\\\\]*(?:\\\\.[^/\\\\]*)*/[gimsuy]?");

    // Matches boolean literals (true, false) and null literal
    public static final Pattern JS_BOOLEAN_NULL_PATTERN = Pattern.compile("\\b(true|false|null)\\b");
}
