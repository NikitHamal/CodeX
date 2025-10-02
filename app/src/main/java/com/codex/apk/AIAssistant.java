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
    private String apiKey = ""; // Tracks Gemini (official) API key

    public AIAssistant(Context context, ExecutorService executorService, AIActionListener actionListener) {
        this.actionListener = actionListener;
        this.currentModel = AIModel.fromModelId("qwen3-coder-plus");
        initializeApiClients(context, null);
    }

    // Legacy constructor for compatibility
    public AIAssistant(Context context, String apiKey, File projectDir, String projectName,
        ExecutorService executorService, AIActionListener actionListener) {
        this.actionListener = actionListener;
        this.currentModel = AIModel.fromModelId("qwen3-coder-plus");
        this.projectDir = projectDir;
        initializeApiClients(context, projectDir);
        if (apiKey != null) this.apiKey = apiKey; else this.apiKey = SettingsActivity.getGeminiApiKey(context);
        setApiKey(this.apiKey);
    }

    private void initializeApiClients(Context context, File projectDir) {
        apiClients.put(AIProvider.ALIBABA, new QwenApiClient(context, actionListener, projectDir));
        apiClients.put(AIProvider.DEEPINFRA, new DeepInfraApiClient(context, actionListener));
        apiClients.put(AIProvider.FREE, new AnyProviderApiClient(context, actionListener));
        apiClients.put(AIProvider.COOKIES, new GeminiFreeApiClient(context, actionListener));
        String initialKey = SettingsActivity.getGeminiApiKey(context);
        this.apiKey = initialKey != null ? initialKey : "";
        apiClients.put(AIProvider.GOOGLE, new GeminiOfficialApiClient(context, actionListener, this.apiKey));
        apiClients.put(AIProvider.ZHIPU, new ZhipuApiClient(context, actionListener, projectDir));
        apiClients.put(AIProvider.OIVSCodeSer0501, new OIVSCodeSer0501ApiClient(context, actionListener));
        apiClients.put(AIProvider.OIVSCodeSer2, new OIVSCodeSer2ApiClient(context, actionListener));
        apiClients.put(AIProvider.WEWORDLE, new WeWordleApiClient(context, actionListener));
        apiClients.put(AIProvider.OPENROUTER, new OpenRouterApiClient(context, actionListener));
    }

    public void sendPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState, String fileName, String fileContent) {
        // For now, attachments are not handled in this refactored version.
        // This would need to be threaded through if a model that uses them is selected.
        sendMessage(userPrompt, chatHistory, qwenState, new ArrayList<>(), fileName, fileContent);
    }

    public void sendMessage(String message, List<ChatMessage> chatHistory, QwenConversationState qwenState, List<File> attachments) {
        sendMessage(message, chatHistory, qwenState, attachments, null, null);
    }

    public void sendMessage(String message, List<ChatMessage> chatHistory, QwenConversationState qwenState, List<File> attachments, String fileName, String fileContent) {
        ApiClient client = apiClients.get(currentModel.getProvider());
        if (client != null) {
            String finalMessage = message;

            if (fileContent != null && !fileContent.isEmpty()) {
                finalMessage = "Here is the content of the file `" + fileName + "`:\n\n```\n" + fileContent + "\n```\n\nNow, please perform the following task: " + message;
            }

            // Choose system prompt based on agent mode
            String system = null;
            if (agentModeEnabled && enabledTools != null && !enabledTools.isEmpty()) {
                system = PromptManager.getDefaultFileOpsPrompt();
            } else {
                system = PromptManager.getDefaultGeneralPrompt();
            }
            if (system != null && !system.isEmpty()) {
                finalMessage = system + "\n\n" + finalMessage;
            }
            // Note: Gemini Free context is maintained via server-side conversation metadata (cid,rid,rcid)
            List<File> safeAttachments = attachments;
            if (currentModel != null && currentModel.getProvider() != AIProvider.COOKIES) {
                safeAttachments = new ArrayList<>();
            }
            client.sendMessage(finalMessage, currentModel, chatHistory, qwenState, thinkingModeEnabled, webSearchEnabled, enabledTools, safeAttachments);
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
    public String getApiKey() { return this.apiKey; }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
        ApiClient google = apiClients.get(AIProvider.GOOGLE);
        if (google instanceof GeminiOfficialApiClient) {
            ((GeminiOfficialApiClient) google).setApiKey(this.apiKey);
        }
    }
    public void shutdown() {}
}
