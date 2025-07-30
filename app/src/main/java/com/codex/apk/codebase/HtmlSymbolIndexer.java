package com.codex.apk.codebase;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and indexes HTML IDs, classes, and tag names (definitions)
 * and also attempts to identify basic symbol usages within HTML.
 */
public class HtmlSymbolIndexer implements SymbolIndexer {

    // Regex for detecting script blocks to find JS usages
    private static final Pattern SCRIPT_BLOCK_PATTERN = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
    // Regex for detecting attribute values that might be symbol usages (e.g., onclick="myFunction()")
    private static final Pattern ATTRIBUTE_USAGE_PATTERN = Pattern.compile("(?:on[a-zA-Z]+|data-[a-zA-Z-]+)=[\"']([^\"']*)[\"']");

    @Override
    public void indexSymbols(String filePath, String content,
                             Map<String, List<String>> symbolDefinitionIndex,
                             Map<String, List<String>> symbolUsageIndex) {
        // --- Index Definitions ---
        // Index IDs
        Matcher idMatcher = CodebasePatterns.HTML_ID_PATTERN.matcher(content);
        while (idMatcher.find()) {
            addSymbolToDefinitionIndex("#" + idMatcher.group(1), filePath, symbolDefinitionIndex);
        }

        // Index classes
        Matcher classMatcher = CodebasePatterns.HTML_CLASS_PATTERN.matcher(content);
        while (classMatcher.find()) {
            String[] classes = classMatcher.group(1).split("\\s+");
            for (String cls : classes) {
                if (!cls.isEmpty()) {
                    addSymbolToDefinitionIndex("." + cls, filePath, symbolDefinitionIndex);
                }
            }
        }

        // Index HTML tag names
        Matcher tagMatcher = CodebasePatterns.HTML_TAG_PATTERN.matcher(content);
        while (tagMatcher.find()) {
            addSymbolToDefinitionIndex("<" + tagMatcher.group(1).toLowerCase() + ">", filePath, symbolDefinitionIndex); // Store as <tagname>
        }

        // --- Index Usages (Simplified) ---
        // 1. Look for usages within <script> blocks (delegating to JS indexer logic)
        Matcher scriptBlockMatcher = SCRIPT_BLOCK_PATTERN.matcher(content);
        JavaScriptSymbolIndexer jsIndexer = new JavaScriptSymbolIndexer(); // Use JS indexer for script content
        while (scriptBlockMatcher.find()) {
            String scriptContent = scriptBlockMatcher.group(1);
            jsIndexer.indexSymbols(filePath, scriptContent, symbolDefinitionIndex, symbolUsageIndex); // Pass both as JS might define/use
        }

        // 2. Look for usages in attribute values (e.g., `onclick="myFunction()"`, `data-component="MyComponent"`)
        Matcher attributeUsageMatcher = ATTRIBUTE_USAGE_PATTERN.matcher(content);
        while (attributeUsageMatcher.find()) {
            String attributeValue = attributeUsageMatcher.group(1);
            // Simple word-based detection within attribute values
            Pattern wordPattern = Pattern.compile("\\b([a-zA-Z_$][\\w$]*)\\b");
            Matcher wordMatcher = wordPattern.matcher(attributeValue);
            while (wordMatcher.find()) {
                String symbol = wordMatcher.group(1);
                // Avoid adding to usage if it's a known definition in this file (e.g., an ID being used in JS within the same file)
                if (!symbolDefinitionIndex.containsKey(symbol) || !symbolDefinitionIndex.get(symbol).contains(filePath)) {
                    addSymbolToUsageIndex(symbol, filePath, symbolUsageIndex);
                }
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
