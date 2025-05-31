package com.codex.apk.codebase;

import java.util.List;
import java.util.Map;

/**
 * Interface for classes responsible for extracting and indexing symbols from file content.
 */
public interface SymbolIndexer {
    /**
     * Extracts symbols (definitions and usages) from the given file content
     * and adds them to the provided symbol index maps.
     *
     * @param filePath The relative path of the file being indexed.
     * @param content The content of the file.
     * @param symbolDefinitionIndex The map to which extracted symbol definitions should be added (symbol name -> list of file paths).
     * @param symbolUsageIndex The map to which extracted symbol usages should be added (symbol name -> list of file paths).
     */
    void indexSymbols(String filePath, String content,
                      Map<String, List<String>> symbolDefinitionIndex,
                      Map<String, List<String>> symbolUsageIndex);
}
