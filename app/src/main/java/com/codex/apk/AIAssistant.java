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
    private boolean agentModeEnabled = false; // New agent mode flag
    private List<ToolSpec> enabledTools = new ArrayList<>();
    private AIAssistant.AIActionListener actionListener;
    private File projectDir; // Track project directory for tool operations

    public AIAssistant(Context context, ExecutorService executorService, AIActionListener actionListener) {
        this.actionListener = actionListener;
        // Default to an Alibaba/Qwen model since we have a working client for it
        this.currentModel = AIModel.fromModelId("qwen3-coder-plus");

        // Initialize API clients for each provider
        apiClients.put(AIProvider.ALIBABA, new QwenApiClient(context, actionListener, null)); // projectDir can be set later
        // Register GLM client for Z provider
        apiClients.put(AIProvider.Z, new GLMApiClient(actionListener));
    }

    // Legacy constructor for compatibility
    public AIAssistant(Context context, String apiKey, File projectDir, String projectName,
        ExecutorService executorService, AIActionListener actionListener) {
        this(context, executorService, actionListener);
        // Wire the provided projectDir into Qwen client so file tools work
        this.projectDir = projectDir;
        ApiClient qwen = new QwenApiClient(context, this.actionListener, projectDir);
        apiClients.put(AIProvider.ALIBABA, qwen);
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
        ApiClient client = apiClients.get(provider);
        if (client != null) {
            new Thread(() -> {
                List<AIModel> models = client.fetchModels();
                if (models != null && !models.isEmpty()) {
                    AIModel.updateModelsForProvider(provider, models);
                    callback.onRefreshComplete(true, "Models refreshed successfully for " + provider.name());
                } else {
                    callback.onRefreshComplete(false, "Failed to refresh models for " + provider.name());
                }
            }).start();
        } else {
            callback.onRefreshComplete(false, "API client for provider " + provider.name() + " not found.");
        }
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
    public boolean isAgentModeEnabled() { return agentModeEnabled; }
    public void setAgentModeEnabled(boolean enabled) { this.agentModeEnabled = enabled; }
    public void setEnabledTools(List<ToolSpec> tools) { this.enabledTools = tools; }
    public void setActionListener(AIActionListener listener) { this.actionListener = listener; }
    public String getApiKey() { return ""; }
    public void setApiKey(String apiKey) {}
    public void shutdown() {}
}
