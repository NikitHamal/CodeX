package com.codex.apk.codebase;

/**
 * Represents metadata for an indexed file.
 */
public class FileMetadata {
    public final String path; // Relative path from project root
    public final String name; // File name
    public final String type; // File type (e.g., "js", "html")
    public final String content; // Full content of the file (for analysis, not for storage in large scale)
    public final long lastIndexed; // Timestamp of last indexing

    public FileMetadata(String path, String name, String type, String content, long lastIndexed) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.content = content;
        this.lastIndexed = lastIndexed;
    }
}
