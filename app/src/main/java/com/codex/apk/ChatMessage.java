package com.codex.apk;

import android.util.Log; // Import Log for error logging
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken
import java.lang.reflect.Type; // Import Type

/**
 * Data class representing a single chat message.
 */
public class ChatMessage {
    public static final int SENDER_USER = 0;
    public static final int SENDER_AI = 1;

    // Status constants for AI messages with proposed actions
    public static final int STATUS_NONE = -1; // Default for user messages or AI thinking/error messages
    public static final int STATUS_PENDING_APPROVAL = 0; // AI proposed actions, waiting for user decision
    public static final int STATUS_ACCEPTED = 1; // User accepted the AI's proposed actions
    public static final int STATUS_DISCARDED = 2; // User discarded the AI's proposed actions

    // New status for indexing progress
    public static final int STATUS_INDEXING_PROGRESS = 3;

    private int sender; // SENDER_USER or SENDER_AI
    private String content; // Message text for user, explanation for AI
    private List<String> actionSummaries; // For AI messages, list of actions taken (brief)
    private List<String> suggestions; // For AI messages, list of suggestions
    private String aiModelName; // For AI messages, the name of the AI model used
    private long timestamp; // Timestamp for ordering messages

    // New fields for AI proposed actions and their status
    private String rawAiResponseJson; // The raw JSON response from the AI model
    private List<FileActionDetail> proposedFileChanges; // Parsed list of proposed file changes
    private int status; // Current status of the AI message (e.g., PENDING_APPROVAL, ACCEPTED, DISCARDED)

    // New fields for indexing progress
    private int indexingProgressCurrent;
    private int indexingProgressTotal;
    private String indexingCurrentFile;


    /**
     * Constructor for user messages.
     */
    public ChatMessage(int sender, String content, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.status = STATUS_NONE; // Default status for user messages
        this.actionSummaries = new ArrayList<>();
        this.suggestions = new ArrayList<>();
        this.proposedFileChanges = new ArrayList<>();
        this.indexingProgressCurrent = 0;
        this.indexingProgressTotal = 0;
        this.indexingCurrentFile = null;
    }

    /**
     * Comprehensive constructor for AI messages, including proposed actions and status.
     */
    public ChatMessage(int sender, String content, List<String> actionSummaries, List<String> suggestions,
                       String aiModelName, long timestamp, String rawAiResponseJson,
                       List<FileActionDetail> proposedFileChanges, int status) {
        this.sender = sender;
        this.content = content;
        this.actionSummaries = actionSummaries != null ? new ArrayList<>(actionSummaries) : new ArrayList<>();
        this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        this.aiModelName = aiModelName;
        this.timestamp = timestamp;
        this.rawAiResponseJson = rawAiResponseJson;
        this.proposedFileChanges = proposedFileChanges != null ? new ArrayList<>(proposedFileChanges) : new ArrayList<>();
        this.status = status;
        this.indexingProgressCurrent = 0;
        this.indexingProgressTotal = 0;
        this.indexingCurrentFile = null;
    }

    /**
     * Constructor for AI indexing progress messages.
     */
    public ChatMessage(int sender, String content, long timestamp, int indexingProgressCurrent,
                       int indexingProgressTotal, String indexingCurrentFile) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.status = STATUS_INDEXING_PROGRESS;
        this.indexingProgressCurrent = indexingProgressCurrent;
        this.indexingProgressTotal = indexingProgressTotal;
        this.indexingCurrentFile = indexingCurrentFile;
        this.actionSummaries = new ArrayList<>();
        this.suggestions = new ArrayList<>();
        this.proposedFileChanges = new ArrayList<>();
    }


    // Getters
    public int getSender() { return sender; }
    public String getContent() { return content; }
    public List<String> getActionSummaries() { return actionSummaries; }
    public List<String> getSuggestions() { return suggestions; }
    public String getAiModelName() { return aiModelName; }
    public long getTimestamp() { return timestamp; }
    public String getRawAiResponseJson() { return rawAiResponseJson; }
    public List<FileActionDetail> getProposedFileChanges() { return proposedFileChanges; }
    public int getStatus() { return status; }
    public int getIndexingProgressCurrent() { return indexingProgressCurrent; }
    public int getIndexingProgressTotal() { return indexingProgressTotal; }
    public String getIndexingCurrentFile() { return indexingCurrentFile; }


    // Setters (for updating message properties after creation, e.g., status)
    public void setContent(String content) { this.content = content; }
    public void setStatus(int status) { this.status = status; }
    public void setActionSummaries(List<String> actionSummaries) { this.actionSummaries = actionSummaries; }
    public void setProposedFileChanges(List<FileActionDetail> proposedFileChanges) { this.proposedFileChanges = proposedFileChanges; }
    public void setIndexingProgress(int current, int total, String currentFile) {
        this.indexingProgressCurrent = current;
        this.indexingProgressTotal = total;
        this.indexingCurrentFile = currentFile;
        this.status = STATUS_INDEXING_PROGRESS; // Ensure status is set correctly
    }


    /**
     * Data class to represent a single file action detail.
     * This is used to pass detailed action information from AI to UI/Processor.
     */
    public static class FileActionDetail {
        public String type; // e.g., "createFile", "modifyLines", "deleteFile"
        public String path; // Relative path for create, update, modify, delete
        public String oldPath; // For renameFile
        public String newPath; // For renameFile
        public String oldContent; // Full content before change (for diffing)
        public String newContent; // Full content after change (for diffing)
        public int startLine; // For modifyLines (1-indexed)
        public int deleteCount; // For modifyLines
        public List<String> insertLines; // For modifyLines

        // Comprehensive constructor
        public FileActionDetail(String type, String path, String oldPath, String newPath,
                                String oldContent, String newContent, int startLine,
                                int deleteCount, List<String> insertLines) {
            this.type = type;
            this.path = path;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.startLine = startLine;
            this.deleteCount = deleteCount;
            this.insertLines = insertLines != null ? new ArrayList<>(insertLines) : null;
        }

        // Method to get a displayable summary of the action
        public String getSummary() {
            switch (type) {
                case "createFile":
                    return "Create file: " + path;
                case "updateFile":
                    return "Update file: " + path;
                case "deleteFile":
                    return "Delete file: " + path;
                case "renameFile":
                    return "Rename file: " + oldPath + " to " + newPath;
                case "modifyLines":
                    String linesModified = "";
                    if (deleteCount > 0 && (insertLines == null || insertLines.isEmpty())) {
                        linesModified = "deleted " + deleteCount + " lines";
                    } else if (deleteCount == 0 && (insertLines != null && !insertLines.isEmpty())) {
                        linesModified = "inserted " + insertLines.size() + " lines";
                    } else if (deleteCount > 0 && (insertLines != null && !insertLines.isEmpty())) {
                        linesModified = "modified " + deleteCount + " lines (replaced with " + insertLines.size() + " new lines)";
                    } else {
                        linesModified = "made changes";
                    }
                    return "Modify " + path + " (line " + startLine + ", " + linesModified + ")";
                default:
                    return "Unknown action: " + type + " on " + path;
            }
        }
    }

    /**
     * Converts this ChatMessage object into a Map for easy storage in SharedPreferences.
     * @return A Map representation of the ChatMessage.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sender", sender);
        map.put("content", content);
        map.put("timestamp", timestamp);
        map.put("status", status); // Include status

        // Only include AI-specific fields if it's an AI message
        if (sender == SENDER_AI) {
            map.put("actionSummaries", actionSummaries);
            map.put("suggestions", suggestions);
            map.put("aiModelName", aiModelName);
            map.put("rawAiResponseJson", rawAiResponseJson);

            // Serialize proposedFileChanges to JSON string
            if (proposedFileChanges != null && !proposedFileChanges.isEmpty()) {
                Gson gson = new Gson();
                map.put("proposedFileChanges", gson.toJson(proposedFileChanges));
            } else {
                map.put("proposedFileChanges", null);
            }

            // Include indexing progress fields if applicable
            if (status == STATUS_INDEXING_PROGRESS) {
                map.put("indexingProgressCurrent", indexingProgressCurrent);
                map.put("indexingProgressTotal", indexingProgressTotal);
                map.put("indexingCurrentFile", indexingCurrentFile);
            }
        }
        return map;
    }

    /**
     * Creates a ChatMessage object from a Map, typically loaded from SharedPreferences.
     * @param map The Map representation of the ChatMessage.
     * @return A ChatMessage object.
     */
    public static ChatMessage fromMap(Map<String, Object> map) {
        int sender = ((Number) map.get("sender")).intValue();
        String content = (String) map.get("content");
        long timestamp = ((Number) map.get("timestamp")).longValue();

        if (sender == SENDER_AI) {
            List<String> actionSummaries = new ArrayList<>();
            Object actionSummariesObj = map.get("actionSummaries");
            if (actionSummariesObj instanceof List) {
                for (Object item : (List<?>) actionSummariesObj) {
                    if (item instanceof String) {
                        actionSummaries.add((String) item);
                    } else {
                        Log.w("ChatMessage", "Non-string item found in actionSummaries: " + item.getClass().getName());
                        actionSummaries.add(String.valueOf(item));
                    }
                }
            } else if (actionSummariesObj != null) {
                Log.w("ChatMessage", "actionSummaries is not a List: " + actionSummariesObj.getClass().getName());
            }


            List<String> suggestions = new ArrayList<>();
            Object suggestionsObj = map.get("suggestions");
            if (suggestionsObj instanceof List) {
                for (Object item : (List<?>) suggestionsObj) {
                    if (item instanceof String) {
                        suggestions.add((String) item);
                    } else {
                        Log.w("ChatMessage", "Non-string item found in suggestions: " + item.getClass().getName());
                        suggestions.add(String.valueOf(item));
                    }
                }
            } else if (suggestionsObj != null) {
                Log.w("ChatMessage", "suggestions is not a List: " + suggestionsObj.getClass().getName());
            }

            String aiModelName = (String) map.get("aiModelName");
            String rawAiResponseJson = (String) map.get("rawAiResponseJson");
            int status = map.containsKey("status") ? ((Number) map.get("status")).intValue() : STATUS_NONE;


            // Deserialize proposedFileChanges from JSON string
            List<FileActionDetail> proposedFileChanges = null;
            String proposedFileChangesJson = (String) map.get("proposedFileChanges");
            if (proposedFileChangesJson != null && !proposedFileChangesJson.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<FileActionDetail>>() {}.getType();
                    proposedFileChanges = gson.fromJson(proposedFileChangesJson, type);
                } catch (Exception e) {
                    Log.e("ChatMessage", "Error deserializing proposedFileChanges: " + e.getMessage(), e);
                }
            }

            // Handle indexing progress fields
            int indexingProgressCurrent = map.containsKey("indexingProgressCurrent") ? ((Number) map.get("indexingProgressCurrent")).intValue() : 0;
            int indexingProgressTotal = map.containsKey("indexingProgressTotal") ? ((Number) map.get("indexingProgressTotal")).intValue() : 0;
            String indexingCurrentFile = (String) map.get("indexingCurrentFile");


            // If it's an indexing progress message, use its specific constructor
            if (status == STATUS_INDEXING_PROGRESS) {
                return new ChatMessage(sender, content, timestamp,
                        indexingProgressCurrent, indexingProgressTotal, indexingCurrentFile);
            } else {
                return new ChatMessage(sender, content, actionSummaries, suggestions, aiModelName,
                        timestamp, rawAiResponseJson, proposedFileChanges, status);
            }
        } else {
            return new ChatMessage(sender, content, timestamp);
        }
    }
}
