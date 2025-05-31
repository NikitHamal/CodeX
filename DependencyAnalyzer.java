package com.codex.apk.codebase;

import android.util.Log;
import com.codex.apk.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Analyzes dependencies between files based on import/require statements and resource links.
 */
public class DependencyAnalyzer {
    private static final String TAG = "DependencyAnalyzer";
    private final File projectDir;
    private final FileManager fileManager;
    private final Map<String, FileMetadata> fileIndex;
    private final List<FileDependency> dependencyGraph;

    public DependencyAnalyzer(File projectDir, FileManager fileManager,
                              Map<String, FileMetadata> fileIndex,
                              List<FileDependency> dependencyGraph) {
        this.projectDir = projectDir;
        this.fileManager = fileManager;
        this.fileIndex = fileIndex;
        this.dependencyGraph = dependencyGraph;
    }

    /**
     * Analyzes dependencies for all files in the index and populates the dependency graph.
     */
    public void analyzeDependencies() {
        for (Map.Entry<String, FileMetadata> entry : fileIndex.entrySet()) {
            String filePath = entry.getKey();
            FileMetadata metadata = entry.getValue();

            List<String> dependencies = findDependencies(metadata.content, metadata.type);
            for (String dep : dependencies) {
                // Resolve relative paths and add to dependency graph if the target file exists in the index
                String resolvedPath = resolveImportPath(filePath, dep);
                if (resolvedPath != null && fileIndex.containsKey(resolvedPath)) {
                    dependencyGraph.add(new FileDependency(filePath, resolvedPath));
                }
            }
        }
    }

    /**
     * Finds direct dependencies (imports, links) within a file's content.
     * @param content The content of the file.
     * @param fileType The type of the file.
     * @return A list of paths that the file depends on.
     */
    private List<String> findDependencies(String content, String fileType) {
        List<String> dependencies = new ArrayList<>();

        switch (fileType) {
            case "js":
                Matcher importRequireMatcher = CodebasePatterns.JS_IMPORT_REQUIRE_PATTERN.matcher(content);
                while (importRequireMatcher.find()) {
                    String path = importRequireMatcher.group(1); // Try import group first
                    if (path == null) {
                        path = importRequireMatcher.group(2); // If null, try require group
                    }
                    if (path != null) {
                        dependencies.add(path);
                    }
                }
                break;

            case "html":
                Matcher resourceMatcher = CodebasePatterns.HTML_RESOURCE_PATTERN.matcher(content);
                while (resourceMatcher.find()) {
                    String path = resourceMatcher.group(1);
                    dependencies.add(path);
                }
                break;

            case "css":
                Matcher cssImportMatcher = CodebasePatterns.CSS_IMPORT_PATTERN.matcher(content);
                while (cssImportMatcher.find()) {
                    String path = cssImportMatcher.group(1);
                    dependencies.add(path);
                }
                break;
        }

        return dependencies;
    }

    /**
     * Resolves an import path relative to the source file's path.
     * @param sourceFilePath The relative path of the file containing the import.
     * @param importPath The path specified in the import/require statement.
     * @return The resolved relative path of the imported file, or null if it cannot be resolved.
     */
    private String resolveImportPath(String sourceFilePath, String importPath) {
        if (importPath.startsWith(".")) {
            // Relative path (e.g., './module.js', '../components/button.js')
            File sourceFile = new File(projectDir, sourceFilePath);
            File parentDir = sourceFile.getParentFile();
            if (parentDir == null) return null;

            File importedFile = new File(parentDir, importPath);
            try {
                // Get canonical path to resolve '..' and '.'
                String canonicalPath = importedFile.getCanonicalPath();
                // Ensure the canonical path is still within the project directory
                if (canonicalPath.startsWith(projectDir.getCanonicalPath())) {
                    return fileManager.getRelativePath(new File(canonicalPath), projectDir);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error resolving import path: " + importPath + " from " + sourceFilePath, e);
                return null;
            }
        } else if (!importPath.contains("/") && !importPath.contains("\\")) {
            // Bare specifier (e.g., 'react', 'lodash'). Assume it might be a local file if it doesn't contain path separators.
            // This is a heuristic; a real module resolver would be more complex.
            // Check if a file with this name (plus common extensions) exists in the project.
            // This is a simplified check.
            String[] commonExtensions = {".js", ".jsx", ".ts", ".tsx", ".json", ".css", ".html"};
            for (String ext : commonExtensions) {
                File potentialFile = new File(projectDir, importPath + ext);
                if (potentialFile.exists() && potentialFile.isFile()) {
                    return fileManager.getRelativePath(potentialFile, projectDir);
                }
                // Also check for directory imports (e.g., 'components' -> 'components/index.js')
                File potentialIndex = new File(new File(projectDir, importPath), "index" + ext);
                if (potentialIndex.exists() && potentialIndex.isFile()) {
                    return fileManager.getRelativePath(potentialIndex, projectDir);
                }
            }
        }
        // For absolute paths or unresolved bare specifiers, return as-is.
        // The `fileIndex.containsKey(resolvedPath)` check will filter out non-existent files.
        return importPath;
    }
}
