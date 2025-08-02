package com.codex.apk;

import java.util.List;

import java.io.File;
import com.codex.apk.ai.AIModel;

public interface ApiClient {
    void sendMessage(
        String message,
        AIModel model,
        List<ChatMessage> history,
        QwenConversationState state,
        boolean thinkingModeEnabled,
        boolean webSearchEnabled,
        List<ToolSpec> enabledTools,
        List<File> attachments
    );
}
