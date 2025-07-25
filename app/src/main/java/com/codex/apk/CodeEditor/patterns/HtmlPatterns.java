package com.codex.apk.CodeEditor.patterns;

import java.util.regex.Pattern;

/**
 * Defines regex patterns for HTML syntax highlighting.
 * Enhanced patterns for better precision.
 */
public class HtmlPatterns {
    // Matches a full HTML tag including its attributes, but not its content. E.g., <div id="myId"> or <img src="image.png"/>.
    // Used to find the overall range for attribute/value parsing.
    public static final Pattern HTML_FULL_TAG_PATTERN = Pattern.compile("<[/]?[a-zA-Z0-9]+(?:\\s+[^>]*?)?>");

    // Matches the tag name itself within an HTML tag (e.g., "div" in <div>)
    public static final Pattern HTML_TAG_NAME_PATTERN = Pattern.compile("<[/]?([a-zA-Z0-9]+)");

    // Matches attribute names (e.g., "id" in id="value")
    public static final Pattern HTML_ATTRIBUTE_NAME_PATTERN = Pattern.compile("([a-zA-Z0-9_:-]+)\\s*=\\s*[\"']");

    // Matches attribute values (e.g., "value" in id="value")
    // Captures the content inside the quotes.
    public static final Pattern HTML_ATTRIBUTE_VALUE_PATTERN = Pattern.compile("=\\s*\"([^\"]*)\"|=\\s*'([^']*)'");

    // Matches HTML comments (<!-- ... -->)
    public static final Pattern HTML_COMMENT_PATTERN = Pattern.compile("<!--[\\s\\S]*?-->");

    // Matches DOCTYPE declaration (e.g., <!DOCTYPE html>)
    public static final Pattern HTML_DOCTYPE_PATTERN = Pattern.compile("<!DOCTYPE[^>]*>");

    // Matches HTML entities (e.g., &nbsp;, &#123;)
    public static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z0-9#]+;");

    // Additional patterns for embedded CSS/JS if needed, but not implemented for this scope.
    public static final Pattern HTML_STYLE_BLOCK_CONTENT_PATTERN = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>");
    public static final Pattern HTML_SCRIPT_BLOCK_CONTENT_PATTERN = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>");
}
