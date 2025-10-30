package com.codex.apk;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * Parser for handling JSON responses from Qwen models, especially for file operations.
 */
public class QwenResponseParser {
    private static final String TAG = "QwenResponseParser";
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ParseResultListener {
        void onParseSuccess(ParsedResponse response);
        void onParseFailed();
    }

    /**
     * Represents a parsed plan step
     */
    public static class PlanStep {
        public final String id;
        public final String title;
        public final String kind; // file | search | analysis | preview | validate | other

        public PlanStep(String id, String title, String kind) {
            this.id = id;
            this.title = title;
            this.kind = kind;
        }
    }

    /**
     * Represents a parsed file operation from JSON response
     */
    public static class FileOperation {
        public final String type;
        public final String path;
        public final String content;
        public final String oldPath;
        public final String newPath;
        public final String search;
        public final String replace;
        // Line-edit fields
        public final Integer startLine;
        public final Integer deleteCount;
        public final List<String> insertLines;
        // Advanced fields
        public final String updateType;
        public final String searchPattern;
        public final String replaceWith;
        public final String diffPatch;
        public final Boolean createBackup;
        public final Boolean validateContent;
        public final String contentType;
        public final String errorHandling;
        public final Boolean generateDiff;
        public final String diffFormat;

        public FileOperation(String type, String path, String content, String oldPath, String newPath, String search, String replace,
                             Integer startLine, Integer deleteCount, List<String> insertLines,
                             String updateType, String searchPattern, String replaceWith, String diffPatch,
                             Boolean createBackup, Boolean validateContent, String contentType,
                             String errorHandling, Boolean generateDiff, String diffFormat) {
            this.type = type;
            this.path = path;
            this.content = content;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.search = search;
            this.replace = replace;
            this.startLine = startLine;
            this.deleteCount = deleteCount;
            this.insertLines = insertLines;
            this.updateType = updateType;
            this.searchPattern = searchPattern;
            this.replaceWith = replaceWith;
            this.diffPatch = diffPatch;
            this.createBackup = createBackup;
            this.validateContent = validateContent;
            this.contentType = contentType;
            this.errorHandling = errorHandling;
            this.generateDiff = generateDiff;
            this.diffFormat = diffFormat;
        }
    }

    /**
     * Represents a complete parsed JSON response
     */
    public static class ParsedResponse {
        public String action; // plan | file_operation | json_response | single file op
        public List<FileOperation> operations;
        public List<PlanStep> planSteps;
        public String explanation;
        public boolean isValid;
        public String rawResponse;

        public ParsedResponse(String action, List<FileOperation> operations, List<PlanStep> planSteps,
                              String explanation, boolean isValid) {
            this.action = action;
            this.operations = operations;
            this.planSteps = planSteps;
            this.explanation = explanation;
            this.isValid = isValid;
        }

        public ParsedResponse() {
            this.operations = new ArrayList<>();
            this.planSteps = new ArrayList<>();
        }
    }

    /**
     * Attempts to parse a JSON response string into a structured response object.
     * Returns null if the response is not valid JSON or doesn't match expected format.
     */
    public static void parseResponseAsync(String jsonToParse, String rawSse, ParseResultListener listener) {
        backgroundExecutor.execute(() -> {
            try {
                ParsedResponse response = parseResponse(jsonToParse);
                if (response != null) {
                    response.rawResponse = rawSse;
                    mainHandler.post(() -> listener.onParseSuccess(response));
                } else {
                    mainHandler.post(listener::onParseFailed);
                }
            } catch (Exception e) {
                mainHandler.post(listener::onParseFailed);
            }
        });
    }

    public static ParsedResponse parseResponse(String responseText) {
        try {
            Log.d(TAG, "Parsing response: " + responseText.substring(0, Math.min(200, responseText.length())) + "...");
            JsonObject jsonObj = JsonParser.parseString(responseText).getAsJsonObject();

            // Plan response (more flexible: any JSON with a "steps" array is considered a plan)
            if (jsonObj.has("steps") && jsonObj.get("steps").isJsonArray()) {
                List<PlanStep> steps = new ArrayList<>();
                JsonArray arr = jsonObj.getAsJsonArray("steps");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject s = arr.get(i).getAsJsonObject();
                    String id = s.has("id") ? s.get("id").getAsString() : ("s" + (i + 1));
                    String title = s.has("title") ? s.get("title").getAsString() : ("Step " + (i + 1));
                    String kind = s.has("kind") ? s.get("kind").getAsString() : "file";
                    steps.add(new PlanStep(id, title, kind));
                }
                String explanation = jsonObj.has("goal") ? ("Plan for: " + jsonObj.get("goal").getAsString()) : "Plan";
                return new ParsedResponse("plan", new ArrayList<>(), steps, explanation, true);
            }

            // Check for multi-operation format first
            if (jsonObj.has("operations") && jsonObj.get("operations").isJsonArray()) {
                Log.d(TAG, "Detected 'operations' array, parsing as multi-operation response");
                return parseFileOperationResponse(jsonObj);
            }

            // Fallback to single-operation if 'operations' is not present
            if (jsonObj.has("action") && isSingleFileAction(jsonObj.get("action").getAsString())) {
                Log.d(TAG, "Detected single-operation response with action: " + jsonObj.get("action").getAsString());
                List<FileOperation> operations = new ArrayList<>();
                String type = jsonObj.get("action").getAsString();
                String path = jsonObj.has("path") ? jsonObj.get("path").getAsString() : "";
                String content = jsonObj.has("content") ? jsonObj.get("content").getAsString() : "";
                String oldPath = jsonObj.has("oldPath") ? jsonObj.get("oldPath").getAsString() : "";
                String newPath = jsonObj.has("newPath") ? jsonObj.get("newPath").getAsString() : "";
                String search = jsonObj.has("search") ? jsonObj.get("search").getAsString() : "";
                String replace = jsonObj.has("replace") ? jsonObj.get("replace").getAsString() : "";

                // Advanced fields
                String updateType = jsonObj.has("updateType") ? jsonObj.get("updateType").getAsString() : null;
                String searchPattern = jsonObj.has("searchPattern") ? jsonObj.get("searchPattern").getAsString() : null;
                String replaceWith = jsonObj.has("replaceWith") ? jsonObj.get("replaceWith").getAsString() : null;
                String diffPatch = jsonObj.has("diffPatch") ? jsonObj.get("diffPatch").getAsString() : null;
                Boolean createBackup = jsonObj.has("createBackup") ? jsonObj.get("createBackup").getAsBoolean() : null;
                Boolean validateContent = jsonObj.has("validateContent") ? jsonObj.get("validateContent").getAsBoolean() : null;
                String contentType = jsonObj.has("contentType") ? jsonObj.get("contentType").getAsString() : null;
                String errorHandling = jsonObj.has("errorHandling") ? jsonObj.get("errorHandling").getAsString() : null;
                Boolean generateDiff = jsonObj.has("generateDiff") ? jsonObj.get("generateDiff").getAsBoolean() : null;
                String diffFormat = jsonObj.has("diffFormat") ? jsonObj.get("diffFormat").getAsString() : null;

                Integer startLine = jsonObj.has("startLine") ? jsonObj.get("startLine").getAsInt() : null;
                Integer deleteCount = jsonObj.has("deleteCount") ? jsonObj.get("deleteCount").getAsInt() : null;
                List<String> insertLines = null;
                if (jsonObj.has("insertLines") && jsonObj.get("insertLines").isJsonArray()) {
                    insertLines = new ArrayList<>();
                    JsonArray arr = jsonObj.getAsJsonArray("insertLines");
                    for (int i = 0; i < arr.size(); i++) insertLines.add(arr.get(i).getAsString());
                }

                operations.add(new FileOperation(type, path, content, oldPath, newPath, search, replace,
                        startLine, deleteCount, insertLines,
                        updateType, searchPattern, replaceWith, diffPatch, createBackup, validateContent, contentType,
                        errorHandling, generateDiff, diffFormat));

                String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
                return new ParsedResponse(type, operations, new ArrayList<>(), explanation, true);
            }

            // Fallback: check if the root object itself is a single file operation
            if (jsonObj.has("type") && isSingleFileAction(jsonObj.get("type").getAsString())) {
                Log.d(TAG, "Detected root object as single file operation with type: " + jsonObj.get("type").getAsString());
                List<FileOperation> operations = new ArrayList<>();
                String type = jsonObj.get("type").getAsString();
                String path = jsonObj.has("path") ? jsonObj.get("path").getAsString() : "";
                String content = jsonObj.has("content") ? jsonObj.get("content").getAsString() : "";
                String oldPath = jsonObj.has("oldPath") ? jsonObj.get("oldPath").getAsString() : "";
                String newPath = jsonObj.has("newPath") ? jsonObj.get("newPath").getAsString() : "";
                String search = jsonObj.has("search") ? jsonObj.get("search").getAsString() : "";
                String replace = jsonObj.has("replace") ? jsonObj.get("replace").getAsString() : "";

                // Advanced fields
                String updateType = jsonObj.has("updateType") ? jsonObj.get("updateType").getAsString() : null;
                String searchPattern = jsonObj.has("searchPattern") ? jsonObj.get("searchPattern").getAsString() : null;
                String replaceWith = jsonObj.has("replaceWith") ? jsonObj.get("replaceWith").getAsString() : null;
                String diffPatch = jsonObj.has("diffPatch") ? jsonObj.get("diffPatch").getAsString() : null;
                Boolean createBackup = jsonObj.has("createBackup") ? jsonObj.get("createBackup").getAsBoolean() : null;
                Boolean validateContent = jsonObj.has("validateContent") ? jsonObj.get("validateContent").getAsBoolean() : null;
                String contentType = jsonObj.has("contentType") ? jsonObj.get("contentType").getAsString() : null;
                String errorHandling = jsonObj.has("errorHandling") ? jsonObj.get("errorHandling").getAsString() : null;
                Boolean generateDiff = jsonObj.has("generateDiff") ? jsonObj.get("generateDiff").getAsBoolean() : null;
                String diffFormat = jsonObj.has("diffFormat") ? jsonObj.get("diffFormat").getAsString() : null;

                Integer startLine = jsonObj.has("startLine") ? jsonObj.get("startLine").getAsInt() : null;
                Integer deleteCount = jsonObj.has("deleteCount") ? jsonObj.get("deleteCount").getAsInt() : null;
                List<String> insertLines = null;
                if (jsonObj.has("insertLines") && jsonObj.get("insertLines").isJsonArray()) {
                    insertLines = new ArrayList<>();
                    JsonArray arr = jsonObj.getAsJsonArray("insertLines");
                    for (int i = 0; i < arr.size(); i++) insertLines.add(arr.get(i).getAsString());
                }

                operations.add(new FileOperation(type, path, content, oldPath, newPath, search, replace,
                        startLine, deleteCount, insertLines,
                        updateType, searchPattern, replaceWith, diffPatch, createBackup, validateContent, contentType,
                        errorHandling, generateDiff, diffFormat));

                String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
                return new ParsedResponse(type, operations, new ArrayList<>(), explanation, true);
            }

            Log.d(TAG, "Not a recognized file operation response, treating as regular JSON");
            // Fallback for non-file operation JSON
            return parseRegularJsonResponse(jsonObj);
        } catch (JsonParseException e) {
            Log.w(TAG, "Failed to parse JSON response: " + responseText, e);
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

                // If modifyLines is present with search/replace pairs, expand into multiple searchAndReplace operations
                if (operation.has("modifyLines") && operation.get("modifyLines").isJsonArray()) {
                    JsonArray hunks = operation.getAsJsonArray("modifyLines");
                    for (int j = 0; j < hunks.size(); j++) {
                        try {
                            JsonObject h = hunks.get(j).getAsJsonObject();
                            String s = h.has("search") ? h.get("search").getAsString() : null;
                            String r = h.has("replace") ? h.get("replace").getAsString() : null;
                            if (s == null || r == null) continue;
                            operations.add(new FileOperation(
                                "searchAndReplace", path, "", "", "", s, r,
                                (Integer) null, (Integer) null, (java.util.List<String>) null,
                                (String) null, (String) null, (String) null, (String) null,
                                (Boolean) null, (Boolean) null, (String) null, (String) null,
                                (Boolean) null, (String) null
                            ));
                        } catch (Exception ignored) {}
                    }
                    continue;
                }

                String content = operation.has("content") ? operation.get("content").getAsString() :
                               (operation.has("newContent") ? operation.get("newContent").getAsString() : "");
                String oldPath = operation.has("oldPath") ? operation.get("oldPath").getAsString() : "";
                String newPath = operation.has("newPath") ? operation.get("newPath").getAsString() : "";
                String search = operation.has("search") ? operation.get("search").getAsString() : "";
                String replace = operation.has("replace") ? operation.get("replace").getAsString() : "";

                // Advanced fields
                String updateType = operation.has("updateType") ? operation.get("updateType").getAsString() : null;
                String searchPattern = operation.has("searchPattern") ? operation.get("searchPattern").getAsString() : null;
                String replaceWith = operation.has("replaceWith") ? operation.get("replaceWith").getAsString() : null;
                String diffPatch = operation.has("diffPatch") ? operation.get("diffPatch").getAsString() : null;
                Boolean createBackup = operation.has("createBackup") ? operation.get("createBackup").getAsBoolean() : null;
                Boolean validateContent = operation.has("validateContent") ? operation.get("validateContent").getAsBoolean() : null;
                String contentType = operation.has("contentType") ? operation.get("contentType").getAsString() : null;
                String errorHandling = operation.has("errorHandling") ? operation.get("errorHandling").getAsString() : null;
                Boolean generateDiff = operation.has("generateDiff") ? operation.get("generateDiff").getAsBoolean() : null;
                String diffFormat = operation.has("diffFormat") ? operation.get("diffFormat").getAsString() : null;
                Integer startLine = operation.has("startLine") ? operation.get("startLine").getAsInt() : null;
                Integer deleteCount = operation.has("deleteCount") ? operation.get("deleteCount").getAsInt() : null;
                List<String> insertLines = null;
                if (operation.has("insertLines") && operation.get("insertLines").isJsonArray()) {
                    insertLines = new ArrayList<>();
                    JsonArray arr = operation.getAsJsonArray("insertLines");
                    for (int j = 0; j < arr.size(); j++) insertLines.add(arr.get(j).getAsString());
                }

                operations.add(new FileOperation(type, path, content, oldPath, newPath, search, replace,
                        startLine, deleteCount, insertLines,
                        updateType, searchPattern, replaceWith, diffPatch, createBackup, validateContent, contentType,
                        errorHandling, generateDiff, diffFormat));
            }
        }
        
        String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
        
        return new ParsedResponse("file_operation", operations, new ArrayList<>(), explanation, true);
    }

    /**
     * Parses a regular JSON response (non-file operation)
     */
    private static ParsedResponse parseRegularJsonResponse(JsonObject jsonObj) {
        // For regular JSON responses, we don't have specific operations
        return new ParsedResponse("json_response", new ArrayList<>(), new ArrayList<>(), 
                                jsonObj.toString(), true);
    }

    private static boolean isSingleFileAction(String action) {
        return "createFile".equals(action) || "updateFile".equals(action) || "deleteFile".equals(action)
                || "renameFile".equals(action) || "readFile".equals(action) || "listFiles".equals(action)
                || "searchAndReplace".equals(action) || "patchFile".equals(action) || "smartUpdate".equals(action);
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
        
        Log.d(TAG, "looksLikeJson: checking '" + trimmed.substring(0, Math.min(50, trimmed.length())) + "...'");
        return isJson;
    }

    /**
     * Converts a ParsedResponse back to a ChatMessage.FileActionDetail list
     */
    public static List<ChatMessage.FileActionDetail> toFileActionDetails(ParsedResponse response) {
        List<ChatMessage.FileActionDetail> details = new ArrayList<>();
        
        for (FileOperation op : response.operations) {
            ChatMessage.FileActionDetail detail = new ChatMessage.FileActionDetail(
                op.type, op.path, op.oldPath, op.newPath,
                "", op.content,
                op.startLine != null ? op.startLine : 0,
                op.deleteCount != null ? op.deleteCount : 0,
                op.insertLines,
                op.search, op.replace
            );
            // Map advanced fields
            if (op.updateType != null) detail.updateType = op.updateType;
            if (op.searchPattern != null) detail.searchPattern = op.searchPattern;
            if (op.replaceWith != null) detail.replaceWith = op.replaceWith;
            if (op.diffPatch != null) detail.diffPatch = op.diffPatch;
            if (op.createBackup != null) detail.createBackup = op.createBackup;
            if (op.validateContent != null) detail.validateContent = op.validateContent;
            if (op.contentType != null) detail.contentType = op.contentType;
            if (op.errorHandling != null) detail.errorHandling = op.errorHandling;
            if (op.generateDiff != null) detail.generateDiff = op.generateDiff;
            if (op.diffFormat != null) detail.diffFormat = op.diffFormat;
            details.add(detail);
        }
        
        return details;
    }

    /** Convert ParsedResponse plan steps to ChatMessage.PlanStep list */
    public static List<ChatMessage.PlanStep> toPlanSteps(ParsedResponse response) {
        List<ChatMessage.PlanStep> out = new ArrayList<>();
        if (response.planSteps == null) return out;
        for (PlanStep s : response.planSteps) {
            out.add(new ChatMessage.PlanStep(s.id, s.title, s.kind));
        }
        return out;
    }
}