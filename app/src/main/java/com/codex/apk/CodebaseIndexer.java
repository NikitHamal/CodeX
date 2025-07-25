package com.codex.apk;

import android.util.Log;
import com.codex.apk.codebase.CodebaseSummarizer;
import com.codex.apk.codebase.CssSymbolIndexer;
import com.codex.apk.codebase.DependencyAnalyzer;
import com.codex.apk.codebase.FileDependency;
import com.codex.apk.codebase.FileMetadata;
import com.codex.apk.codebase.HtmlSymbolIndexer;
import com.codex.apk.codebase.JavaScriptSymbolIndexer;
import com.codex.apk.codebase.SymbolIndexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CodebaseIndexer {
    private static final String TAG = "CodebaseIndexer";
    private final File projectDir;
    private final FileManager fileManager;
    private final IndexingListener listener;

    // Index data structures
    private final Map<String, FileMetadata> fileIndex = new HashMap<>();
    private final Map<String, List<String>> symbolDefinitionIndex = new HashMap<>(); // Maps symbol name to list of files where it's defined
    private final Map<String, List<String>> symbolUsageIndex = new HashMap<>(); // Maps symbol name to list of files where it's used
    private final List<FileDependency> dependencyGraph = new ArrayList<>(); // Stores direct file-to-file dependencies
    private final Map<String, List<String>> fileTypeIndex = new HashMap<>(); // Maps file type (e.g., "js", "html") to list of relative paths

    // Modularized components
    private final Map<String, SymbolIndexer> symbolIndexers;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final CodebaseSummarizer codebaseSummarizer;

    /**
     * Listener interface for indexing progress updates.
     */
    public interface IndexingListener {
        void onIndexingStarted(int totalFiles);
        void onIndexingProgress(int indexedCount, int totalFiles, String currentFile);
        void onIndexingCompleted();
        void onIndexingError(String errorMessage);
    }

    public CodebaseIndexer(File projectDir, FileManager fileManager, IndexingListener listener) {
        this.projectDir = projectDir;
        this.fileManager = fileManager;
        this.listener = listener;
        initializeFileTypeIndex();

        // Initialize modular components
        symbolIndexers = new HashMap<>();
        symbolIndexers.put("js", new JavaScriptSymbolIndexer());
        symbolIndexers.put("html", new HtmlSymbolIndexer());
        symbolIndexers.put("css", new CssSymbolIndexer()); // CSS symbol indexing remains definition-only for now

        this.dependencyAnalyzer = new DependencyAnalyzer(projectDir, fileManager, fileIndex, dependencyGraph);
        // FIX: Pass both symbolDefinitionIndex and symbolUsageIndex to CodebaseSummarizer constructor
        this.codebaseSummarizer = new CodebaseSummarizer(fileIndex, symbolDefinitionIndex, symbolUsageIndex, dependencyGraph, fileTypeIndex, fileManager);
    }

    /**
     * Initializes the map that categorizes files by their type.
     */
    private void initializeFileTypeIndex() {
        fileTypeIndex.put("html", new ArrayList<>());
        fileTypeIndex.put("css", new ArrayList<>());
        fileTypeIndex.put("js", new ArrayList<>());
        fileTypeIndex.put("json", new ArrayList<>());
        fileTypeIndex.put("xml", new ArrayList<>());
        fileTypeIndex.put("txt", new ArrayList<>());
        fileTypeIndex.put("md", new ArrayList<>());
        fileTypeIndex.put("other", new ArrayList<>());
    }

    /**
     * Performs an incremental indexing of the project directory.
     * It detects new, modified, and deleted files and updates the index accordingly.
     */
    public void indexProject() {
        Log.d(TAG, "Starting incremental project indexing...");

        Set<String> filesOnDisk = new HashSet<>();
        List<File> allFiles = new ArrayList<>();
        collectAllFiles(projectDir, allFiles);

        for (File file : allFiles) {
            if (file.isFile()) {
                try {
                    String relativePath = fileManager.getRelativePath(file, projectDir);
                    if (relativePath != null) {
                        filesOnDisk.add(relativePath);
                    }
                } catch (Exception e) { // FIX: Changed to generic Exception to avoid unreachable catch
                    Log.e(TAG, "Error getting relative path for file: " + file.getAbsolutePath(), e);
                }
            }
        }

        // Identify deleted files
        Set<String> indexedFiles = new HashSet<>(fileIndex.keySet());
        Set<String> deletedFiles = new HashSet<>(indexedFiles);
        deletedFiles.removeAll(filesOnDisk);

        // Identify new and modified files
        Set<String> filesToProcess = new HashSet<>(filesOnDisk);
        filesToProcess.removeAll(indexedFiles); // Add new files

        for (String relativePath : indexedFiles) {
            if (filesOnDisk.contains(relativePath)) {
                File file = new File(projectDir, relativePath);
                FileMetadata metadata = fileIndex.get(relativePath);
                if (file.lastModified() > metadata.lastIndexed) {
                    filesToProcess.add(relativePath); // Add modified files
                }
            }
        }

        final int totalFilesToProcess = filesToProcess.size() + deletedFiles.size();
        final AtomicInteger processedCount = new AtomicInteger(0);

        if (listener != null) {
            listener.onIndexingStarted(totalFilesToProcess);
        }

        try {
            // Process deleted files first to clean up indices
            for (String relativePath : deletedFiles) {
                removeFileFromIndex(relativePath);
                processedCount.incrementAndGet();
                if (listener != null) {
                    listener.onIndexingProgress(processedCount.get(), totalFilesToProcess, relativePath + " (deleted)");
                }
                Log.d(TAG, "Removed deleted file from index: " + relativePath);
            }

            // Process new and modified files
            for (String relativePath : filesToProcess) {
                File file = new File(projectDir, relativePath);
                if (file.exists() && file.isFile()) {
                    indexFile(file);
                    processedCount.incrementAndGet();
                    if (listener != null) {
                        listener.onIndexingProgress(processedCount.get(), totalFilesToProcess, file.getName());
                    }
                    Log.d(TAG, "Indexed/Re-indexed file: " + relativePath);
                } else {
                    // This case might happen if a file was marked for processing but then deleted before its turn
                    Log.w(TAG, "File no longer exists, skipping re-indexing: " + relativePath);
                    processedCount.incrementAndGet(); // Still count it as processed
                }
            }

            // Re-analyze all dependencies as changes in any file can affect the graph
            dependencyGraph.clear(); // Clear before re-analyzing
            dependencyAnalyzer.analyzeDependencies();
            Log.d(TAG, "Project incremental indexing completed. Files indexed: " + fileIndex.size());
            if (listener != null) {
                listener.onIndexingCompleted();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during incremental project indexing: " + e.getMessage(), e);
            if (listener != null) {
                listener.onIndexingError("Error during indexing: " + e.getMessage());
            }
        }
    }

    /**
     * Recursively collects all files in a directory.
     * @param directory The directory to scan.
     * @param fileList The list to add files to.
     */
    private void collectAllFiles(File directory, List<File> fileList) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectAllFiles(file, fileList);
            } else {
                fileList.add(file);
            }
        }
    }

    /**
     * Indexes a single file, updating its metadata, symbols, and type in the index.
     * If the file was already indexed, its old entries are removed first.
     * @param file The file to index.
     */
    public void indexFile(File file) {
        try {
            String relativePath = fileManager.getRelativePath(file, projectDir);
            if (relativePath == null) {
                Log.e(TAG, "Could not get relative path for file: " + file.getAbsolutePath());
                return;
            }
            String content = fileManager.readFileContent(file);
            String fileType = getFileType(file.getName());

            // Remove old index entries if this is an update
            removeFileFromIndex(relativePath);

            FileMetadata metadata = new FileMetadata(
                    relativePath,
                    file.getName(),
                    fileType,
                    content,
                    System.currentTimeMillis()
            );

            fileIndex.put(relativePath, metadata);
            fileTypeIndex.computeIfAbsent(fileType, k -> new ArrayList<>()).add(relativePath); // Ensure list exists
            
            // Delegate symbol indexing to appropriate handler
            SymbolIndexer indexer = symbolIndexers.get(fileType);
            if (indexer != null) {
                // FIX: Pass both symbolDefinitionIndex and symbolUsageIndex
                indexer.indexSymbols(relativePath, content, symbolDefinitionIndex, symbolUsageIndex);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error indexing file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Removes all index entries associated with a given file path.
     * @param relativePath The relative path of the file to remove.
     */
    public void removeFileFromIndex(String relativePath) {
        FileMetadata oldMeta = fileIndex.remove(relativePath);
        if (oldMeta != null) {
            fileTypeIndex.get(oldMeta.type).remove(relativePath);
            // Remove symbols defined in this file
            symbolDefinitionIndex.forEach((symbol, files) -> files.remove(relativePath));
            symbolDefinitionIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty()); // Clean up empty symbol entries

            // Remove symbol usages from this file
            symbolUsageIndex.forEach((symbol, files) -> files.remove(relativePath));
            symbolUsageIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty()); // Clean up empty symbol usage entries

            // Remove dependencies involving this file
            dependencyGraph.removeIf(dep ->
                    dep.sourceFile.equals(relativePath) || dep.targetFile.equals(relativePath)
            );
        }
    }

    /**
     * Updates the index for a file that has been renamed or moved.
     * @param oldPath The old relative path of the file.
     * @param newPath The new relative path of the file.
     * @param newFile The new File object.
     */
    public void updateFileIndex(String oldPath, String newPath, File newFile) {
        removeFileFromIndex(oldPath);
        indexFile(newFile);
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

    /**
     * Retrieves the metadata for a specific file.
     * @param filePath The relative path of the file.
     * @return FileMetadata object, or null if not found.
     */
    public FileMetadata getFileMetadata(String filePath) {
        return fileIndex.get(filePath);
    }

    /**
     * Retrieves a list of symbols defined within a specific file.
     * @param filePath The relative path of the file.
     * @return A list of symbols defined in the file.
     */
    public List<String> getSymbolsDefinedInFile(String filePath) {
        return codebaseSummarizer.getSymbolsDefinedInFile(filePath);
    }

    /**
     * Retrieves a list of symbols used within a specific file.
     * @param filePath The relative path of the file.
     * @return A list of symbols used in the file.
     */
    public List<String> getSymbolsUsedInFile(String filePath) {
        return codebaseSummarizer.getSymbolsUsedInFile(filePath);
    }

    /**
     * Retrieves a list of files where a given symbol is defined.
     * @param symbol The symbol name.
     * @return A list of relative paths of files where the symbol is defined.
     */
    public List<String> getSymbolDefinitions(String symbol) {
        return codebaseSummarizer.getSymbolDefinitions(symbol);
    }

    // --- Summary Methods (Delegated to CodebaseSummarizer) ---

    /**
     * Generates a summary of the entire codebase, including statistics, key files,
     * key components, and dependency analysis.
     * @return A string containing the codebase summary.
     */
    public String getCodebaseSummary() {
        return codebaseSummarizer.getCodebaseSummary();
    }

    /**
     * Generates a detailed summary for a specific file.
     * @param filePath The relative path of the file.
     * @return A string containing the file summary, or an error message if the file is not found.
     */
    public String getFileSummary(String filePath) {
        return codebaseSummarizer.getFileSummary(filePath);
    }

    /**
     * Generates a summary of files related to a given file through dependencies or shared symbols.
     * @param filePath The relative path of the file.
     * @return A string containing the related files summary, or an error message if the file is not found.
     */
    public String getRelatedFilesSummary(String filePath) {
        return codebaseSummarizer.getRelatedFilesSummary(filePath);
    }

    /**
     * Retrieves a list of files that directly depend on a given file.
     * @param filePath The relative path of the target file.
     * @return A list of relative paths of files that depend on the target file.
     */
    public List<String> getFileDependents(String filePath) {
        return codebaseSummarizer.getFileDependents(filePath);
    }
}
