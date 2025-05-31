package com.codex.apk.codebase;

import java.util.regex.Pattern;

/**
 * Centralized repository for all regex patterns used in codebase indexing.
 */
public class CodebasePatterns {

    // JavaScript: Functions, classes, variables (const, let, var), exported symbols
    public static final Pattern JS_FUNCTION_PATTERN = Pattern.compile(
            "(?:function\\s+([a-zA-Z_$][\\w$]*)\\s*\\(|(?:const|let|var)\\s+([a-zA-Z_$][\\w$]*)\\s*=\\s*(?:function\\s*\\(|\\([^)]*\\)\\s*=>))"
    );
    public static final Pattern JS_CLASS_PATTERN = Pattern.compile("class\\s+([a-zA-Z_$][\\w$]*)");
    public static final Pattern JS_EXPORT_NAMED_PATTERN = Pattern.compile(
            "export\\s+(?:const|let|var|function|class)\\s+([a-zA-Z_$][\\w$]*)"
    );
    public static final Pattern JS_EXPORT_DEFAULT_PATTERN = Pattern.compile(
            "export\\s+default\\s+(?:function\\s+([a-zA-Z_$][\\w$]*)|class\\s+([a-zA-Z_$][\\w$]*)|([a-zA-Z_$][\\w$]*))"
    );
    public static final Pattern JS_EXPORT_DESTRUCTURING_PATTERN = Pattern.compile(
            "export\\s+\\{([^}]+)\\}"
    ); // For `export { name1, name2 as alias }`

    // HTML: IDs and Classes
    public static final Pattern HTML_ID_PATTERN = Pattern.compile("id=[\"']([^\"']+)[\"']");
    public static final Pattern HTML_CLASS_PATTERN = Pattern.compile("class=[\"']([^\"']+)[\"']");
    public static final Pattern HTML_TAG_PATTERN = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)(?:\\s[^>]*)?>"); // Basic tag detection

    // CSS: Selectors (classes, IDs, tags)
    public static final Pattern CSS_SELECTOR_PATTERN = Pattern.compile("^([.#]?[a-zA-Z0-9_-]+(?:\\s*[:>+~][^,{]+)*)\\s*\\{"); // Improved CSS selector

    // JavaScript: import and require statements
    public static final Pattern JS_IMPORT_REQUIRE_PATTERN = Pattern.compile(
            "(?:import(?:\\s+.*?\\s+from)?\\s*[\"']([^\"\']+)[\"']|require\\([\"']([^\"\']+)[\"']\\))"
    );

    // HTML: script src, link href, img src
    public static final Pattern HTML_RESOURCE_PATTERN = Pattern.compile(
            "(?:src|href)=[\"']([^\"']+\\.(?:js|css|png|jpg|jpeg|gif|svg|webp))[\"']"
    );

    // CSS: @import rules
    public static final Pattern CSS_IMPORT_PATTERN = Pattern.compile(
            "@import\\s+(?:url\\()?\\s*[\"']([^\"']+)[\"'](?:\\))?;"
    );
}
