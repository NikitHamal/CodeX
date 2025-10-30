package com.codex.apk;

import android.util.Log; // Import Log for error logging
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken
import java.lang.reflect.Type; // Import Type
import com.codex.apk.ai.WebSource;

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

    private int sender; // SENDER_USER or SENDER_AI
    private String content; // Message text for user, explanation for AI
    private List<String> actionSummaries; // For AI messages, list of actions taken (brief)
    private List<String> suggestions; // For AI messages, list of suggestions
    private String aiModelName; // For AI messages, the name of the AI model used
    private long timestamp; // Timestamp for ordering messages
    private List<String> userAttachmentPaths; // For user messages, list of attached file display names/paths

    // New fields for AI proposed actions and their status
    private String rawAiResponseJson; // The raw JSON response from the AI model
    private List<FileActionDetail> proposedFileChanges; // Parsed list of proposed file changes
    private int status; // Current status of the AI message (e.g., PENDING_APPROVAL, ACCEPTED, DISCARDED)
    
    // New fields for thinking mode and web search
    private String thinkingContent; // The thinking/reasoning content from AI
    private List<WebSource> webSources; // Web sources used in the response

    // Plan steps (for agent plan rendering)
    private List<PlanStep> planSteps;
    
    // Tools used by the AI for this message (executed via ToolExecutor or provider tools)
    private List<ToolUsage> toolUsages;
    
    // Qwen threading fields
    private String fid; // Unique message id
    private String parentId; // Parent message id
    private List<String> childrenIds; // Children message ids
    
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
        this.webSources = new ArrayList<>();
        this.planSteps = new ArrayList<>();
        this.toolUsages = new ArrayList<>();
        this.fid = java.util.UUID.randomUUID().toString();
        this.parentId = null;
        this.childrenIds = new ArrayList<>();
        this.userAttachmentPaths = new ArrayList<>();
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
        this.webSources = new ArrayList<>();
        this.planSteps = new ArrayList<>();
        this.toolUsages = new ArrayList<>();
        this.fid = java.util.UUID.randomUUID().toString();
        this.parentId = null;
        this.childrenIds = new ArrayList<>();
    }

    // Getters
    public int getSender() { return sender; }
    public String getContent() { return content; }
    public List<String> getActionSummaries() { return actionSummaries; }
    public List<String> getSuggestions() { return suggestions; }
    public String getAiModelName() { return aiModelName; }
    public void setAiModelName(String aiModelName) { this.aiModelName = aiModelName; }
    public long getTimestamp() { return timestamp; }
    public String getRawAiResponseJson() { return rawAiResponseJson; }
    public String getRawApiResponse() { return rawAiResponseJson; }
    public List<FileActionDetail> getProposedFileChanges() { return proposedFileChanges; }
    public List<PlanStep> getPlanSteps() { return planSteps; }
    public List<ToolUsage> getToolUsages() { return toolUsages; }
    public int getStatus() { return status; }
    public String getThinkingContent() { return thinkingContent; }
    public List<WebSource> getWebSources() { return webSources; }
    public List<String> getUserAttachmentPaths() { return userAttachmentPaths; }

    // Getters and setters for Qwen threading fields
    public String getFid() { return fid; }
    public void setFid(String fid) { this.fid = fid; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public List<String> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }


    // Setters (for updating message properties after creation, e.g., status)
    public void setContent(String content) { this.content = content; }
    public void setStatus(int status) { this.status = status; }
    public void setActionSummaries(List<String> actionSummaries) { this.actionSummaries = actionSummaries; }
    public void setProposedFileChanges(List<FileActionDetail> proposedFileChanges) { this.proposedFileChanges = proposedFileChanges; }
    public void setThinkingContent(String thinkingContent) { this.thinkingContent = thinkingContent; }
    public void setWebSources(List<WebSource> webSources) { this.webSources = webSources; }
    public void setPlanSteps(List<PlanStep> planSteps) { this.planSteps = planSteps; }
    public void setUserAttachmentPaths(List<String> paths) { this.userAttachmentPaths = paths != null ? new ArrayList<>(paths) : new ArrayList<>(); }
    public void setToolUsages(List<ToolUsage> toolUsages) { this.toolUsages = toolUsages != null ? new ArrayList<>(toolUsages) : new ArrayList<>(); }

    public com.google.gson.JsonObject toJsonObject() {
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
        jsonObject.addProperty("role", sender == SENDER_USER ? "user" : "assistant");
        jsonObject.addProperty("content", content);
        return jsonObject;
    }

    /** Plan step model for UI */
    public static class PlanStep {
        public final String id;
        public final String title;
        public final String kind;
        public String status; // pending | running | completed | failed
        public String rawResponse; // raw JSON/text response captured for this specific step
        public PlanStep(String id, String title, String kind) {
            this.id = id;
            this.title = title;
            this.kind = kind;
            this.status = "pending";
            this.rawResponse = null;
        }
    }

    /** Tool usage model for UI */
    public static class ToolUsage {
        public final String toolName;            // Tool name
        public String argsJson;              // Arguments as JSON string (pretty or compact)
        public String resultJson;            // Result as JSON string
        public boolean ok;                   // Execution success flag
        public String status;                // pending | running | completed | failed
        public long durationMs;              // Optional runtime duration
        public String filePath;              // Key file path affected/read
        public int addedLines;               // quick metrics for diffs
        public int removedLines;
        public List<String> filePaths;       // Aggregated related paths (for grouping)

        public ToolUsage(String toolName) {
            this.toolName = toolName;
            this.status = "pending";
            this.ok = false;
            this.durationMs = 0L;
            this.filePaths = new ArrayList<>();
        }
    }

    /**
     * Data class to represent a single file action detail.
     * This is used to pass detailed action information from AI to UI/Processor.
     */
    public static class FileActionDetail {
        public String type; // e.g., "createFile", "updateFile", "deleteFile", "smartUpdate"
        public String path; // Relative path for create, update, modify, delete
        public String oldPath; // For renameFile
        public String newPath; // For renameFile
        public String oldContent; // Full content before change (for diffing)
        public String newContent; // Full content after change (for diffing)
        public int startLine; // For modifyLines (1-indexed)
        public int deleteCount; // For modifyLines
        public List<String> insertLines; // For modifyLines
        public String search; // For searchAndReplace
        public String replace; // For searchAndReplace
        
        // NEW: Advanced operation fields
        public String updateType; // "full", "append", "prepend", "replace", "patch", "smart"
        public String searchPattern; // Regex pattern for smart replacements
        public String replaceWith; // Replacement content for smart updates
        public String diffPatch; // Unified diff patch content
        public String versionId; // Version identifier for tracking
        public String backupPath; // Path to backup file
        public boolean createBackup; // Whether to create backup
        public boolean validateContent; // Whether to validate content
        public String contentType; // File type for validation
        public Map<String, Object> metadata; // Additional metadata
        public List<String> validationRules; // Content validation rules
        public String errorHandling; // "strict", "lenient", "auto-revert"
        public boolean generateDiff; // Whether to generate diff
        public String diffFormat; // "unified", "context", "side-by-side"

        // Agent execution status fields
        public String stepStatus; // pending | running | completed | failed
        public String stepMessage; // latest progress/error message

        // Comprehensive constructor
        public FileActionDetail(String type, String path, String oldPath, String newPath,
                                String oldContent, String newContent, int startLine,
                                int deleteCount, List<String> insertLines, String search, String replace) {
            this.type = type;
            this.path = path;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.startLine = startLine;
            this.deleteCount = deleteCount;
            this.insertLines = insertLines != null ? new ArrayList<>(insertLines) : null;
            this.search = search;
            this.replace = replace;
            
            // Initialize advanced fields
            this.updateType = "full";
            this.createBackup = true;
            this.validateContent = true;
            this.generateDiff = true;
            this.diffFormat = "unified";
            this.errorHandling = "strict";
            this.metadata = new HashMap<>();
            this.validationRules = new ArrayList<>();

            // Initialize agent status
            this.stepStatus = "pending";
            this.stepMessage = "";
        }

        // Enhanced constructor with advanced options
        public FileActionDetail(String type, String path, String oldPath, String newPath,
                                String oldContent, String newContent, int startLine,
                                int deleteCount, List<String> insertLines, String search, String replace,
                                String updateType, String searchPattern, String replaceWith,
                                String diffPatch, String versionId, boolean createBackup,
                                boolean validateContent, String contentType, String errorHandling,
                                boolean generateDiff, String diffFormat) {
            this(type, path, oldPath, newPath, oldContent, newContent, startLine,
                 deleteCount, insertLines, search, replace);
            
            this.updateType = updateType != null ? updateType : "full";
            this.searchPattern = searchPattern;
            this.replaceWith = replaceWith;
            this.diffPatch = diffPatch;
            this.versionId = versionId;
            this.createBackup = createBackup;
            this.validateContent = validateContent;
            this.contentType = contentType;
            this.errorHandling = errorHandling != null ? errorHandling : "strict";
            this.generateDiff = generateDiff;
            this.diffFormat = diffFormat != null ? diffFormat : "unified";
            this.metadata = new HashMap<>();
            this.validationRules = new ArrayList<>();
        }

        // Method to get a displayable summary of the action
        public String getSummary() {
            switch (type) {
                case "createFile":
                    return "Create file: " + path;
                case "updateFile":
                    return "Update file: " + path + " (" + updateType + ")";
                case "smartUpdate":
                    return "Smart update file: " + path + " (" + updateType + ")";
                case "deleteFile":
                    return "Delete file: " + path;
                case "renameFile":
                    return "Rename file: " + oldPath + " to " + newPath;
                case "searchAndReplace":
                    return "Search and replace in file: " + path;
                case "patchFile":
                    return "Apply patch to file: " + path;
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

            // Serialize tool usages list to JSON string for persistence
            if (toolUsages != null && !toolUsages.isEmpty()) {
                Gson gson = new Gson();
                map.put("toolUsages", gson.toJson(toolUsages));
            } else {
                map.put("toolUsages", null);
            }

        } else {
            // User message attachments
            if (userAttachmentPaths != null && !userAttachmentPaths.isEmpty()) {
                map.put("userAttachmentPaths", new ArrayList<>(userAttachmentPaths));
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

            // Deserialize tool usages if present
            List<ToolUsage> toolUsages = null;
            String toolUsagesJson = (String) map.get("toolUsages");
            if (toolUsagesJson != null && !toolUsagesJson.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<ToolUsage>>() {}.getType();
                    toolUsages = gson.fromJson(toolUsagesJson, type);
                } catch (Exception e) {
                    Log.e("ChatMessage", "Error deserializing toolUsages: " + e.getMessage(), e);
                }
            }

            ChatMessage msg = new ChatMessage(sender, content, actionSummaries, suggestions, aiModelName,
                    timestamp, rawAiResponseJson, proposedFileChanges, status);
            if (toolUsages != null) {
                msg.setToolUsages(toolUsages);
            }
            return msg;
        } else {
            return new ChatMessage(sender, content, timestamp);
        }
    }
}
