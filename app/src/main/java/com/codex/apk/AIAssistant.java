package com.codex.apk;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;

public class AIAssistant {

    private Map<AIProvider, ApiClient> apiClients = new HashMap<>();
    private AIModel currentModel;
    private boolean thinkingModeEnabled = false;
    private boolean webSearchEnabled = false;
    private List<ToolSpec> enabledTools = new ArrayList<>();
    private AIActionListener actionListener;

    public AIAssistant(Context context, ExecutorService executorService, AIActionListener actionListener) {
        this.actionListener = actionListener;
        this.currentModel = AIModel.GEMINI_2_5_FLASH; // Default model

        // Initialize API clients for each provider
        apiClients.put(AIProvider.ALIBABA, new QwenApiClient(context, actionListener, null)); // projectDir can be set later
        // apiClients.put(AIProvider.Z, new GLMApiClient(actionListener));
        // GLMApiClient needs to be updated to implement ApiClient
    }

    // Legacy constructor for compatibility
    public AIAssistant(Context context, String apiKey, File projectDir, String projectName,
        ExecutorService executorService, AIActionListener actionListener) {
        this(context, executorService, actionListener);
    }

    public void sendPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState, String fileName, String fileContent) {
        // For now, attachments are not handled in this refactored version.
        // This would need to be threaded through if a model that uses them is selected.
        sendMessage(userPrompt, chatHistory, qwenState, new ArrayList<>());
    }

    public void sendMessage(String message, List<ChatMessage> chatHistory, QwenConversationState qwenState, List<File> attachments) {
        ApiClient client = apiClients.get(currentModel.getProvider());
        if (client != null) {
            client.sendMessage(message, currentModel, chatHistory, qwenState, thinkingModeEnabled, webSearchEnabled, enabledTools, attachments);
        } else {
            if (actionListener != null) {
                actionListener.onAiError("API client for provider " + currentModel.getProvider() + " not found.");
            }
        }
    }

    public void refreshModelsForProvider(AIProvider provider, RefreshCallback callback) {
        callback.onRefreshComplete(false, "Provider does not support refresh");
    }

    public interface RefreshCallback {
        void onRefreshComplete(boolean success, String message);
    }

    public interface AIActionListener {
        void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions,
                                 List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName);
        void onAiError(String errorMessage);
        void onAiRequestStarted();
        void onAiStreamUpdate(String partialResponse, boolean isThinking);
        void onAiRequestCompleted();
        void onQwenConversationStateUpdated(QwenConversationState state);
    }

    // Getters and Setters
    public AIModel getCurrentModel() { return currentModel; }
    public void setCurrentModel(AIModel model) { this.currentModel = model; }
    public boolean isThinkingModeEnabled() { return thinkingModeEnabled; }
    public void setThinkingModeEnabled(boolean enabled) { this.thinkingModeEnabled = enabled; }
    public boolean isWebSearchEnabled() { return webSearchEnabled; }
    public void setWebSearchEnabled(boolean enabled) { this.webSearchEnabled = enabled; }
    public void setEnabledTools(List<ToolSpec> tools) { this.enabledTools = tools; }
    public void setActionListener(AIActionListener listener) { this.actionListener = listener; }
    public String getApiKey() { return ""; }
    public void setApiKey(String apiKey) {}
    public void shutdown() {}
}
