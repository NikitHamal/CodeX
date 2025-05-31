package com.codex.apk.codebase;

import com.codex.apk.FileManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods for generating various summaries of the codebase based on indexed data.
 */
public class CodebaseSummarizer {
    private final Map<String, FileMetadata> fileIndex;
    private final Map<String, List<String>> symbolDefinitionIndex;
    private final Map<String, List<String>> symbolUsageIndex; // New: for symbol usage
    private final List<FileDependency> dependencyGraph;
    private final Map<String, List<String>> fileTypeIndex;
    private final FileManager fileManager; // Needed for getRelativePath in some contexts

    public CodebaseSummarizer(Map<String, FileMetadata> fileIndex,
                              Map<String, List<String>> symbolDefinitionIndex,
                              Map<String, List<String>> symbolUsageIndex, // New parameter
                              List<FileDependency> dependencyGraph,
                              Map<String, List<String>> fileTypeIndex,
                              FileManager fileManager) {
        this.fileIndex = fileIndex;
        this.symbolDefinitionIndex = symbolDefinitionIndex;
        this.symbolUsageIndex = symbolUsageIndex; // Assign new index
        this.dependencyGraph = dependencyGraph;
        this.fileTypeIndex = fileTypeIndex;
        this.fileManager = fileManager;
    }

    /**
     * Generates a summary of the entire codebase, including statistics, key files,
     * key components, and dependency analysis.
     * @return A string containing the codebase summary.
     */
    public String getCodebaseSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("Project Statistics:\n");
        summary.append("- Total files indexed: ").append(fileIndex.size()).append("\n");
        for (Map.Entry<String, List<String>> entry : fileTypeIndex.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                summary.append("- ").append(entry.getKey().toUpperCase())
                        .append(" files: ").append(entry.getValue().size()).append("\n");
            }
        }

        // Key files
        summary.append("\nKey Files (potential entry points/config):\n");
        List<String> keyFiles = detectKeyFiles();
        if (keyFiles.isEmpty()) {
            summary.append("  No obvious key files detected.\n");
        } else {
            for (String file : keyFiles) {
                summary.append("  - ").append(file).append("\n");
            }
        }

        // Key components/symbols
        summary.append("\nKey Components/Symbols Detected:\n");
        List<String> components = detectKeyComponents();
        if (components.isEmpty()) {
            summary.append("  No significant components or widely used symbols detected.\n");
        } else {
            for (String comp : components) {
                summary.append("  - ").append(comp).append("\n");
            }
        }

        // Dependency analysis
        summary.append("\nDependency Analysis:\n");
        Map<String, Integer> mostDependent = getMostDependentFiles();
        if (mostDependent.isEmpty()) {
            summary.append("  No significant file dependencies detected.\n");
        } else {
            summary.append("  Most depended-on files (files with most incoming dependencies):\n");
            for (Map.Entry<String, Integer> entry : mostDependent.entrySet()) {
                summary.append("  - ").append(entry.getKey())
                        .append(" (").append(entry.getValue()).append(" dependents)\n");
            }
        }

        // Find files with no dependents (potential entry points or dead code)
        List<String> filesWithNoDependents = getFilesWithNoDependents();
        if (!filesWithNoDependents.isEmpty()) {
            summary.append("  Files with no other files depending on them (potential entry points or unused files):\n");
            for (String file : filesWithNoDependents) {
                summary.append("  - ").append(file).append("\n");
            }
        }

        return summary.toString();
    }

    /**
     * Generates a detailed summary for a specific file.
     * @param filePath The relative path of the file.
     * @return A string containing the file summary, or an error message if the file is not found.
     */
    public String getFileSummary(String filePath) {
        if (!fileIndex.containsKey(filePath)) {
            return "File not found in index: " + filePath;
        }

        FileMetadata meta = fileIndex.get(filePath);
        StringBuilder summary = new StringBuilder();

        summary.append("File: ").append(filePath).append("\n");
        summary.append("Type: ").append(meta.type.toUpperCase()).append("\n");
        summary.append("Size: ").append(meta.content.length()).append(" characters\n");
        summary.append("Last indexed: ").append(meta.lastIndexed).append(" (Unix timestamp)\n");

        // Symbols defined in this file
        List<String> definedSymbols = getSymbolsDefinedInFile(filePath);
        if (!definedSymbols.isEmpty()) {
            summary.append("\nSymbols defined in this file:\n");
            for (String symbol : definedSymbols) {
                summary.append("  - ").append(symbol).append("\n");
            }
        } else {
            summary.append("\nNo specific symbols defined in this file (or not parsed).\n");
        }

        // Symbols used in this file
        List<String> usedSymbols = getSymbolsUsedInFile(filePath);
        if (!usedSymbols.isEmpty()) {
            summary.append("\nSymbols used in this file:\n");
            for (String symbol : usedSymbols) {
                summary.append("  - ").append(symbol).append("\n");
            }
        } else {
            summary.append("\nNo specific symbols used in this file (or not parsed).\n");
        }

        // Dependencies (files this file imports/links to)
        List<String> deps = getFileDependencies(filePath);
        if (!deps.isEmpty()) {
            summary.append("\nDependencies (files this file uses):\n");
            for (String dep : deps) {
                summary.append("  - ").append(dep).append("\n");
            }
        } else {
            summary.append("\nNo direct file dependencies found.\n");
        }

        // Dependents (files that import/link to this file)
        List<String> dependents = getFileDependents(filePath);
        if (!dependents.isEmpty()) {
            summary.append("\nFiles that depend on this file:\n");
            for (String dep : dependents) {
                summary.append("  - ").append(dep).append("\n");
            }
        } else {
            summary.append("\nNo other files depend on this file.\n");
        }

        return summary.toString();
    }

    /**
     * Generates a summary of files related to a given file through dependencies or shared symbols.
     * @param filePath The relative path of the file.
     * @return A string containing the related files summary, or an error message if the file is not found.
     */
    public String getRelatedFilesSummary(String filePath) {
        if (!fileIndex.containsKey(filePath)) {
            return "File not found in index: " + filePath;
        }

        StringBuilder summary = new StringBuilder();
        Set<String> relatedFiles = new HashSet<>();

        // Add direct dependencies
        relatedFiles.addAll(getFileDependencies(filePath));

        // Add files that depend on this one
        relatedFiles.addAll(getFileDependents(filePath));

        // Add files that define symbols used in this file
        for (String symbol : getSymbolsUsedInFile(filePath)) {
            List<String> filesWithSymbol = symbolDefinitionIndex.get(symbol);
            if (filesWithSymbol != null) {
                relatedFiles.addAll(filesWithSymbol);
            }
        }

        // Add files that use symbols defined in this file
        for (String symbol : getSymbolsDefinedInFile(filePath)) {
            List<String> filesUsingSymbol = symbolUsageIndex.get(symbol);
            if (filesUsingSymbol != null) {
                relatedFiles.addAll(filesUsingSymbol);
            }
        }

        // Remove self from related files
        relatedFiles.remove(filePath);

        if (relatedFiles.isEmpty()) {
            return "No related files found for " + filePath + ".\n";
        }

        summary.append("Files related to ").append(filePath).append(":\n");
        // Sort for consistent output
        List<String> sortedRelatedFiles = new ArrayList<>(relatedFiles);
        Collections.sort(sortedRelatedFiles);
        for (String file : sortedRelatedFiles) {
            summary.append("  - ").append(file).append("\n");
        }

        return summary.toString();
    }

    /**
     * Detects common key files that might be entry points or important configurations.
     * @return A list of descriptions for key files found.
     */
    private List<String> detectKeyFiles() {
        List<String> keyFiles = new ArrayList<>();

        if (fileIndex.containsKey("index.html")) keyFiles.add("index.html (main HTML entry point)");
        if (fileIndex.containsKey("main.js")) keyFiles.add("main.js (common main JavaScript file)");
        if (fileIndex.containsKey("app.js")) keyFiles.add("app.js (common application JavaScript file)");
        if (fileIndex.containsKey("index.js")) keyFiles.add("index.js (common main JavaScript file)");
        if (fileIndex.containsKey("styles.css")) keyFiles.add("styles.css (main stylesheet)");
        if (fileIndex.containsKey("package.json")) keyFiles.add("package.json (Node.js/NPM project configuration)");
        if (fileIndex.containsKey("README.md")) keyFiles.add("README.md (project documentation)");
        // Add more common key files as needed

        return keyFiles;
    }

    /**
     * Detects key components or widely used symbols across the codebase.
     * @return A list of descriptions for key components/symbols.
     */
    private List<String> detectKeyComponents() {
        List<String> components = new ArrayList<>();
        
        // Identify symbols defined in multiple files (potential shared utilities/components)
        for (Map.Entry<String, List<String>> entry : symbolDefinitionIndex.entrySet()) {
            String symbol = entry.getKey();
            List<String> files = entry.getValue();
            if (files.size() > 1) { // Symbol defined in more than one file
                components.add(symbol + " (defined in " + files.size() + " files)");
            }
        }

        // Heuristic: Capitalized symbols in JS files might be components/classes
        for (Map.Entry<String, List<String>> entry : symbolDefinitionIndex.entrySet()) {
            String symbol = entry.getKey();
            List<String> files = entry.getValue();
            if (symbol.matches("^[A-Z][a-zA-Z0-9_$]*") && files.stream().anyMatch(f -> getFileType(f).equals("js"))) {
                // Avoid adding if already added as a multi-file symbol
                boolean alreadyAdded = components.stream().anyMatch(c -> c.startsWith(symbol + " "));
                if (!alreadyAdded) {
                    components.add(symbol + " (likely JS component/class)");
                }
            }
        }

        // Common HTML IDs/Classes that might indicate main application structure
        if (symbolDefinitionIndex.containsKey("#app")) components.add("#app (main application container ID)");
        if (symbolDefinitionIndex.containsKey("#root")) components.add("#root (React/Vue root element ID)");
        if (symbolDefinitionIndex.containsKey(".container")) components.add(".container (common layout class, e.g., Bootstrap)");

        // Sort for consistent output
        Collections.sort(components);
        return components;
    }

    /**
     * Gets the files that are most depended upon by other files in the project.
     * @return A map where keys are file paths and values are the number of files depending on them.
     */
    private Map<String, Integer> getMostDependentFiles() {
        Map<String, Integer> dependencyCounts = new HashMap<>();

        for (FileDependency dep : dependencyGraph) {
            dependencyCounts.put(
                    dep.targetFile,
                    dependencyCounts.getOrDefault(dep.targetFile, 0) + 1
            );
        }

        // Sort by most depended-on (descending)
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(dependencyCounts.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        Map<String, Integer> result = new HashMap<>();
        // Return top 5 most depended-on files
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            result.put(sorted.get(i).getKey(), sorted.get(i).getValue());
        }

        return result;
    }

    /**
     * Gets a list of files that have no other files depending on them.
     * These could be entry points or potentially unused files.
     * @return A list of file paths.
     */
    private List<String> getFilesWithNoDependents() {
        Set<String> allFiles = new HashSet<>(fileIndex.keySet());
        Set<String> filesWithDependents = new HashSet<>();

        for (FileDependency dep : dependencyGraph) {
            filesWithDependents.add(dep.targetFile);
        }

        allFiles.removeAll(filesWithDependents); // Remove files that are targets of any dependency

        List<String> result = new ArrayList<>(allFiles);
        Collections.sort(result); // Sort for consistent output
        return result;
    }

    /**
     * Retrieves a list of symbols defined within a specific file.
     * @param filePath The relative path of the file.
     * @return A list of symbols defined in the file.
     */
    public List<String> getSymbolsDefinedInFile(String filePath) { // FIX: Made public
        List<String> symbols = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : symbolDefinitionIndex.entrySet()) {
            if (entry.getValue().contains(filePath)) {
                symbols.add(entry.getKey());
            }
        }
        Collections.sort(symbols);
        return symbols;
    }

    /**
     * Retrieves a list of symbols used within a specific file.
     * @param filePath The relative path of the file.
     * @return A list of symbols used in the file.
     */
    public List<String> getSymbolsUsedInFile(String filePath) { // FIX: Made public
        List<String> symbols = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : symbolUsageIndex.entrySet()) {
            if (entry.getValue().contains(filePath)) {
                symbols.add(entry.getKey());
            }
        }
        Collections.sort(symbols);
        return symbols;
    }

    /**
     * Retrieves a list of files where a given symbol is defined.
     * @param symbol The symbol name.
     * @return A list of relative paths of files where the symbol is defined.
     */
    public List<String> getSymbolDefinitions(String symbol) { // FIX: Made public
        List<String> definitions = symbolDefinitionIndex.get(symbol);
        return definitions != null ? new ArrayList<>(definitions) : Collections.emptyList();
    }

    /**
     * Retrieves a list of files that a given file directly depends on.
     * @param filePath The relative path of the source file.
     * @return A list of relative paths of dependent files.
     */
    private List<String> getFileDependencies(String filePath) {
        List<String> deps = new ArrayList<>();
        for (FileDependency dep : dependencyGraph) {
            if (dep.sourceFile.equals(filePath)) {
                deps.add(dep.targetFile);
            }
        }
        Collections.sort(deps);
        return deps;
    }

    /**
     * Retrieves a list of files that directly depend on a given file.
     * @param filePath The relative path of the target file.
     * @return A list of relative paths of files that depend on the target file.
     */
    public List<String> getFileDependents(String filePath) {
        List<String> dependents = new ArrayList<>();
        for (FileDependency dep : dependencyGraph) {
            if (dep.targetFile.equals(filePath)) {
                dependents.add(dep.sourceFile);
            }
        }
        Collections.sort(dependents);
        return dependents;
    }

    /**
     * Determines the file type based on its extension.
     * @param fileName The name of the file.
     * @return A string representing the file type (e.g., "html", "js", "css", "other").
     */
    private String getFileType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "html";
        if (fileName.endsWith(".css")) return "css";
        if (fileName.endsWith(".js") || fileName.endsWith(".jsx")) return "js";
        if (fileName.endsWith(".json")) return "json";
        if (fileName.endsWith(".xml")) return "xml";
        if (fileName.endsWith(".txt")) return "txt";
        if (fileName.endsWith(".md")) return "md";
        return "other";
    }
}
