package com.codex.apk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * FileSearchHelper provides file and content search functionality within projects.
 */
public class FileSearchHelper {
    
    /**
     * Represents a search result with file and line information.
     */
    public static class SearchResult {
        private final File file;
        private final String fileName;
        private final int lineNumber;
        private final String lineContent;
        private final String matchedText;
        
        public SearchResult(File file, String fileName, int lineNumber, String lineContent, String matchedText) {
            this.file = file;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchedText = matchedText;
        }
        
        public File getFile() { return file; }
        public String getFileName() { return fileName; }
        public int getLineNumber() { return lineNumber; }
        public String getLineContent() { return lineContent; }
        public String getMatchedText() { return matchedText; }
    }
    
    /**
     * Searches for files by name pattern in the project directory.
     * @param projectDir The project directory to search in
     * @param pattern The file name pattern to search for
     * @param caseSensitive Whether the search should be case sensitive
     * @return List of matching files
     */
    public static List<File> searchFilesByName(File projectDir, String pattern, boolean caseSensitive) {
        List<File> results = new ArrayList<>();
        
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory()) {
            return results;
        }
        
        String searchPattern = caseSensitive ? pattern : pattern.toLowerCase();
        searchFilesByNameRecursive(projectDir, searchPattern, caseSensitive, results);
        
        return results;
    }
    
    /**
     * Searches for text content within files in the project directory.
     * @param projectDir The project directory to search in
     * @param searchText The text to search for
     * @param caseSensitive Whether the search should be case sensitive
     * @param useRegex Whether to treat searchText as a regular expression
     * @param fileExtensions List of file extensions to search in (null for all files)
     * @param maxResults Maximum number of results to return
     * @return List of search results
     */
    public static List<SearchResult> searchInFiles(File projectDir, String searchText, boolean caseSensitive, 
                                                  boolean useRegex, List<String> fileExtensions, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory() || 
            searchText == null || searchText.trim().isEmpty()) {
            return results;
        }
        
        Pattern searchPattern = null;
        if (useRegex) {
            try {
                searchPattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                // If regex is invalid, fall back to literal search
                useRegex = false;
            }
        }
        
        searchInFilesRecursive(projectDir, searchText, searchPattern, caseSensitive, useRegex, 
                              fileExtensions, results, maxResults);
        
        return results;
    }
    
    /**
     * Gets recently modified files in the project.
     * @param projectDir The project directory
     * @param maxFiles Maximum number of files to return
     * @return List of recently modified files
     */
    public static List<File> getRecentFiles(File projectDir, int maxFiles) {
        List<File> allFiles = new ArrayList<>();
        getAllFilesRecursive(projectDir, allFiles);
        
        // Sort by last modified time (most recent first)
        allFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        // Return only the requested number of files
        return allFiles.subList(0, Math.min(maxFiles, allFiles.size()));
    }
    
    /**
     * Recursively searches for files by name pattern.
     */
    private static void searchFilesByNameRecursive(File dir, String pattern, boolean caseSensitive, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Skip hidden directories and common build/cache directories
                String dirName = file.getName();
                if (!dirName.startsWith(".") && !dirName.equals("node_modules") && 
                    !dirName.equals("build") && !dirName.equals("dist")) {
                    searchFilesByNameRecursive(file, pattern, caseSensitive, results);
                }
            } else {
                String fileName = caseSensitive ? file.getName() : file.getName().toLowerCase();
                if (fileName.contains(pattern)) {
                    results.add(file);
                }
            }
        }
    }
    
    /**
     * Recursively searches for text content in files.
     */
    private static void searchInFilesRecursive(File dir, String searchText, Pattern searchPattern, 
                                              boolean caseSensitive, boolean useRegex, List<String> fileExtensions,
                                              List<SearchResult> results, int maxResults) {
        if (results.size() >= maxResults) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (results.size() >= maxResults) break;
            
            if (file.isDirectory()) {
                // Skip hidden directories and common build/cache directories
                String dirName = file.getName();
                if (!dirName.startsWith(".") && !dirName.equals("node_modules") && 
                    !dirName.equals("build") && !dirName.equals("dist")) {
                    searchInFilesRecursive(file, searchText, searchPattern, caseSensitive, useRegex,
                                          fileExtensions, results, maxResults);
                }
            } else {
                // Check file extension
                if (fileExtensions != null && !fileExtensions.isEmpty()) {
                    String fileName = file.getName().toLowerCase();
                    boolean matchesExtension = false;
                    for (String ext : fileExtensions) {
                        if (fileName.endsWith("." + ext.toLowerCase())) {
                            matchesExtension = true;
                            break;
                        }
                    }
                    if (!matchesExtension) continue;
                }
                
                // Search within the file
                searchInFile(file, searchText, searchPattern, caseSensitive, useRegex, results, maxResults);
            }
        }
    }
    
    /**
     * Searches for text content within a single file.
     */
    private static void searchInFile(File file, String searchText, Pattern searchPattern, 
                                    boolean caseSensitive, boolean useRegex, List<SearchResult> results, int maxResults) {
        if (results.size() >= maxResults) return;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null && results.size() < maxResults) {
                String searchLine = caseSensitive ? line : line.toLowerCase();
                String searchTarget = caseSensitive ? searchText : searchText.toLowerCase();
                
                if (useRegex && searchPattern != null) {
                    if (searchPattern.matcher(line).find()) {
                        results.add(new SearchResult(file, file.getName(), lineNumber, line.trim(), searchText));
                    }
                } else {
                    if (searchLine.contains(searchTarget)) {
                        results.add(new SearchResult(file, file.getName(), lineNumber, line.trim(), searchText));
                    }
                }
                
                lineNumber++;
            }
        } catch (IOException e) {
            // Skip files that can't be read
        }
    }
    
    /**
     * Recursively gets all files in a directory.
     */
    private static void getAllFilesRecursive(File dir, List<File> files) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles == null) return;
        
        for (File file : dirFiles) {
            if (file.isDirectory()) {
                // Skip hidden directories and common build/cache directories
                String dirName = file.getName();
                if (!dirName.startsWith(".") && !dirName.equals("node_modules") && 
                    !dirName.equals("build") && !dirName.equals("dist")) {
                    getAllFilesRecursive(file, files);
                }
            } else {
                files.add(file);
            }
        }
    }
}