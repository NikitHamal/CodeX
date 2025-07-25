package com.codex.apk;

import java.io.File;

public class FileItem {
    private final File file;
    private final int level;
    // private final FileItem parent; // Not strictly needed by adapter if tree is flat list
    private boolean expanded;
    
    public FileItem(File file, int level, FileItem parent) {
        this.file = file;
        this.level = level;
        // this.parent = parent;
        this.expanded = (level == 0 && file.isDirectory()); 
    }
    
    public File getFile() { return file; }
    public String getName() { return file.getName(); }
    public int getLevel() { return level; }
    public boolean isDirectory() { return file.isDirectory(); }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public void toggleExpanded() { this.expanded = !this.expanded; }
}
