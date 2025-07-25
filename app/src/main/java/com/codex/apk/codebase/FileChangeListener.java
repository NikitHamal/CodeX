package com.codex.apk.codebase;

import java.io.File;

/**
 * Interface for listeners that want to be notified of file system changes.
 */
public interface FileChangeListener {
    /**
     * Called when a new file is created.
     * @param file The newly created file.
     */
    void onFileCreated(File file);

    /**
     * Called when an existing file is modified.
     * @param file The modified file.
     */
    void onFileModified(File file);

    /**
     * Called when a file or directory is deleted.
     * @param file The deleted file or directory.
     */
    void onFileDeleted(File file);
}
