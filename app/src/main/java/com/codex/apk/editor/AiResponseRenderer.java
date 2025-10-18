package com.codex.apk.editor;

import com.codex.apk.ChatMessage;
import com.codex.apk.ai.WebSource;

import java.util.ArrayList;
import java.util.List;

class AiResponseRenderer {

    ChatMessage buildAssistantMessage(String explanation,
                                      List<String> suggestions,
                                      String aiModelDisplayName,
                                      String rawAiResponseJson,
                                      List<ChatMessage.FileActionDetail> proposedFileChanges,
                                      List<ChatMessage.PlanStep> planSteps,
                                      String thinkingContent,
                                      List<WebSource> webSources,
                                      List<ChatMessage.ToolUsage> toolUsages) {
        String finalExplanation = explanation != null ? explanation.trim() : "";
        if ((finalExplanation == null || finalExplanation.isEmpty())
                && proposedFileChanges != null && !proposedFileChanges.isEmpty()) {
            try {
                finalExplanation = AiResponseUtils.buildFileChangeSummary(proposedFileChanges);
            } catch (Exception ignore) {}
            if (finalExplanation == null || finalExplanation.isEmpty()) {
                finalExplanation = "Proposed file changes available.";
            }
        }

        ChatMessage message = new ChatMessage(
                ChatMessage.SENDER_AI,
                finalExplanation,
                null,
                suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>(),
                aiModelDisplayName,
                System.currentTimeMillis(),
                rawAiResponseJson,
                proposedFileChanges,
                ChatMessage.STATUS_PENDING_APPROVAL
        );

        if (thinkingContent != null && !thinkingContent.isEmpty()) {
            message.setThinkingContent(thinkingContent);
        }
        if (webSources != null && !webSources.isEmpty()) {
            message.setWebSources(webSources);
        }
        if (planSteps != null && !planSteps.isEmpty()) {
            message.setPlanSteps(planSteps);
        }
        if (toolUsages != null && !toolUsages.isEmpty()) {
            message.setToolUsages(toolUsages);
        }

        return message;
    }
}
