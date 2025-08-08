package com.codex.apk;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;

public class AiProcessor {
    private static final String TAG = "AiProcessor";
    private static final Gson gson = new Gson();
    private final File projectDir;
    private final Context context;
    private final FileManager fileManager;

    public AiProcessor(File projectDir, Context context) {
        this.projectDir = projectDir;
        this.context = context;
        this.fileManager = new FileManager(context, projectDir);
    }

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
                summary = handleUpdateFile(detail);
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
            // patchFile case removed as per user request to remove advanced file manager features
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
        return summary;
    }

    private String handleUpdateFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String content = detail.newContent;
        File fileToUpdate = new File(projectDir, path);

        if (!fileToUpdate.exists()) {
            throw new IOException("File not found for update: " + path);
        }

        fileManager.writeFileContent(fileToUpdate, content);

        return "Updated file: " + path;
    }

    private String handleSearchAndReplace(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String search = detail.search;
        String replace = detail.replace;
        String searchPattern = detail.searchPattern;
        File fileToUpdate = new File(projectDir, path);
        
        if (!fileToUpdate.exists()) {
            throw new IOException("File not found for search and replace: " + path);
        }

        String content = fileManager.readFileContent(fileToUpdate);
        String newContent;

        if (searchPattern != null && !searchPattern.isEmpty()) {
            try {
                newContent = content.replaceAll(searchPattern, replace);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid regex pattern: " + searchPattern);
            }
        } else {
            newContent = content.replace(search, replace);
        }

        fileManager.writeFileContent(fileToUpdate, newContent);

        return "Performed search and replace on file: " + path;
    }

    private String handleCreateFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String content = detail.newContent != null ? detail.newContent : "";
        File newFile = new File(projectDir, path);
        
        File parentDir = newFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (newFile.exists()) {
            throw new IOException("File already exists");
        }

        if (!newFile.createNewFile()) {
            throw new IOException("Failed to create file");
        }
        
        fileManager.writeFileContent(newFile, content);
        
        return "Created file: " + path;
    }

    private String handleDeleteFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        File fileToDelete = new File(projectDir, path);
        
        if (!fileToDelete.exists()) {
            Log.w(TAG, "deleteFileByPath: File or directory does not exist: " + fileToDelete.getAbsolutePath());
            return "File or directory does not exist: " + path;
        }

        fileManager.deleteFileOrDirectory(fileToDelete);
        
        return "Deleted file/directory: " + path;
    }

    private String handleRenameFile(ChatMessage.FileActionDetail detail) throws IOException {
        String oldPath = detail.oldPath;
        String newPath = detail.newPath;
        File oldFile = new File(projectDir, oldPath);
        File newFile = new File(projectDir, newPath);
        
        fileManager.renameFileOrDir(oldFile, newFile);
        
        return "Renamed " + oldPath + " to " + newPath;
    }
}
