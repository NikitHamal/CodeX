package com.codex.apk.editor;

import com.codex.apk.AIChatFragment;
import com.codex.apk.AIAssistant;
import com.codex.apk.ChatMessage;
import com.codex.apk.EditorActivity;
import com.codex.apk.util.AdaptiveUIThrottler;

/**
 * Handles the lifecycle of streaming chat messages (showing and clearing the
 * "thinking" placeholder) while the AI generates responses.
 */
public class AiStreamingHandler {
    private final EditorActivity activity;
    private final AiAssistantManager manager;
    private final AdaptiveUIThrottler throttler;

    public AiStreamingHandler(EditorActivity activity, AiAssistantManager manager) {
        this.activity = activity;
        this.manager = manager;
        this.throttler = new AdaptiveUIThrottler(activity);
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

    public void handleStreamUpdate(AIChatFragment chatFragment,
                                   int messagePosition,
                                   String partialResponse,
                                   boolean isThinking) {
        if (chatFragment == null) {
            return;
        }

        ChatMessage existing = chatFragment.getMessageAt(messagePosition);
        if (existing == null) {
            return;
        }

        if (isThinking) {
            existing.setContent(partialResponse != null ? partialResponse : existing.getContent());
        } else {
            existing.setContent(partialResponse != null ? partialResponse : existing.getContent());
            existing.setThinkingContent(null);
        }
        throttler.scheduleUpdate(() -> chatFragment.updateMessage(messagePosition, existing), 1);
    }
}
