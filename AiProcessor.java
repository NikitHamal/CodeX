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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            case "modifyLines":
                summary = handleModifyLines(detail);
                break;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
        return summary;
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

    private String handleModifyLines(ChatMessage.FileActionDetail detail) throws IOException {
        String path = detail.path;
        int startLine = detail.startLine; // 1-indexed
        int deleteCount = detail.deleteCount;
        List<String> insertLines = detail.insertLines;

        File fileToModify = new File(projectDir, path);
        if (!fileToModify.exists()) {
            throw new IOException("File not found for modification: " + path);
        }

        String fileContent = fileManager.readFileContent(fileToModify);

        List<String> lines = new ArrayList<>();
        Pattern lineBreakPattern = Pattern.compile("\\r?\\n");
        Matcher matcher = lineBreakPattern.matcher(fileContent);
        int lastEnd = 0;
        while (matcher.find()) {
            lines.add(fileContent.substring(lastEnd, matcher.start()));
            lastEnd = matcher.end();
        }
        lines.add(fileContent.substring(lastEnd)); // Add the last line

        // Adjust for 0-indexed list
        int actualStartLineIndex = startLine - 1;

        if (actualStartLineIndex < 0 || actualStartLineIndex > lines.size()) {
            throw new IOException("Start line " + startLine + " out of bounds for file " + path + " with " + lines.size() + " lines.");
        }

        // Remove lines
        for (int i = 0; i < deleteCount; i++) {
            if (actualStartLineIndex < lines.size()) {
                lines.remove(actualStartLineIndex);
            } else {
                Log.w(TAG, "Attempted to delete line beyond file end. Path: " + path + ", StartLine: " + startLine + ", DeleteCount: " + deleteCount);
                break;
            }
        }

        // Insert lines
        for (int i = 0; i < insertLines.size(); i++) {
            lines.add(actualStartLineIndex + i, insertLines.get(i));
        }

        String newContent = String.join("\n", lines);
        fileManager.writeFileContent(fileToModify, newContent);
        return "Modified lines in " + path + " (start: " + startLine + ", deleted: " + deleteCount + ", inserted: " + insertLines.size() + ")";
    }
}
