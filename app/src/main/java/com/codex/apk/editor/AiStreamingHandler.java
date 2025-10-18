package com.codex.apk.editor;

import com.codex.apk.AIChatFragment;
import com.codex.apk.AIAssistant;
import com.codex.apk.ChatMessage;
import com.codex.apk.EditorActivity;

/**
 * Handles the lifecycle of streaming chat messages (showing and clearing the
 * "thinking" placeholder) while the AI generates responses.
 */
public class AiStreamingHandler {
    private final EditorActivity activity;
    private final AiAssistantManager manager;

    public AiStreamingHandler(EditorActivity activity, AiAssistantManager manager) {
        this.activity = activity;
        this.manager = manager;
    }

    public void handleRequestStarted(AIChatFragment chatFragment,
                                     AIAssistant aiAssistant,
                                     boolean suppressThinkingMessage) {
        if (suppressThinkingMessage) {
            if (chatFragment != null) {
                chatFragment.hideThinkingMessage();
            }
            manager.setCurrentStreamingMessagePosition(null);
            return;
        }

        if (chatFragment != null && aiAssistant != null) {
            ChatMessage aiMsg = new ChatMessage(
                    ChatMessage.SENDER_AI,
                    activity.getString(com.codex.apk.R.string.ai_is_thinking),
                    null, null,
                    aiAssistant.getCurrentModel().getDisplayName(),
                    System.currentTimeMillis(),
                    null, null,
                    ChatMessage.STATUS_NONE
            );
            manager.setCurrentStreamingMessagePosition(chatFragment.addMessage(aiMsg));
        }
    }

    public void handleRequestCompleted(AIChatFragment chatFragment) {
        if (chatFragment != null) {
            chatFragment.hideThinkingMessage();
        }
        manager.setCurrentStreamingMessagePosition(null);
    }
}
