package com.codex.apk.codebase;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Extracts and indexes CSS selectors (IDs, classes, and element selectors).
 * Currently, only focuses on definitions.
 */
public class CssSymbolIndexer implements SymbolIndexer {

    @Override
    public void indexSymbols(String filePath, String content,
                             Map<String, List<String>> symbolDefinitionIndex,
                             Map<String, List<String>> symbolUsageIndex) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            Matcher selectorMatcher = CodebasePatterns.CSS_SELECTOR_PATTERN.matcher(line);
            if (selectorMatcher.find()) {
                String selector = selectorMatcher.group(1).trim();
                if (!selector.isEmpty() && !selector.startsWith("@")) { // Exclude @rules
                    addSymbolToDefinitionIndex(selector, filePath, symbolDefinitionIndex);
                }
            }
        }
        // No explicit usage indexing for CSS in this iteration due to complexity
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
}
