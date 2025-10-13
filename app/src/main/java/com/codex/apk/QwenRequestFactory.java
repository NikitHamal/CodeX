package com.codex.apk;

import com.codex.apk.ai.AIModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public class QwenRequestFactory {
    private static final String QWEN_BX_V = "2.5.31";

    public static okhttp3.Headers buildQwenHeaders(String midtoken, String conversationId) {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder()
                .add("Authorization", "Bearer")
                .add("Content-Type", "application/json")
                .add("Accept", "*/*")
                .add("bx-umidtoken", midtoken)
                .add("bx-v", QWEN_BX_V)
                .add("Accept-Language", "en-US,en;q=0.9")
                .add("Connection", "keep-alive")
                .add("Origin", "https://chat.qwen.ai")
                .add("Sec-Fetch-Dest", "empty")
                .add("Sec-Fetch-Mode", "cors")
                .add("Sec-Fetch-Site", "same-origin")
                .add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .add("Source", "web");

        if (conversationId != null) {
            builder.add("Referer", "https://chat.qwen.ai/c/" + conversationId);
        } else {
            builder.add("Referer", "https://chat.qwen.ai/");
        }

        return builder.build();
    }

    public static JsonObject buildCompletionRequestBody(QwenConversationState state, AIModel model, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, String userMessage) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("stream", true);
        requestBody.addProperty("incremental_output", true);
        requestBody.addProperty("chat_id", state.getConversationId());
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("model", model.getModelId());
        requestBody.addProperty("parent_id", state.getLastParentId());
        requestBody.addProperty("timestamp", System.currentTimeMillis());

        JsonArray messages = new JsonArray();
        if (state.getLastParentId() == null) {
            messages.add(createSystemMessage(enabledTools));
        }

        JsonObject userMsg = createUserMessage(userMessage, model, thinkingModeEnabled, webSearchEnabled);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        if (enabledTools != null && !enabledTools.isEmpty()) {
            requestBody.add("tools", ToolSpec.toJsonArray(enabledTools));
            JsonObject toolChoice = new JsonObject();
            toolChoice.addProperty("type", "auto");
            requestBody.add("tool_choice", toolChoice);
        }

        return requestBody;
    }

    public static JsonObject buildContinuationRequestBody(QwenConversationState state, AIModel model, String toolResultJson) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("stream", true);
        requestBody.addProperty("incremental_output", true);
        requestBody.addProperty("chat_id", state.getConversationId());
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("model", model.getModelId());
        requestBody.addProperty("parent_id", state.getLastParentId());
        requestBody.addProperty("timestamp", System.currentTimeMillis());

        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", "```json\n" + toolResultJson + "\n```\n");
        msg.addProperty("user_action", "chat");
        msg.add("files", new JsonArray());
        msg.addProperty("timestamp", System.currentTimeMillis());
        JsonArray modelsArray = new JsonArray();
        modelsArray.add(model.getModelId());
        msg.add("models", modelsArray);
        msg.addProperty("chat_type", "t2t");
        JsonObject featureConfig = new JsonObject();
        featureConfig.addProperty("thinking_enabled", false);
        featureConfig.addProperty("output_schema", "phase");
        msg.add("feature_config", featureConfig);
        msg.addProperty("fid", java.util.UUID.randomUUID().toString());
        msg.add("childrenIds", new JsonArray());
        messages.add(msg);
        requestBody.add("messages", messages);

        return requestBody;
    }

    private static JsonObject createSystemMessage(List<ToolSpec> enabledTools) {
        return PromptManager.createSystemMessage(enabledTools);
    }

    private static JsonObject createUserMessage(String message, AIModel model, boolean thinkingModeEnabled, boolean webSearchEnabled) {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.addProperty("content", message);
        messageObj.addProperty("user_action", "chat");
        messageObj.add("files", new JsonArray());
        messageObj.addProperty("timestamp", System.currentTimeMillis());
        JsonArray modelsArray = new JsonArray();
        modelsArray.add(model.getModelId());
        messageObj.add("models", modelsArray);
        String chatType = webSearchEnabled ? "search" : "t2t";
        messageObj.addProperty("chat_type", chatType);
        JsonObject featureConfig = new JsonObject();
        featureConfig.addProperty("thinking_enabled", thinkingModeEnabled);
        featureConfig.addProperty("output_schema", "phase");
        if (webSearchEnabled) {
            featureConfig.addProperty("search_version", "v2");
        }
        if (thinkingModeEnabled) {
            int defaultBudget = 81920;
            int modelBudget = 0;
            try {
                com.codex.apk.ai.ModelCapabilities caps = model.getCapabilities();
                if (caps != null && caps.maxThinkingGenerationLength > 0) {
                    modelBudget = caps.maxThinkingGenerationLength;
                }
            } catch (Exception ignore) {}
            featureConfig.addProperty("thinking_budget", modelBudget > 0 ? modelBudget : defaultBudget);
        }
        messageObj.add("feature_config", featureConfig);
        messageObj.addProperty("fid", java.util.UUID.randomUUID().toString());
        messageObj.add("parentId", null);
        messageObj.add("childrenIds", new JsonArray());

        // Extra metadata to align with latest Qwen web payloads
        JsonObject extra = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("subChatType", chatType);
        extra.add("meta", meta);
        messageObj.add("extra", extra);
        messageObj.addProperty("sub_chat_type", chatType);
        return messageObj;
    }
}