package com.codex.apk.codebase;

/**
 * Represents a direct dependency relationship between two files.
 */
public class FileDependency {
    public final String sourceFile; // The file that depends on another
    public final String targetFile; // The file being depended upon

    public FileDependency(String sourceFile, String targetFile) {
        this.sourceFile = sourceFile;
        this.targetFile = targetFile;
    }
}
