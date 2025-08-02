package com.codex.apk;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiProcessor {
    private static final String TAG = "AiProcessor";
    private final File projectDir;
    private final Context context;
    private final FileManager fileManager;

    public AiProcessor(File projectDir, Context context) {
        this.projectDir = projectDir;
        this.context = context;
        this.fileManager = new FileManager(context, projectDir);
    }

    /**
     * Applies a single file action as described by a FileActionDetail object.
     * This method performs the actual file system modification.
     *
     * @param detail The FileActionDetail containing the action to apply.
     * @return A summary string of the applied action.
     * @throws IOException If a file operation fails.
     * @throws IllegalArgumentException If the action type is unknown or parameters are invalid.
     */
    public String applyFileAction(ChatMessage.FileActionDetail detail) throws IOException, IllegalArgumentException {
        String actionType = detail.type;
        String summary = "";

        switch (actionType) {
            case "createFile":
                summary = handleCreateFile(detail);
                break;
            case "updateFile":
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
            // Removed 'modifyLines' action
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
        return summary;
    }

    private String handleSearchAndReplace(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String search = detail.search;
        String replace = detail.replace;
        File fileToUpdate = new File(projectDir, path);
        if (!fileToUpdate.exists()) {
            throw new IOException("File not found for search and replace: " + path);
        }
        String content = fileManager.readFileContent(fileToUpdate);
        String newContent = content.replace(search, replace);
        fileManager.writeFileContent(fileToUpdate, newContent);
        return "Performed search and replace on file: " + path;
    }

    private String handleCreateFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        String content = detail.newContent != null ? detail.newContent : "";
        File newFile = new File(projectDir, path);
        fileManager.createNewFile(newFile.getParentFile(), newFile.getName());
        fileManager.writeFileContent(newFile, content);
        return "Created file: " + path;
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

    private String handleDeleteFile(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        File fileToDelete = new File(projectDir, path);
        if (!fileToDelete.exists()) {
            throw new IOException("File not found for deletion: " + path);
        }
        fileManager.deleteFileOrDirectory(fileToDelete);
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
        fileManager.renameFileOrDir(oldFile, newFile);
        return "Renamed " + oldPath + " to " + newPath;
    }

}
