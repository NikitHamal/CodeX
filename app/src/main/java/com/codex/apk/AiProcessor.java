package com.codex.apk;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced AI Processor with advanced file operations.
 * Merges updateFile and searchAndReplace with versioning, diff generation, and smart updates.
 */
import com.google.gson.Gson;

public class AiProcessor {
    private static final String TAG = "AiProcessor";
    private static final Gson gson = new Gson();
    private final File projectDir;
    private final Context context;
    private final AdvancedFileManager advancedFileManager;

    public AiProcessor(File projectDir, Context context) {
        this.projectDir = projectDir;
        this.context = context;
        this.advancedFileManager = new AdvancedFileManager(context, projectDir);
    }

    /**
     * Applies a single file action as described by a FileActionDetail object.
     * This method performs the actual file system modification with advanced features.
     *
     * @param detail The FileActionDetail containing the action to apply.
     * @return A summary string of the applied action.
     * @throws IOException If a file operation fails.
     * @throws IllegalArgumentException If the action type is unknown or parameters are invalid.
     */
    public String applyFileAction(ChatMessage.FileActionDetail detail) throws IOException, IllegalArgumentException {
        Log.d(TAG, "Applying file action: " + gson.toJson(detail));
        String actionType = detail.type;
        String summary = "";

        switch (actionType) {
            case "createFile":
                summary = handleCreateFile(detail);
                break;
            case "updateFile":
            case "smartUpdate":
                summary = handleAdvancedUpdateFile(detail);
                break;
            case "deleteFile":
                summary = handleDeleteFile(detail);
                break;
            case "renameFile":
                summary = handleRenameFile(detail);
                break;
            case "searchAndReplace":
                summary = handleSearchAndReplace(detail);
                break;
            case "patchFile":
                summary = handlePatchFile(detail);
                break;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
        return summary;
    }

    /**
     * Enhanced file update with advanced features
     */
    private String handleAdvancedUpdateFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String content = detail.newContent;
        File fileToUpdate = new File(projectDir, path);
        
        if (!fileToUpdate.exists()) {
            throw new IOException("File not found for update: " + path);
        }

        // Determine update type
        String updateType = detail.updateType != null ? detail.updateType : "full";
        boolean createBackup = detail.createBackup;
        boolean validateContent = detail.validateContent;
        String contentType = detail.contentType;
        String errorHandling = detail.errorHandling != null ? detail.errorHandling : "strict";

        // Use advanced file manager for smart update
        AdvancedFileManager.FileOperationResult result = advancedFileManager.smartUpdateFile(
            fileToUpdate, content, updateType, createBackup, validateContent, contentType, errorHandling
        );

        if (!result.isSuccess()) {
            throw new IOException("Update failed: " + result.getMessage());
        }

        // Build comprehensive summary
        StringBuilder summary = new StringBuilder();
        summary.append("Updated file: ").append(path);
        
        if (result.getVersionId() != null) {
            summary.append(" (version: ").append(result.getVersionId()).append(")");
        }
        
        if (result.getBackupFile() != null) {
            summary.append(" (backup created)");
        }
        
        if (result.isReverted()) {
            summary.append(" (reverted due to validation failure)");
        }

        return summary.toString();
    }

    /**
     * Enhanced search and replace with regex support
     */
    private String handleSearchAndReplace(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String search = detail.search;
        String replace = detail.replace;
        String searchPattern = detail.searchPattern;
        File fileToUpdate = new File(projectDir, path);
        
        if (!fileToUpdate.exists()) {
            throw new IOException("File not found for search and replace: " + path);
        }

        String content = advancedFileManager.readFileContent(fileToUpdate);
        String newContent;

        if (searchPattern != null && !searchPattern.isEmpty()) {
            // Use regex pattern
            try {
                newContent = content.replaceAll(searchPattern, replace);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid regex pattern: " + searchPattern);
            }
        } else {
            // Use simple string replacement
            newContent = content.replace(search, replace);
        }

        // Use advanced file manager for the update
        AdvancedFileManager.FileOperationResult result = advancedFileManager.smartUpdateFile(
            fileToUpdate, newContent, "replace", true, true, detail.contentType, "strict"
        );

        if (!result.isSuccess()) {
            throw new IOException("Search and replace failed: " + result.getMessage());
        }

        return "Performed search and replace on file: " + path + 
               (result.getVersionId() != null ? " (version: " + result.getVersionId() + ")" : "");
    }

    /**
     * Handle patch file operations
     */
    private String handlePatchFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String patchContent = detail.diffPatch;
        File fileToUpdate = new File(projectDir, path);
        
        if (!fileToUpdate.exists()) {
            throw new IOException("File not found for patch: " + path);
        }

        if (patchContent == null || patchContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Patch content is empty");
        }

        // Use advanced file manager with patch update type
        AdvancedFileManager.FileOperationResult result = advancedFileManager.smartUpdateFile(
            fileToUpdate, patchContent, "patch", true, true, detail.contentType, "strict"
        );

        if (!result.isSuccess()) {
            throw new IOException("Patch application failed: " + result.getMessage());
        }

        return "Applied patch to file: " + path + 
               (result.getVersionId() != null ? " (version: " + result.getVersionId() + ")" : "");
    }

    private String handleCreateFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String content = detail.newContent != null ? detail.newContent : "";
        File newFile = new File(projectDir, path);
        
        // Create parent directories if they don't exist
        File parentDir = newFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Validate content if requested
        if (detail.validateContent) {
            AdvancedFileManager.ValidationResult validation = advancedFileManager.validateContent(content, detail.contentType);
            if (!validation.isValid()) {
                throw new IllegalArgumentException("Content validation failed: " + validation.getReason());
            }
        }

        advancedFileManager.writeFileContent(newFile, content);
        
        // Create version for new file
        String versionId = advancedFileManager.createVersion(newFile, "", content, "");
        
        return "Created file: " + path + " (version: " + versionId + ")";
    }

    private String handleDeleteFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        File fileToDelete = new File(projectDir, path);
        
        if (!fileToDelete.exists()) {
            throw new IOException("File not found for deletion: " + path);
        }

        // Create backup before deletion if requested
        if (detail.createBackup) {
            advancedFileManager.createBackup(fileToDelete);
        }

        if (fileToDelete.isDirectory()) {
            deleteDirectoryRecursive(fileToDelete);
        } else {
            fileToDelete.delete();
        }
        
        return "Deleted file/directory: " + path;
    }

    private String handleRenameFile(ChatMessage.FileActionDetail detail) throws IOException {
        String oldPath = detail.oldPath;
        String newPath = detail.newPath;
        File oldFile = new File(projectDir, oldPath);
        File newFile = new File(projectDir, newPath);
        
        if (!oldFile.exists()) {
            throw new IOException("Source file/directory not found for rename: " + oldPath);
        }
        
        if (newFile.exists()) {
            throw new IOException("Target file/directory already exists for rename: " + newPath);
        }

        // Create backup before rename if requested
        if (detail.createBackup) {
            advancedFileManager.createBackup(oldFile);
        }

        // Create parent directories for new file if needed
        File newParentDir = newFile.getParentFile();
        if (newParentDir != null && !newParentDir.exists()) {
            newParentDir.mkdirs();
        }

        boolean success = oldFile.renameTo(newFile);
        if (!success) {
            throw new IOException("Failed to rename file from " + oldPath + " to " + newPath);
        }
        
        return "Renamed " + oldPath + " to " + newPath;
    }

    private boolean deleteDirectoryRecursive(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectoryRecursive(child);
                }
            }
        }
        return dir.delete();
    }

    /**
     * Get the advanced file manager for external use
     */
    public AdvancedFileManager getAdvancedFileManager() {
        return advancedFileManager;
    }
}
