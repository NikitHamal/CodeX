package com.codex.apk.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import com.codex.apk.AIChatFragment;
import com.codex.apk.AIAssistant;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.WebSource;
import com.codex.apk.AiProcessor; // Import AiProcessor
import com.codex.apk.ChatMessage;
import com.codex.apk.CodeEditorFragment;
import com.codex.apk.EditorActivity;
import com.codex.apk.FileManager;
import com.codex.apk.ToolSpec;
import com.codex.apk.SettingsActivity;
import com.codex.apk.TabItem;
import com.codex.apk.DiffGenerator;
import com.codex.apk.QwenResponseParser; // Plan/file parsing
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.codex.apk.ToolExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Manages the interaction with the AIAssistant, handling UI updates and delegation
 * of AI-related actions from EditorActivity to the core AIAssistant logic.
 */
public class AiAssistantManager implements AIAssistant.AIActionListener { // Directly implement AIActionListener

    private static final String TAG = "AiAssistantManager";
    private final EditorActivity activity; // Reference to the hosting activity
    private final AIAssistant aiAssistant;
    private final FileManager fileManager;
    private final AiProcessor aiProcessor; // AiProcessor instance
    private final ExecutorService executorService;
    private PlanExecutor planExecutor; // The new PlanExecutor instance

    // Track current streaming AI message position
    private Integer currentStreamingMessagePosition = null;

    public AiAssistantManager(EditorActivity activity, File projectDir, String projectName,
                              FileManager fileManager, ExecutorService executorService) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.executorService = executorService;
        this.aiProcessor = new AiProcessor(projectDir, activity.getApplicationContext());
        this.planExecutor = new PlanExecutor(activity, this); // Initialize the PlanExecutor

        String apiKey = SettingsActivity.getGeminiApiKey(activity);
        this.aiAssistant = new AIAssistant(activity, apiKey, projectDir, projectName, executorService, this);
        this.aiAssistant.setEnabledTools(com.codex.apk.ToolSpec.defaultFileToolsPlusSearchNet());

        // Model selection: prefer per-project last-used, else global default, else fallback
        SharedPreferences settingsPrefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences modelPrefs = activity.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
        String projectKey = "project_" + (projectName != null ? projectName : "default") + "_last_model";
        String lastUsed = settingsPrefs.getString(projectKey, null);
        String defaultModelName = modelPrefs.getString("default_model", null);
        String initialName = lastUsed != null ? lastUsed : (defaultModelName != null ? defaultModelName : AIModel.fromModelId("qwen3-coder-plus").getDisplayName());
        AIModel initialModel = AIModel.fromDisplayName(initialName);
        if (initialModel != null) {
            this.aiAssistant.setCurrentModel(initialModel);
        }
    }

    public void setCurrentStreamingMessagePosition(Integer position) {
        this.currentStreamingMessagePosition = position;
    }

    public AIAssistant getAIAssistant() { return aiAssistant; }

    public void onResume() {
        String updatedApiKey = SettingsActivity.getGeminiApiKey(activity);
        String currentApiKeyInAssistant = aiAssistant.getApiKey();
        if (!updatedApiKey.equals(currentApiKeyInAssistant)) {
            Log.i(TAG, "API Key changed in settings. Updating AIAssistant API key.");
            aiAssistant.setApiKey(updatedApiKey);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void sendAiPrompt(String userPrompt, List<ChatMessage> chatHistory, com.codex.apk.QwenConversationState qwenState, TabItem activeTabItem) {
        if (!isNetworkAvailable()) {
            activity.showToast("No internet connection.");
            return;
        }
        String currentFileContent = "";
        String currentFileName = "";
        if (activeTabItem != null) {
            currentFileContent = activeTabItem.getContent();
            currentFileName = activeTabItem.getFileName();
        }

        if (aiAssistant == null) {
            activity.showToast("AI Assistant is not available.");
            Log.e(TAG, "sendAiPrompt: AIAssistant not initialized!");
            return;
        }

        try {
            aiAssistant.sendPrompt(userPrompt, chatHistory, qwenState, currentFileName, currentFileContent);
            // Persist per-project last used model
            String projectKey = "project_" + (activity.getProjectName() != null ? activity.getProjectName() : "default") + "_last_model";
            activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString(projectKey, aiAssistant.getCurrentModel().getDisplayName())
                    .apply();
        } catch (Exception e) {
            activity.showToast("AI Error: " + e.getMessage());
            Log.e(TAG, "AI processing error", e);
        }
    }

    public void onAiAcceptActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User accepted AI actions for message at position: " + messagePosition);
        if (message.getProposedFileChanges() == null || message.getProposedFileChanges().isEmpty()) {
            activity.showToast("No proposed changes to apply.");
            return;
        }

        boolean isAgent = aiAssistant != null && aiAssistant.isAgentModeEnabled();

        if (!isAgent) {
            executorService.execute(() -> {
                try {
                    List<String> appliedSummaries = new ArrayList<>();
                    List<File> changedFiles = new ArrayList<>();
                    for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
                        String summary = aiProcessor.applyFileAction(detail);
                        appliedSummaries.add(summary);
                        // Track changed files to refresh them
                        File fileToRefresh = new File(activity.getProjectDirectory(), detail.path);
                        if (fileToRefresh.exists()) {
                            changedFiles.add(fileToRefresh);
                        }
                        if ("renameFile".equalsIgnoreCase(detail.type) && detail.newPath != null) {
                             File newFile = new File(activity.getProjectDirectory(), detail.newPath);
                             if (newFile.exists()) {
                                 changedFiles.add(newFile);
                             }
                        }
                    }
                    activity.runOnUiThread(() -> {
                        activity.showToast("AI actions applied successfully!");
                        message.setStatus(ChatMessage.STATUS_ACCEPTED);
                        message.setActionSummaries(appliedSummaries);
                        AIChatFragment aiChatFragment = activity.getAiChatFragment();
                        if (aiChatFragment != null) {
                            aiChatFragment.updateMessage(messagePosition, message);
                        }
                        // Refresh tabs and file tree
                        activity.tabManager.refreshOpenTabsAfterAi();
                        activity.loadFileTree();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error applying AI actions: " + e.getMessage(), e);
                    activity.runOnUiThread(() -> activity.showToast("Failed to apply AI actions: " + e.getMessage()));
                }
            });
            return;
        }

        // Agent mode: auto-apply without additional approval
        executorService.execute(() -> {
            List<String> appliedSummaries = new ArrayList<>();
            List<ChatMessage.FileActionDetail> steps = message.getProposedFileChanges();

            boolean anyFailed = false;
            for (int i = 0; i < steps.size(); i++) {
                ChatMessage.FileActionDetail step = steps.get(i);

                try {
                    String summary = aiProcessor.applyFileAction(step);
                    appliedSummaries.add(summary);
                    if (planExecutor != null && planExecutor.isExecutingPlan()) {
                        planExecutor.addExecutedStepSummary(summary);
                    }
                    step.stepStatus = "completed";
                    step.stepMessage = "Completed";
                } catch (Exception ex) {
                    Log.e(TAG, "Agent step failed: " + step.getSummary(), ex);
                    step.stepStatus = "failed";
                    step.stepMessage = ex.getMessage();
                    if (planExecutor != null && planExecutor.isExecutingPlan()) {
                        planExecutor.addExecutedStepSummary("FAILED: " + step.getSummary() + " - " + ex.getMessage());
                    }
                    anyFailed = true;
                }

                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) {
                        frag.updateMessage(messagePosition, message);
                    }
                });
            }

            activity.runOnUiThread(() -> {
                message.setStatus(ChatMessage.STATUS_ACCEPTED);
                AIChatFragment frag = activity.getAiChatFragment();
                if (frag != null) frag.updateMessage(messagePosition, message);
                activity.tabManager.refreshOpenTabsAfterAi();
                activity.loadFileTree();
                activity.showToast("Agent step applied");
                if (planExecutor != null && planExecutor.isExecutingPlan()) {
                    planExecutor.onStepActionsApplied();
                }
            });
        });
    }

    private String extractJsonFromCodeBlock(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        // Look for ```json ... ``` pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```json\\s*([\\s\\S]*?)```", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Also check for ``` ... ``` pattern (without json specifier)
        pattern = java.util.regex.Pattern.compile("```\\s*([\\s\\S]*?)```");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            if (looksLikeJson(extracted)) {
                return extracted;
            }
        }
        return null;
    }

    private boolean looksLikeJson(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (!((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]")))) {
            return false;
        }
        try {
            JsonParser.parseString(trimmed);
            return true;
        } catch (com.google.gson.JsonSyntaxException e) {
            return false;
        }
    }

    private int findMatchingBraceEnd(String s, int startIdx) {
        int depth = 0; boolean inString = false; boolean escape = false;
        for (int i = startIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) { escape = false; continue; }
                if (c == '\\') { escape = true; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // Public API required by EditorActivity and UI
    public void onAiDiscardActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User discarded AI actions for message at position: " + messagePosition);
        message.setStatus(ChatMessage.STATUS_DISCARDED);
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
        }
        activity.showToast("AI actions discarded.");
    }

    public void onReapplyActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User requested to reapply AI actions for message at position: " + messagePosition);
        onAiAcceptActions(messagePosition, message);
    }

    public void acceptPlan(int messagePosition, ChatMessage message) {
        if (planExecutor != null) {
            planExecutor.acceptPlan(messagePosition, message);
        }
    }

    public void discardPlan(int messagePosition, ChatMessage message) {
        if (planExecutor != null) {
            planExecutor.discardPlan(messagePosition, message);
        }
    }

    public void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail) {
        Log.d(TAG, "User clicked on file change: " + fileActionDetail.path + " (" + fileActionDetail.type + ")");

        String fileNameToOpen = fileActionDetail.path; // Default to path
        if (fileActionDetail.type != null && fileActionDetail.type.equals("renameFile")) {
            fileNameToOpen = fileActionDetail.newPath; // Use newPath for renamed files
        }

        // Ensure we have authoritative old/new contents; if missing, read from disk and/or synthesize
        String oldFileContent = fileActionDetail.oldContent != null ? fileActionDetail.oldContent : "";
        String newFileContent = fileActionDetail.newContent != null ? fileActionDetail.newContent : "";

        try {
            File projectDir = activity.getProjectDirectory();
            if ((oldFileContent == null || oldFileContent.isEmpty())) {
                String pathToRead = "renameFile".equals(fileActionDetail.type) ? fileActionDetail.oldPath : fileActionDetail.path;
                if (pathToRead != null && !pathToRead.isEmpty() && projectDir != null) {
                    File f = new File(projectDir, pathToRead);
                    if (f.exists()) {
                        oldFileContent = activity.getFileManager().readFileContent(f);
                    }
                }
            }
            if ((newFileContent == null || newFileContent.isEmpty()) && "renameFile".equals(fileActionDetail.type)) {
                String pathToRead = fileActionDetail.newPath;
                if (pathToRead != null && !pathToRead.isEmpty() && projectDir != null) {
                    File f = new File(projectDir, pathToRead);
                    if (f.exists()) {
                        newFileContent = activity.getFileManager().readFileContent(f);
                    }
                }
            }
        } catch (Exception ignore) {}

        // Prefer provided diffPatch if it looks like a valid unified diff; otherwise, generate fallback
        String providedPatch = fileActionDetail.diffPatch != null ? fileActionDetail.diffPatch.trim() : "";
        boolean looksUnified = false;
        if (!providedPatch.isEmpty()) {
            // Heuristics: presence of @@ hunk headers or ---/+++ file markers
            looksUnified = providedPatch.contains("@@ ") || (providedPatch.startsWith("--- ") && providedPatch.contains("\n+++ "));
        }

        String diffContent;
        if (!providedPatch.isEmpty() && looksUnified) {
            diffContent = providedPatch;
        } else {
            // Fallback: generate unified diff from contents with appropriate file marker paths
            if ("createFile".equals(fileActionDetail.type)) {
                diffContent = DiffGenerator.generateDiff("", newFileContent, "unified", "/dev/null", "b/" + fileNameToOpen);
            } else if ("deleteFile".equals(fileActionDetail.type)) {
                diffContent = DiffGenerator.generateDiff(oldFileContent, "", "unified", "a/" + fileNameToOpen, "/dev/null");
            } else if ("renameFile".equals(fileActionDetail.type)) {
                String oldPath = fileActionDetail.oldPath != null ? fileActionDetail.oldPath : fileNameToOpen;
                String newPath = fileActionDetail.newPath != null ? fileActionDetail.newPath : fileNameToOpen;
                diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + oldPath, "b/" + newPath);
            } else { // updateFile, smartUpdate, patchFile, modifyLines, etc.
                // If we have modifyLines or searchAndReplace info but newContent is empty, try constructing a minimal new content
                String effectiveNew = newFileContent;
                if ((effectiveNew == null || effectiveNew.isEmpty())) {
                    try {
                        if (fileActionDetail.diffPatch != null && !fileActionDetail.diffPatch.isEmpty()) {
                            // Show provided patch even if new content missing
                            diffContent = fileActionDetail.diffPatch;
                        } else if (fileActionDetail.insertLines != null && fileActionDetail.startLine > 0) {
                            effectiveNew = com.codex.apk.util.FileOps.applyModifyLines(oldFileContent, fileActionDetail.startLine, fileActionDetail.deleteCount, fileActionDetail.insertLines);
                            diffContent = DiffGenerator.generateDiff(oldFileContent, effectiveNew, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
                        } else if ((fileActionDetail.searchPattern != null || fileActionDetail.search != null) && (fileActionDetail.replaceWith != null || fileActionDetail.replace != null)) {
                            String pattern = fileActionDetail.searchPattern != null ? fileActionDetail.searchPattern : fileActionDetail.search;
                            String repl = fileActionDetail.replaceWith != null ? fileActionDetail.replaceWith : fileActionDetail.replace;
                            effectiveNew = com.codex.apk.util.FileOps.applySearchReplace(oldFileContent, pattern, repl);
                            diffContent = DiffGenerator.generateDiff(oldFileContent, effectiveNew, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
                        } else if (fileActionDetail.newContent != null && !fileActionDetail.newContent.isEmpty()) {
                            effectiveNew = fileActionDetail.newContent;
                            diffContent = DiffGenerator.generateDiff(oldFileContent, effectiveNew, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
                        } else {
                            diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
                        }
                    } catch (Exception e) {
                        diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
                    }
                } else {
                    diffContent = DiffGenerator.generateDiff(oldFileContent, effectiveNew, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
                }
            }
        }

        activity.tabManager.openDiffTab(fileNameToOpen, diffContent);
    }

    public void shutdown() { if (aiAssistant != null) aiAssistant.shutdown(); }

    // --- Implement AIAssistant.AIActionListener methods ---
    @Override
    public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName) {
        onAiActionsProcessedInternal(rawAiResponseJson, explanation, suggestions, proposedFileChanges, new ArrayList<>(), aiModelDisplayName, null, null);
    }

    @Override
    public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> proposedFileChanges, List<ChatMessage.PlanStep> planSteps, String aiModelDisplayName) {
        onAiActionsProcessedInternal(rawAiResponseJson, explanation, suggestions, proposedFileChanges, planSteps, aiModelDisplayName, null, null);
    }

    public void onAiActionsProcessed(String rawAiResponseJson, String explanation,
                                   List<String> suggestions,
                                   List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName,
                                   String thinkingContent, List<WebSource> webSources) {
        // This method now delegates to the new internal method, creating an empty plan list.
        onAiActionsProcessedInternal(rawAiResponseJson, explanation, suggestions, proposedFileChanges, new ArrayList<>(), aiModelDisplayName, thinkingContent, webSources);
    }

    private void onAiActionsProcessedInternal(String rawAiResponseJson, String explanation,
                                              List<String> suggestions,
                                              List<ChatMessage.FileActionDetail> proposedFileChanges,
                                              List<ChatMessage.PlanStep> planSteps,
                                              String aiModelDisplayName,
                                              String thinkingContent, List<WebSource> webSources) {
        activity.runOnUiThread(() -> {
            AIChatFragment uiFrag = activity.getAiChatFragment();
            if (uiFrag == null) {
                Log.w(TAG, "AiChatFragment is null! Cannot add message to UI.");
                return;
            }

            boolean isCurrentlyExecutingPlan = planExecutor != null && planExecutor.isExecutingPlan();

            if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && isCurrentlyExecutingPlan) {
                uiFrag.hideThinkingMessage();
                currentStreamingMessagePosition = null;
            }

            // Centralized tool call handling
            String jsonToParseForTools = extractJsonFromCodeBlock(explanation);
            if (jsonToParseForTools == null) {
                jsonToParseForTools = extractJsonFromCodeBlock(rawAiResponseJson);
            }
            if (jsonToParseForTools == null && looksLikeJson(explanation)) {
                jsonToParseForTools = explanation;
            }

            if (jsonToParseForTools != null) {
                try {
                    JsonObject maybe = JsonParser.parseString(jsonToParseForTools).getAsJsonObject();
                    if (maybe.has("action") && "tool_call".equalsIgnoreCase(maybe.get("action").getAsString()) && maybe.has("tool_calls") && maybe.get("tool_calls").isJsonArray()) {
                        JsonArray calls = maybe.getAsJsonArray("tool_calls");
                        JsonArray results = new JsonArray();
                        File projectDir = activity.getProjectDirectory();
                        for (int i = 0; i < calls.size(); i++) {
                            try {
                                JsonObject c = calls.get(i).getAsJsonObject();
                                String name = c.get("name").getAsString();
                                JsonObject args = c.has("args") && c.get("args").isJsonObject() ? c.getAsJsonObject("args") : new JsonObject();
                                JsonObject res = new JsonObject();
                                res.addProperty("name", name);
                                JsonObject exec = ToolExecutor.execute(projectDir, name, args);
                                res.add("result", exec);
                                results.add(res);
                            } catch (Exception inner) {
                                JsonObject res = new JsonObject();
                                res.addProperty("name", "unknown");
                                JsonObject err = new JsonObject();
                                err.addProperty("ok", false);
                                err.addProperty("error", inner.getMessage());
                                res.add("result", err);
                                results.add(res);
                                Log.w(TAG, "Error executing tool", inner);
                            }
                        }
                        String continuation = ToolExecutor.buildToolResultContinuation(results);
                        String fenced = "```json\n" + continuation + "\n```\n";
                        Log.d(TAG, "Sending tool results back to AI: " + fenced);
                        sendAiPrompt(fenced, new java.util.ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not execute tool call. Error parsing JSON.", e);
                }
            }

            List<ChatMessage.FileActionDetail> effectiveProposedFileChanges = proposedFileChanges;
            boolean isPlan = planSteps != null && !planSteps.isEmpty();

            if ((effectiveProposedFileChanges == null || effectiveProposedFileChanges.isEmpty()) && !isPlan) {
                try {
                    QwenResponseParser.ParsedResponse parsed = null;
                    if (rawAiResponseJson != null) {
                        String normalized = extractJsonFromCodeBlock(rawAiResponseJson);
                        String toParse = normalized != null ? normalized : rawAiResponseJson;
                        if (toParse != null) parsed = QwenResponseParser.parseResponse(toParse);
                    }
                    if (parsed == null && explanation != null && !explanation.isEmpty()) {
                        String exNorm = extractJsonFromCodeBlock(explanation);
                        if (exNorm != null) parsed = QwenResponseParser.parseResponse(exNorm);
                    }
                    if (parsed != null && parsed.action != null && parsed.action.contains("file")) {
                        effectiveProposedFileChanges = QwenResponseParser.toFileActionDetails(parsed);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Fallback file op parse failed", e);
                }
            }

            boolean hasOps = effectiveProposedFileChanges != null && !effectiveProposedFileChanges.isEmpty();
            if (isPlan && hasOps) {
                Log.w(TAG, "Mixed 'plan' and 'file_operation' detected; preferring plan.");
                effectiveProposedFileChanges = new ArrayList<>();
            }

            if (isCurrentlyExecutingPlan) {
                planExecutor.onStepExecutionResult(effectiveProposedFileChanges, rawAiResponseJson, explanation);
                return;
            }

            String finalExplanation = explanation != null ? explanation.trim() : "";
            if ((finalExplanation == null || finalExplanation.isEmpty()) && effectiveProposedFileChanges != null && !effectiveProposedFileChanges.isEmpty()) {
                // Build a concise summary with aggregated counts per affected file
                try {
                    finalExplanation = buildFileChangeSummary(effectiveProposedFileChanges);
                } catch (Exception ignore) {}
                if (finalExplanation == null || finalExplanation.isEmpty()) {
                    finalExplanation = "Proposed file changes available.";
                }
            }
            ChatMessage aiMessage = new ChatMessage(
                    ChatMessage.SENDER_AI, finalExplanation, null,
                    suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>(),
                    aiModelDisplayName, System.currentTimeMillis(), rawAiResponseJson,
                    effectiveProposedFileChanges, ChatMessage.STATUS_PENDING_APPROVAL
            );
            if (thinkingContent != null && !thinkingContent.isEmpty()) {
                aiMessage.setThinkingContent(thinkingContent);
            }
            if (webSources != null && !webSources.isEmpty()) {
                aiMessage.setWebSources(webSources);
            }
            if (isPlan) {
                aiMessage.setPlanSteps(planSteps);
            }

            Integer targetPos = currentStreamingMessagePosition;
            if (targetPos != null && uiFrag.getMessageAt(targetPos) != null) {
                uiFrag.updateMessage(targetPos, aiMessage);
                currentStreamingMessagePosition = null;
                if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && hasOps) {
                    onAiAcceptActions(targetPos, aiMessage);
                }
            } else {
                int insertedPos = uiFrag.addMessage(aiMessage);
                if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && hasOps) {
                    onAiAcceptActions(insertedPos, aiMessage);
                }
            }
        });
    }

    private String buildFileChangeSummary(List<ChatMessage.FileActionDetail> details) {
        // Aggregate by effective path (tracking renames in this message)
        java.util.LinkedHashMap<String, int[]> countsByPath = new java.util.LinkedHashMap<>();
        java.util.Set<String> allPaths = new java.util.LinkedHashSet<>();
        for (ChatMessage.FileActionDetail d : details) {
            if (d.path != null && !d.path.isEmpty()) allPaths.add(d.path);
            if ("renameFile".equals(d.type)) {
                if (d.oldPath != null && !d.oldPath.isEmpty()) allPaths.add(d.oldPath);
                if (d.newPath != null && !d.newPath.isEmpty()) allPaths.add(d.newPath);
            }
        }
        // Build alias set per path by walking rename chain backwards
        java.util.Map<String, java.util.Set<String>> aliases = new java.util.HashMap<>();
        for (String p : allPaths) {
            java.util.Set<String> set = new java.util.LinkedHashSet<>();
            set.add(p);
            boolean changed;
            for (int pass = 0; pass < 3; pass++) {
                changed = false;
                for (ChatMessage.FileActionDetail a : details) {
                    if ("renameFile".equals(a.type) && a.newPath != null && set.contains(a.newPath) && a.oldPath != null) {
                        if (set.add(a.oldPath)) changed = true;
                    }
                }
                if (!changed) break;
            }
            aliases.put(p, set);
        }

        // Define helper to find a canonical display name
        java.util.function.Function<ChatMessage.FileActionDetail, String> displayPath = a -> {
            if ("renameFile".equals(a.type) && a.newPath != null && !a.newPath.isEmpty()) return a.newPath;
            return a.path != null ? a.path : "";
        };

        // Aggregate counts
        for (ChatMessage.FileActionDetail a : details) {
            String key = displayPath.apply(a);
            if (key.isEmpty()) continue;
            int[] current = countsByPath.computeIfAbsent(key, k -> new int[]{0, 0});
            if ("modifyLines".equals(a.type)) {
                current[0] += (a.insertLines != null) ? a.insertLines.size() : 0;
                current[1] += Math.max(0, a.deleteCount);
            } else if (a.diffPatch != null && !a.diffPatch.isEmpty()) {
                int[] c = com.codex.apk.DiffUtils.countAddRemove(a.diffPatch);
                current[0] += c[0];
                current[1] += c[1];
            } else if ("createFile".equals(a.type) && a.newContent != null) {
                current[0] += countLines(a.newContent);
            } else if ("deleteFile".equals(a.type) && a.oldContent != null) {
                current[1] += countLines(a.oldContent);
            } else if (a.oldContent != null || a.newContent != null) {
                int[] c = com.codex.apk.DiffUtils.countAddRemoveFromContents(a.oldContent, a.newContent);
                current[0] += c[0];
                current[1] += c[1];
            }
        }

        if (countsByPath.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Changes:\n");
        for (java.util.Map.Entry<String, int[]> e : countsByPath.entrySet()) {
            int add = e.getValue()[0];
            int rem = e.getValue()[1];
            sb.append("- ").append(e.getKey());
            if (add > 0 || rem > 0) sb.append(" (+").append(add).append(" -").append(rem).append(")");
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private int countLines(String s) {
        if (s == null || s.isEmpty()) return 0;
        int lines = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') lines++;
        return lines;
    }

    @Override
    public void onAiError(String errorMessage) {
        activity.runOnUiThread(() -> {
            activity.showToast("AI Error: " + errorMessage);
            sendSystemMessage("Error: " + errorMessage);
        });
    }

    private void sendSystemMessage(String content) {
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            ChatMessage systemMessage = new ChatMessage(
                    ChatMessage.SENDER_AI,
                    content,
                    null, null,
                    "System",
                    System.currentTimeMillis(),
                    null, null,
                    ChatMessage.STATUS_NONE
            );
            aiChatFragment.addMessage(systemMessage);
        }
    }

    @Override
    public void onAiRequestStarted() {
        activity.runOnUiThread(() -> {
            boolean suppress = aiAssistant != null && aiAssistant.isAgentModeEnabled() && planExecutor != null && planExecutor.isExecutingPlan();
            if (suppress) {
                AIChatFragment uiFrag = activity.getAiChatFragment();
                if (uiFrag != null) {
                    uiFrag.hideThinkingMessage();
                }
                currentStreamingMessagePosition = null;
                return;
            }
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null) {
                ChatMessage aiMsg = new ChatMessage(
                        ChatMessage.SENDER_AI,
                        activity.getString(com.codex.apk.R.string.ai_is_thinking),
                        null, null,
                        aiAssistant.getCurrentModel().getDisplayName(),
                        System.currentTimeMillis(),
                        null, null,
                        ChatMessage.STATUS_NONE
                );
                currentStreamingMessagePosition = chatFragment.addMessage(aiMsg);
            }
        });
    }

    @Override
    public void onAiStreamUpdate(String partialResponse, boolean isThinking) {
        activity.runOnUiThread(() -> {
            if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && planExecutor != null && planExecutor.isExecutingPlan()) return;
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment == null || currentStreamingMessagePosition == null) return;
            ChatMessage msg = chatFragment.getMessageAt(currentStreamingMessagePosition);
            if (msg == null) return;

            if (isThinking) {
                msg.setThinkingContent(partialResponse);
                // When thinking, the main content should be blank or a placeholder
                if (msg.getContent() == null || !msg.getContent().equals(activity.getString(com.codex.apk.R.string.ai_is_thinking))) {
                    msg.setContent("");
                }
            } else {
                msg.setContent(partialResponse != null ? partialResponse : "");
                // Clear thinking content when we get a final response
                msg.setThinkingContent(null);
            }
            chatFragment.updateMessage(currentStreamingMessagePosition, msg);
        });
    }

    @Override
    public void onAiRequestCompleted() {
        activity.runOnUiThread(() -> {
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null) {
                chatFragment.hideThinkingMessage();
            }
            currentStreamingMessagePosition = null;
        });
    }

    @Override
    public void onQwenConversationStateUpdated(com.codex.apk.QwenConversationState state) {
        activity.runOnUiThread(() -> {
            if (activity != null) {
                activity.onQwenConversationStateUpdated(state);
            }
        });
    }
}