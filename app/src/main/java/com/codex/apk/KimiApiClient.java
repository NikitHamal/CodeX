package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class KimiApiClient implements ApiClient {

    private final AIAssistant.AIActionListener actionListener;

    public KimiApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.actionListener = actionListener;
    }

    @Override
    public List<AIModel> fetchModels() {
        // Return an empty list as the provider is a placeholder
        return new ArrayList<>();
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        if (actionListener != null) {
            actionListener.onAiError("The Kimi provider is currently unavailable due to technical issues.");
        }
    }
}
