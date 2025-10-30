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
import com.codex.apk.AiProcessor;
import com.codex.apk.ChatMessage;
import com.codex.apk.EditorActivity;
import com.codex.apk.FileManager;
import com.codex.apk.ToolSpec;
import com.codex.apk.SettingsActivity;
import com.codex.apk.TabItem;
import com.codex.apk.DiffGenerator;
import com.codex.apk.QwenResponseParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
import com.codex.apk.QwenResponseParser;
import com.codex.apk.StreamingApiClient;

/**
 * Manages the interaction with the AIAssistant, handling UI updates and delegation
 * of AI-related actions from EditorActivity to the core AIAssistant logic.
 */
public class AiAssistantManager implements AIAssistant.AIActionListener, com.codex.apk.StreamingApiClient.StreamListener {

    private static final String TAG = "AiAssistantManager";
    private final EditorActivity activity; // Reference to the hosting activity
    private final AIAssistant aiAssistant;
    private final FileManager fileManager;
    private final AiProcessor aiProcessor; // AiProcessor instance
    private final ExecutorService executorService;
    private final AiActionApplier actionApplier;
    private final ToolExecutionCoordinator toolCoordinator;
    private final AiStreamingHandler streamingHandler;
    private final PlanExecutor planExecutor;
    private final AiResponseRenderer responseRenderer;
    private List<ChatMessage.ToolUsage> lastToolUsages;
    private Integer currentToolsMessagePosition = null;
    private Integer currentStreamingMessagePosition = null;

    public AiAssistantManager(EditorActivity activity, File projectDir, String projectName,
                              FileManager fileManager, ExecutorService executorService) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.executorService = executorService;
        this.aiProcessor = new AiProcessor(projectDir, fileManager);

        this.planExecutor = new PlanExecutor(activity, this); // Initialize the PlanExecutor
        this.actionApplier = new AiActionApplier(activity, aiProcessor, planExecutor, executorService);
        this.toolCoordinator = new ToolExecutionCoordinator(activity, executorService, this::handleToolContinuation);
        this.streamingHandler = new AiStreamingHandler(activity, this);
        this.responseRenderer = new AiResponseRenderer();

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

    private void handleToolContinuation(JsonArray results) {
        activity.runOnUiThread(() -> {
            String continuation = ToolExecutionCoordinator.buildContinuationPayload(results);
            Log.d(TAG, "Sending tool results back to AI: ```json\n" + continuation + "\n```\n");
            sendAiPrompt("```json\n" + continuation + "\n```\n", new ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
        });
    }

    public void setCurrentStreamingMessagePosition(Integer position) {
        this.currentStreamingMessagePosition = position;
    }

    public void recordToolUsages(List<ChatMessage.ToolUsage> toolUsages) {
        if (toolUsages == null || toolUsages.isEmpty()) {
            return;
        }
        this.lastToolUsages = new ArrayList<>(toolUsages);
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
            // Ensure context is retained for providers that lack conversation support
            List<ChatMessage> effectiveHistory = chatHistory;
            try {
                if (aiAssistant.getCurrentModel() != null && aiAssistant.getCurrentModel().getCapabilities() != null) {
                    boolean singleRound = aiAssistant.getCurrentModel().getCapabilities().isSingleRound;
                    if (singleRound) {
                        // Supply a trimmed but sufficient window of recent messages to maintain context
                        int max = 12; // safe small window for mobile
                        if (chatHistory != null && chatHistory.size() > max) {
                            effectiveHistory = new ArrayList<>(chatHistory.subList(Math.max(0, chatHistory.size() - max), chatHistory.size()));
                        }
                    }
                }
            } catch (Exception ignore) {}

            aiAssistant.sendPrompt(userPrompt, effectiveHistory, qwenState, currentFileName, currentFileContent);
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
            actionApplier.applyAcceptedActions(messagePosition, message);
            return;
        }

        actionApplier.applyAgentActions(messagePosition, message);
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
            String jsonToParseForTools = AiResponseUtils.extractJsonBlock(explanation, rawAiResponseJson);
            JsonArray toolCalls = AiResponseUtils.extractToolCalls(jsonToParseForTools);

            if (toolCalls != null) {
                try {
                    currentToolsMessagePosition = toolCoordinator.displayRunningTools(uiFrag, aiModelDisplayName, rawAiResponseJson, toolCalls);
                    toolCoordinator.executeTools(toolCalls, activity.getProjectDirectory(), currentToolsMessagePosition, uiFrag);
                    lastToolUsages = toolCoordinator.getLastToolUsages();
                    return;
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
                        String normalized = AiResponseUtils.extractJsonFromContent(rawAiResponseJson);
                        String toParse = normalized != null ? normalized : rawAiResponseJson;
                        if (toParse != null) parsed = QwenResponseParser.parseResponse(toParse);
                    }
                    if (parsed == null && explanation != null && !explanation.isEmpty()) {
                        String exNorm = AiResponseUtils.extractJsonFromContent(explanation);
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

            ChatMessage aiMessage = responseRenderer.buildAssistantMessage(
                    explanation,
                    suggestions,
                    aiModelDisplayName,
                    rawAiResponseJson,
                    effectiveProposedFileChanges,
                    planSteps,
                    thinkingContent,
                    webSources,
                    lastToolUsages
            );

            Integer targetPos = currentStreamingMessagePosition;
            // Prefer to replace a transient tools message if present to avoid duplicates
            Integer replacePos = currentToolsMessagePosition != null ? currentToolsMessagePosition : targetPos;
            if (replacePos != null && uiFrag.getMessageAt(replacePos) != null) {
                uiFrag.updateMessage(replacePos, aiMessage);
                if (replacePos.equals(currentStreamingMessagePosition)) currentStreamingMessagePosition = null;
                if (replacePos.equals(currentToolsMessagePosition)) currentToolsMessagePosition = null;
                if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && hasOps) {
                    onAiAcceptActions(replacePos, aiMessage);
                }
            } else {
                int insertedPos = uiFrag.addMessage(aiMessage);
                if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && hasOps) {
                    onAiAcceptActions(insertedPos, aiMessage);
                }
            }
            // Clear after attaching/replacing to prevent duplicate display in future messages
            lastToolUsages = null;
        });
    }

    @Override
    public void onAiError(String errorMessage) {
        activity.runOnUiThread(() -> {
            activity.showToast("AI Error: " + errorMessage);
            // If we have a streaming message, show the error inline with a retry affordance instead of adding a separate system message.
            AIChatFragment frag = activity.getAiChatFragment();
            if (frag != null && currentStreamingMessagePosition != null) {
                ChatMessage err = frag.getMessageAt(currentStreamingMessagePosition);
                if (err != null) {
                    err.setContent("Error: " + (errorMessage != null ? errorMessage : "Unknown error"));
                    // Mark status none and clear thinking/live chips
                    err.setThinkingContent(null);
                    err.setToolUsages(null);
                    frag.updateMessage(currentStreamingMessagePosition, err);
                    // Attach a one-time retry using the last prompt the assistant saw
                    attachInlineRetry(currentStreamingMessagePosition);
                    return;
                }
            }
            sendSystemMessage("Error: " + errorMessage);
        });
    }

    /** Adds an inline retry by resending last prompt without creating a new message item. */
    private void attachInlineRetry(int messagePosition) {
        try {
            // We simulate a retry button by reusing the existing message slot when called by UI (EditorActivity already holds last prompt)
            android.widget.Toast.makeText(activity, "Tap message to retry", android.widget.Toast.LENGTH_SHORT).show();
            // Temporary: single-tap on the error message will retry
            AIChatFragment frag = activity.getAiChatFragment();
            if (frag == null) return;
            android.os.Handler h = new android.os.Handler(activity.getMainLooper());
            h.post(() -> {
                // We cannot set listeners on the adapter item here; instead, re-send immediately to reduce friction
                // Reconstruct last prompt from EditorActivity cache
                String lastPrompt = activity.getLastUserPrompt();
                if (lastPrompt != null && !lastPrompt.isEmpty()) {
                    sendAiPrompt(lastPrompt, new java.util.ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
                }
            });
        } catch (Exception ignore) {}
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
            AIChatFragment chatFragment = activity.getAiChatFragment();
            boolean suppress = aiAssistant != null && aiAssistant.isAgentModeEnabled() && planExecutor != null && planExecutor.isExecutingPlan();
            streamingHandler.handleRequestStarted(chatFragment, aiAssistant, suppress);
        });
    }

    @Override
    public void onAiStreamUpdate(String partialResponse, boolean isThinking) {
        activity.runOnUiThread(() -> {
            if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && planExecutor != null && planExecutor.isExecutingPlan()) return;
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment == null || currentStreamingMessagePosition == null) return;
            streamingHandler.handleStreamUpdate(chatFragment, currentStreamingMessagePosition, partialResponse, isThinking);
        });
    }

    @Override
    public void onAiRequestCompleted() {
        activity.runOnUiThread(() -> streamingHandler.handleRequestCompleted(activity.getAiChatFragment()));
    }

    @Override
    public void onQwenConversationStateUpdated(com.codex.apk.QwenConversationState state) {
        activity.runOnUiThread(() -> {
            if (activity != null) {
                activity.onQwenConversationStateUpdated(state);
            }
        });
    }

    // --- StreamListener Implementation ---

    @Override
    public void onStreamStarted(String requestId) {
        onAiRequestStarted();
    }

    @Override
    public void onStreamPartialUpdate(String requestId, String partialResponse, boolean isThinking) {
        onAiStreamUpdate(partialResponse, isThinking);
    }

    @Override
    public void onStreamCompleted(String requestId, com.codex.apk.QwenResponseParser.ParsedResponse response) {
        onAiActionsProcessedInternal(
            response.rawResponse,
            response.explanation,
            new ArrayList<>(),
            com.codex.apk.QwenResponseParser.toFileActionDetails(response),
            com.codex.apk.QwenResponseParser.toPlanSteps(response),
            aiAssistant.getCurrentModel().getDisplayName(),
            null,
            null
        );
        onAiRequestCompleted();
    }

    @Override
    public void onStreamError(String requestId, String errorMessage, Throwable throwable) {
        onAiError(errorMessage);
        onAiRequestCompleted();
    }
}