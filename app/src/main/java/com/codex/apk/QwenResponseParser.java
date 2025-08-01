package com.codex.apk;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for handling JSON responses from Qwen models, especially for file operations.
 */
public class QwenResponseParser {
    private static final String TAG = "QwenResponseParser";

    /**
     * Represents a parsed file operation from JSON response
     */
    public static class FileOperation {
        public final String type;
        public final String path;
        public final String content;
        public final String oldPath;
        public final String newPath;

        public FileOperation(String type, String path, String content, String oldPath, String newPath) {
            this.type = type;
            this.path = path;
            this.content = content;
            this.oldPath = oldPath;
            this.newPath = newPath;
        }
    }

    /**
     * Represents a complete parsed JSON response
     */
    public static class ParsedResponse {
        public final String action;
        public final List<FileOperation> operations;
        public final String explanation;
        public final List<String> suggestions;
        public final boolean isValid;

        public ParsedResponse(String action, List<FileOperation> operations, 
                            String explanation, List<String> suggestions, boolean isValid) {
            this.action = action;
            this.operations = operations;
            this.explanation = explanation;
            this.suggestions = suggestions;
            this.isValid = isValid;
        }
    }

    /**
     * Attempts to parse a JSON response string into a structured response object.
     * Returns null if the response is not valid JSON or doesn't match expected format.
     */
    public static ParsedResponse parseResponse(String responseText) {
        try {
            Log.d(TAG, "Parsing response: " + responseText.substring(0, Math.min(200, responseText.length())) + "...");
            JsonObject jsonObj = JsonParser.parseString(responseText).getAsJsonObject();

            // Multi-operation: { action: "file_operation", operations: [...] }
            if (jsonObj.has("action") && "file_operation".equals(jsonObj.get("action").getAsString())) {
                Log.d(TAG, "Detected multi-operation file_operation response");
                return parseFileOperationResponse(jsonObj);
            }

            // Single-operation: { action: "createFile", ... } or updateFile/deleteFile/etc
            if (jsonObj.has("action") && isSingleFileAction(jsonObj.get("action").getAsString())) {
                Log.d(TAG, "Detected single-operation response with action: " + jsonObj.get("action").getAsString());
                List<FileOperation> operations = new ArrayList<>();
                String type = jsonObj.get("action").getAsString();
                String path = jsonObj.has("path") ? jsonObj.get("path").getAsString() : "";
                String content = jsonObj.has("content") ? jsonObj.get("content").getAsString() : "";
                String oldPath = jsonObj.has("oldPath") ? jsonObj.get("oldPath").getAsString() : "";
                String newPath = jsonObj.has("newPath") ? jsonObj.get("newPath").getAsString() : "";
                operations.add(new FileOperation(type, path, content, oldPath, newPath));
                String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
                List<String> suggestions = new ArrayList<>();
                if (jsonObj.has("suggestions")) {
                    JsonArray suggestionsArray = jsonObj.getAsJsonArray("suggestions");
                    for (int i = 0; i < suggestionsArray.size(); i++) {
                        suggestions.add(suggestionsArray.get(i).getAsString());
                    }
                }
                return new ParsedResponse(type, operations, explanation, suggestions, true);
            }

            Log.d(TAG, "Not a file operation response, treating as regular JSON");
            // Fallback: regular JSON
            return parseRegularJsonResponse(jsonObj);
        } catch (JsonParseException e) {
            Log.w(TAG, "Failed to parse JSON response: " + responseText);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            return null;
        }
    }

    /**
     * Parses a file operation response
     */
    private static ParsedResponse parseFileOperationResponse(JsonObject jsonObj) {
        List<FileOperation> operations = new ArrayList<>();
        
        if (jsonObj.has("operations")) {
            JsonArray operationsArray = jsonObj.getAsJsonArray("operations");
            for (int i = 0; i < operationsArray.size(); i++) {
                JsonObject operation = operationsArray.get(i).getAsJsonObject();
                
                String type = operation.get("type").getAsString();
                String path = operation.has("path") ? operation.get("path").getAsString() : "";
                String content = operation.has("content") ? operation.get("content").getAsString() : "";
                String oldPath = operation.has("oldPath") ? operation.get("oldPath").getAsString() : "";
                String newPath = operation.has("newPath") ? operation.get("newPath").getAsString() : "";
                
                operations.add(new FileOperation(type, path, content, oldPath, newPath));
            }
        }
        
        String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
        List<String> suggestions = new ArrayList<>();
        
        if (jsonObj.has("suggestions")) {
            JsonArray suggestionsArray = jsonObj.getAsJsonArray("suggestions");
            for (int i = 0; i < suggestionsArray.size(); i++) {
                suggestions.add(suggestionsArray.get(i).getAsString());
            }
        }
        
        return new ParsedResponse("file_operation", operations, explanation, suggestions, true);
    }

    /**
     * Parses a regular JSON response (non-file operation)
     */
    private static ParsedResponse parseRegularJsonResponse(JsonObject jsonObj) {
        // For regular JSON responses, we don't have specific operations
        return new ParsedResponse("json_response", new ArrayList<>(), 
                                jsonObj.toString(), new ArrayList<>(), true);
    }

    private static boolean isSingleFileAction(String action) {
        return "createFile".equals(action) || "updateFile".equals(action) || "deleteFile".equals(action)
                || "renameFile".equals(action) || "readFile".equals(action) || "listFiles".equals(action);
    }

    /**
     * Validates if a response string looks like it might be JSON
     */
    public static boolean looksLikeJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.d(TAG, "looksLikeJson: response is null or empty");
            return false;
        }
        
        String trimmed = response.trim();
        boolean isJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                        (trimmed.startsWith("[") && trimmed.endsWith("]"));
        
        Log.d(TAG, "looksLikeJson: checking '" + trimmed.substring(0, Math.min(50, trimmed.length())) + "...' -> " + isJson);
        return isJson;
    }

    /**
     * Converts a ParsedResponse back to a ChatMessage.FileActionDetail list
     */
    public static List<ChatMessage.FileActionDetail> toFileActionDetails(ParsedResponse response) {
        List<ChatMessage.FileActionDetail> details = new ArrayList<>();
        
        for (FileOperation op : response.operations) {
            ChatMessage.FileActionDetail detail = new ChatMessage.FileActionDetail(
                op.type, op.path, op.oldPath, op.newPath, "", op.content, 0, 0, null
            );
            details.add(detail);
        }
        
        return details;
    }
}