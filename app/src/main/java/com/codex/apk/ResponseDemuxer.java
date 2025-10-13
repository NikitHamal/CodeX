package com.codex.apk;

import com.codex.apk.util.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized demux for provider completions: extracts JSON from text,
 * parses into plan or file operations, and notifies the UI uniformly.
 *
 * Tool-calls are handled upstream for Qwen (since it supports continuation);
 * for generic providers we only surface plan/file ops or plain text.
 */
public final class ResponseDemuxer {
    private ResponseDemuxer() {}

    public static void handleGeneric(
            AIAssistant.AIActionListener listener,
            String modelDisplayName,
            String rawResponse,
            String explanation,
            String thinking
    ) {
        if (listener == null) return;
        String jsonToParse = JsonUtils.extractJsonFromCodeBlock(explanation);
        if (jsonToParse == null && JsonUtils.looksLikeJson(explanation)) {
            jsonToParse = explanation;
        }
        if (jsonToParse == null && rawResponse != null) {
            // Optional salvage: sometimes providers place the JSON only in raw SSE
            String maybe = JsonUtils.extractJsonFromCodeBlock(rawResponse);
            if (maybe != null) jsonToParse = maybe;
        }

        if (jsonToParse != null) {
            try {
                // If it's a tool_call from a generic provider (no continuation), just fall back to text
                try {
                    JsonObject maybe = JsonParser.parseString(jsonToParse).getAsJsonObject();
                    if (maybe.has("action") && "tool_call".equalsIgnoreCase(maybe.get("action").getAsString())) {
                        listener.onAiActionsProcessed(
                                rawResponse,
                                explanation,
                                new java.util.ArrayList<String>(),
                                new java.util.ArrayList<com.codex.apk.ChatMessage.FileActionDetail>(),
                                modelDisplayName
                        );
                        return;
                    }
                } catch (Exception ignore) {}

                QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(jsonToParse);
                if (parsed != null && parsed.isValid) {
                    if ("plan".equals(parsed.action) && parsed.planSteps != null && !parsed.planSteps.isEmpty()) {
                        List<ChatMessage.PlanStep> planSteps = QwenResponseParser.toPlanSteps(parsed);
                        listener.onAiActionsProcessed(
                                rawResponse,
                                parsed.explanation,
                                new java.util.ArrayList<String>(),
                                new java.util.ArrayList<com.codex.apk.ChatMessage.FileActionDetail>(),
                                planSteps,
                                modelDisplayName
                        );
                        return;
                    }
                    List<ChatMessage.FileActionDetail> fileActions = QwenResponseParser.toFileActionDetails(parsed);
                    listener.onAiActionsProcessed(
                            rawResponse,
                            parsed.explanation,
                            new java.util.ArrayList<String>(),
                            fileActions,
                            modelDisplayName
                    );
                    return;
                }
            } catch (Exception ignore) {}
        }
        // Fallback: plain text
        listener.onAiActionsProcessed(
                rawResponse,
                explanation,
                new java.util.ArrayList<String>(),
                new java.util.ArrayList<com.codex.apk.ChatMessage.FileActionDetail>(),
                modelDisplayName
        );
    }
}
