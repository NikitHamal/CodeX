package com.codex.apk;

import java.io.File;

public class TabItem {
    private File file;
    private String content; 
    private boolean modified;
    
    public TabItem(File file, String initialContent) {
        this.file = file;
        this.content = initialContent;
        this.modified = false;
    }
    
    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; } 
    public String getFileName() { return file.getName(); }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isModified() { return modified; }
    public void setModified(boolean modified) { this.modified = modified; }
}
