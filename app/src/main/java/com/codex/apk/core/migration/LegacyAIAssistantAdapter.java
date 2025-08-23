package com.codex.apk.core.migration;

import android.content.Context;

import com.codex.apk.AIAssistant;
import com.codex.apk.ApiClient;
import com.codex.apk.ChatMessage;
import com.codex.apk.QwenConversationState;
import com.codex.apk.ToolSpec;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.core.service.AIServiceManager;
import com.codex.apk.core.model.AIRequest;
import com.codex.apk.core.model.AIResponse;
import com.codex.apk.core.model.Message;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Migration compatibility layer that maintains the existing AIAssistant API
 * while delegating to the new modular architecture underneath.
 * This allows gradual migration without breaking existing code.
 */
public class LegacyAIAssistantAdapter extends AIAssistant {
    
    private final AIServiceManager serviceManager;
    private final LegacyToModernConverter converter;
    private final Context context;
    
    public LegacyAIAssistantAdapter(Context context, ExecutorService executorService, 
                                  AIActionListener actionListener, AIServiceManager serviceManager) {
        super(context, executorService, actionListener);
        this.context = context;
        this.serviceManager = serviceManager;
        this.converter = new LegacyToModernConverter();
    }
    
    @Override
    public void sendMessage(String message, List<ChatMessage> history, QwenConversationState qwenState, List<File> attachments) {
        try {
            // Convert legacy parameters to new format
            AIRequest request = converter.convertToAIRequest(
                message, getCurrentModel(), history, qwenState, 
                isThinkingModeEnabled(), isWebSearchEnabled(), 
                getEnabledTools(), attachments
            );
            
            // Execute through new architecture
            serviceManager.executeRequest(request, this::handleResponse, this::handleError)
                    .thenRun(this::handleComplete);
                
        } catch (Exception e) {
            if (getActionListener() != null) {
                getActionListener().onAiError("Request conversion failed: " + e.getMessage());
            }
        }
    }
    
    private void handleResponse(AIResponse response) {
        AIActionListener listener = getActionListener();
        if (listener == null) return;
        
        if (response.isStreaming()) {
            listener.onAiStreamUpdate(response.getContent(), response.hasThinking());
        }
        
        if (response.isComplete()) {
            // Convert back to legacy format
            LegacyResponse legacyResponse = converter.convertToLegacyResponse(response);
            listener.onAiActionsProcessed(
                legacyResponse.rawJson,
                legacyResponse.explanation,
                legacyResponse.suggestions,
                legacyResponse.fileActions,
                legacyResponse.modelName
            );
        }
    }
    
    private void handleError(Throwable error) {
        if (getActionListener() != null) {
            getActionListener().onAiError(error.getMessage());
        }
    }
    
    private void handleComplete() {
        if (getActionListener() != null) {
            getActionListener().onAiRequestCompleted();
        }
    }
    
    // Override other methods as needed to maintain compatibility
    @Override
    public void refreshModelsForProvider(AIProvider provider, RefreshCallback callback) {
        // Use new service manager for model fetching
        try {
            serviceManager.getService(provider)
                .getModels()
                .thenAccept(models -> {
                    if (models != null && !models.isEmpty()) {
                        AIModel.updateModelsForProvider(provider, models);
                        callback.onRefreshComplete(true, "Models refreshed successfully for " + provider.name());
                    } else {
                        callback.onRefreshComplete(false, "Failed to refresh models for " + provider.name());
                    }
                })
                .exceptionally(throwable -> {
                    callback.onRefreshComplete(false, "Error refreshing models: " + throwable.getMessage());
                    return null;
                });
        } catch (Exception e) {
            callback.onRefreshComplete(false, "Service error: " + e.getMessage());
        }
    }
    
    /**
     * Gets the underlying service manager for advanced operations.
     * This allows gradual migration by exposing the new architecture.
     * 
     * @return The service manager instance
     */
    public AIServiceManager getServiceManager() {
        return serviceManager;
    }
    
    /**
     * Converter helper class for transforming between legacy and modern formats.
     */
    private static class LegacyToModernConverter {
        
        public AIRequest convertToAIRequest(String message, AIModel model, List<ChatMessage> history,
                                          QwenConversationState qwenState, boolean thinkingMode, 
                                          boolean webSearch, List<ToolSpec> tools, List<File> attachments) {
            
            AIRequest.Builder builder = AIRequest.builder()
                .withModel(model != null ? model.getModelId() : null);
            
            // Convert message history
            if (history != null) {
                for (ChatMessage chatMsg : history) {
                    Message.MessageRole role = chatMsg.getSender() == ChatMessage.SENDER_USER ? 
                        Message.MessageRole.USER : Message.MessageRole.ASSISTANT;
                    builder.addMessage(Message.builder()
                        .withRole(role)
                        .withContent(chatMsg.getContent())
                        .build());
                }
            }
            
            // Add current message
            builder.addMessage(Message.user(message));
            
            // Set parameters
            builder.withParameters(com.codex.apk.core.model.RequestParameters.builder()
                .withStream(true) // Default to streaming for better UX
                .build());
            
            // Set required capabilities
            com.codex.apk.core.model.RequiredCapabilities capabilities = 
                new com.codex.apk.core.model.RequiredCapabilities(
                    true,     // streaming
                    !attachments.isEmpty(), // vision if attachments
                    tools != null && !tools.isEmpty(), // tools
                    webSearch, // web search
                    thinkingMode, // thinking
                    false     // multimodal
                );
            builder.requireCapabilities(capabilities);
            
            // Add tools
            if (tools != null) {
                builder.withTools(tools);
            }
            
            // Add metadata for legacy compatibility
            builder.addMetadata("legacy_qwen_state", qwenState);
            builder.addMetadata("thinking_mode", thinkingMode);
            builder.addMetadata("web_search", webSearch);
            
            try {
                return builder.build();
            } catch (AIRequest.ValidationException e) {
                throw new RuntimeException("Failed to convert legacy request", e);
            }
        }
        
        public LegacyResponse convertToLegacyResponse(AIResponse response) {
            LegacyResponse legacy = new LegacyResponse();
            legacy.rawJson = response.toString(); // Simplified
            legacy.explanation = response.getContent();
            legacy.suggestions = java.util.Collections.emptyList(); // Could be extracted from response
            legacy.fileActions = java.util.Collections.emptyList(); // Could be converted from tool calls
            legacy.modelName = response.getModel();
            return legacy;
        }
    }
    
    /**
     * Legacy response format for compatibility.
     */
    private static class LegacyResponse {
        String rawJson;
        String explanation;
        List<String> suggestions;
        List<ChatMessage.FileActionDetail> fileActions;
        String modelName;
    }
    
    // Helper methods for accessing private fields from parent class
    private AIActionListener getActionListener() {
        // Would need to expose this in parent class or use reflection
        return null; // Placeholder
    }
    
    private List<ToolSpec> getEnabledTools() {
        // Would need to expose this in parent class
        return java.util.Collections.emptyList(); // Placeholder
    }
}