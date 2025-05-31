package com.codex.apk.codebase;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and indexes JavaScript functions, classes, and exported symbols (definitions)
 * and also attempts to identify basic symbol usages.
 */
public class JavaScriptSymbolIndexer implements SymbolIndexer {

    // Regex for basic function calls or variable access (simplified)
    private static final Pattern JS_USAGE_PATTERN = Pattern.compile(
            "\\b([a-zA-Z_$][\\w$]*)(?:\\s*\\(|\\s*[.=;])" // Matches symbol followed by ( or . or = or ;
    );

    @Override
    public void indexSymbols(String filePath, String content,
                             Map<String, List<String>> symbolDefinitionIndex,
                             Map<String, List<String>> symbolUsageIndex) {
        // --- Index Definitions ---
        // Index functions and variables
        Matcher functionMatcher = CodebasePatterns.JS_FUNCTION_PATTERN.matcher(content);
        while (functionMatcher.find()) {
            String symbol = functionMatcher.group(1) != null ?
                    functionMatcher.group(1) : functionMatcher.group(2);
            addSymbolToDefinitionIndex(symbol, filePath, symbolDefinitionIndex);
        }

        // Index classes
        Matcher classMatcher = CodebasePatterns.JS_CLASS_PATTERN.matcher(content);
        while (classMatcher.find()) {
            addSymbolToDefinitionIndex(classMatcher.group(1), filePath, symbolDefinitionIndex);
        }

        // Index named exports
        Matcher namedExportMatcher = CodebasePatterns.JS_EXPORT_NAMED_PATTERN.matcher(content);
        while (namedExportMatcher.find()) {
            addSymbolToDefinitionIndex(namedExportMatcher.group(1), filePath, symbolDefinitionIndex);
        }

        // Index default exports
        Matcher defaultExportMatcher = CodebasePatterns.JS_EXPORT_DEFAULT_PATTERN.matcher(content);
        while (defaultExportMatcher.find()) {
            String symbol = defaultExportMatcher.group(1);
            if (symbol == null) symbol = defaultExportMatcher.group(2);
            if (symbol == null) symbol = defaultExportMatcher.group(3);
            addSymbolToDefinitionIndex(symbol, filePath, symbolDefinitionIndex);
        }

        // Index destructuring exports
        Matcher destructuringExportMatcher = CodebasePatterns.JS_EXPORT_DESTRUCTURING_PATTERN.matcher(content);
        while (destructuringExportMatcher.find()) {
            String exportedNames = destructuringExportMatcher.group(1);
            for (String name : exportedNames.split(",")) {
                name = name.trim();
                if (name.contains(" as ")) {
                    name = name.substring(name.indexOf(" as ") + 4).trim();
                }
                addSymbolToDefinitionIndex(name, filePath, symbolDefinitionIndex);
            }
        }

        // --- Index Usages (Simplified) ---
        Matcher usageMatcher = JS_USAGE_PATTERN.matcher(content);
        while (usageMatcher.find()) {
            String symbol = usageMatcher.group(1);
            // Only add to usage index if it's not a definition (to avoid self-referencing)
            // This is a heuristic and might not be perfect.
            if (!symbolDefinitionIndex.containsKey(symbol) || !symbolDefinitionIndex.get(symbol).contains(filePath)) {
                 addSymbolToUsageIndex(symbol, filePath, symbolUsageIndex);
            }
        }
    }

    /**
     * Helper method to add a symbol to the symbol definition index map.
     * @param symbol The symbol name.
     * @param filePath The relative path of the file where the symbol is defined.
     * @param symbolDefinitionIndex The map to add the symbol definition to.
     */
    private void addSymbolToDefinitionIndex(String symbol, String filePath, Map<String, List<String>> symbolDefinitionIndex) {
        if (symbol == null || symbol.isEmpty()) return;
        symbolDefinitionIndex.computeIfAbsent(symbol, k -> new java.util.ArrayList<>()).add(filePath);
    }

    /**
     * Helper method to add a symbol to the symbol usage index map.
     * @param symbol The symbol name.
     * @param filePath The relative path of the file where the symbol is used.
     * @param symbolUsageIndex The map to add the symbol usage to.
     */
    private void addSymbolToUsageIndex(String symbol, String filePath, Map<String, List<String>> symbolUsageIndex) {
        if (symbol == null || symbol.isEmpty()) return;
        symbolUsageIndex.computeIfAbsent(symbol, k -> new java.util.ArrayList<>()).add(filePath);
    }
}
