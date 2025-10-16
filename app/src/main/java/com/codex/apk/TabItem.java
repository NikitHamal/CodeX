package com.codex.apk;

import java.io.File;

public class TabItem {
    public enum TabType {
        FILE, FRAGMENT
    }

    private File file;
    private String content;
    private boolean modified;
    private boolean lastNotifiedModifiedState;
    private boolean wrapEnabled = false;
    private boolean readOnly = false;
    private final TabType tabType;
    private androidx.fragment.app.Fragment fragment;
    private String title;

    public TabItem(File file, String initialContent) {
        this.file = file;
        this.content = initialContent;
        this.modified = false;
        this.lastNotifiedModifiedState = false;
        this.tabType = TabType.FILE;
    }

    public TabItem(String title, androidx.fragment.app.Fragment fragment) {
        this.title = title;
        this.fragment = fragment;
        this.tabType = TabType.FRAGMENT;
    }

    /**
     * Reloads the content of the tab from the file system.
     * @param fileManager The FileManager instance to use for reading the file.
     * @return True if the content was reloaded successfully, false otherwise.
     */
    public boolean reloadContent(FileManager fileManager) {
        try {
            String newContent = fileManager.readFileContent(file);
            setContent(newContent);
            setModified(false); // After reloading, it's no longer modified
            setLastNotifiedModifiedState(false);
            return true;
        } catch (Exception e) {
            // Log the error or handle it as needed
            return false;
        }
    }
    
    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }
    public String getFileName() {
        if (file != null) {
            return file.getName();
        }
        return title;
    }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isModified() { return modified; }
    public void setModified(boolean modified) { this.modified = modified; }
    public boolean getLastNotifiedModifiedState() { return lastNotifiedModifiedState; }
    public void setLastNotifiedModifiedState(boolean lastNotifiedModifiedState) { this.lastNotifiedModifiedState = lastNotifiedModifiedState; }

    public boolean isWrapEnabled() { return wrapEnabled; }
    public void setWrapEnabled(boolean wrapEnabled) { this.wrapEnabled = wrapEnabled; }

    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    public TabType getTabType() { return tabType; }
    public androidx.fragment.app.Fragment getFragment() { return fragment; }
    public String getTitle() { return title; }
}
