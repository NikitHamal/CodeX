package com.codex.apk.ai.api;

import com.codex.apk.AIAssistant;
import com.codex.apk.ChatMessage;
import com.codex.apk.QwenConversationState;
import com.codex.apk.ToolSpec;
import com.codex.apk.ai.AIModel;

import java.io.File;
import java.util.List;

/**
 * Provider-agnostic LLM client abstraction. Existing ApiClient can remain;
 * this interface will be used for new clients and for progressive migration.
 */
public interface LLMClient {
    void sendMessage(
            String message,
            AIModel model,
            List<ChatMessage> history,
            QwenConversationState state,
            boolean thinkingModeEnabled,
            boolean webSearchEnabled,
            List<ToolSpec> enabledTools,
            List<File> attachments,
            AIAssistant.AIActionListener listener
    );

    List<AIModel> fetchModels();
}
