package com.codex.apk;

import android.content.Context;
import android.util.Log;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QwenApiClient implements ApiClient {
    private static final String TAG = "QwenApiClient";
    private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";

    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;
    private final QwenConversationManager conversationManager;
    private final QwenMidTokenManager midTokenManager;
    private final File projectDir;

    public QwenApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.actionListener = actionListener;
        this.projectDir = projectDir;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
        this.midTokenManager = new QwenMidTokenManager(context, this.httpClient);
        this.conversationManager = new QwenConversationManager(this.httpClient, this.midTokenManager);
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                String conversationId = conversationManager.startOrContinueConversation(state, model, webSearchEnabled);
                if (conversationId == null) {
                    if (actionListener != null) actionListener.onAiError("Failed to create conversation");
                    return;
                }
                state.setConversationId(conversationId);
                performCompletion(state, model, thinkingModeEnabled, webSearchEnabled, enabledTools, message);
            } catch (IOException e) {
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void performCompletion(QwenConversationState state, AIModel model, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, String userMessage) throws IOException {
        JsonObject requestBody = QwenRequestFactory.buildCompletionRequestBody(state, model, thinkingModeEnabled, webSearchEnabled, enabledTools, userMessage);
        String qwenToken = midTokenManager.ensureMidToken(false);
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .headers(QwenRequestFactory.buildQwenHeaders(qwenToken, state.getConversationId()))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                QwenStreamProcessor.StreamProcessingResult result = new QwenStreamProcessor(actionListener, state, model, projectDir).process(response);
                if (result.isContinuation) {
                    performContinuation(state, model, result.continuationJson);
                }
            } else {
                int code = response.code();
                if (code == 401 || code == 429 || code == 403) {
                    qwenToken = midTokenManager.ensureMidToken(true);
                    Request retryReq = new Request.Builder()
                            .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
                            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                            .headers(QwenRequestFactory.buildQwenHeaders(qwenToken, state.getConversationId()))
                            .build();
                    try (Response resp2 = httpClient.newCall(retryReq).execute()) {
                        if (resp2.isSuccessful() && resp2.body() != null) {
                            QwenStreamProcessor.StreamProcessingResult result = new QwenStreamProcessor(actionListener, state, model, projectDir).process(resp2);
                            if (result.isContinuation) {
                                performContinuation(state, model, result.continuationJson);
                            }
                            return;
                        }
                    }
                }
                if (actionListener != null) actionListener.onAiError("Failed to send message (HTTP " + code + ")");
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }
    }

    private void performContinuation(QwenConversationState state, AIModel model, String toolResultJson) throws IOException {
        JsonObject requestBody = QwenRequestFactory.buildContinuationRequestBody(state, model, toolResultJson);
        String qwenToken = midTokenManager.ensureMidToken(false);
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .headers(QwenRequestFactory.buildQwenHeaders(qwenToken, state.getConversationId()))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                new QwenStreamProcessor(actionListener, state, model, projectDir).process(response);
            }
        }
    }

    @Override
    public List<AIModel> fetchModels() {
        String mid;
        try {
            mid = midTokenManager.ensureMidToken(false);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch midtoken for models", e);
            return new java.util.ArrayList<>();
        }
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/models")
                .headers(QwenRequestFactory.buildQwenHeaders(mid, null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                List<AIModel> models = new ArrayList<>();
                if (responseJson.has("data")) {
                    if (responseJson.get("data").isJsonArray()) {
                        JsonArray data = responseJson.getAsJsonArray("data");
                        for (int i = 0; i < data.size(); i++) {
                            JsonObject modelData = data.get(i).getAsJsonObject();
                            AIModel model = parseModelData(modelData);
                            if (model != null) {
                                models.add(model);
                            }
                        }
                    } else if (responseJson.get("data").isJsonObject()) {
                        JsonObject modelData = responseJson.getAsJsonObject("data");
                        AIModel model = parseModelData(modelData);
                        if (model != null) {
                            models.add(model);
                        }
                    }
                }
                return models;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching Qwen models", e);
        }
        return new java.util.ArrayList<>();
    }

    private AIModel parseModelData(JsonObject modelData) {
        try {
            String modelId = modelData.get("id").getAsString();
            String displayName = modelData.has("name") ? modelData.get("name").getAsString() : modelId;
            
            JsonObject info = modelData.getAsJsonObject("info");
            JsonObject meta = info.getAsJsonObject("meta");
            JsonObject capabilitiesJson = meta.getAsJsonObject("capabilities");

            boolean supportsThinking = capabilitiesJson.has("thinking") && capabilitiesJson.get("thinking").getAsBoolean();
            boolean supportsThinkingBudget = capabilitiesJson.has("thinking_budget") && capabilitiesJson.get("thinking_budget").getAsBoolean();
            boolean supportsVision = capabilitiesJson.has("vision") && capabilitiesJson.get("vision").getAsBoolean();
            boolean supportsDocument = capabilitiesJson.has("document") && capabilitiesJson.get("document").getAsBoolean();
            boolean supportsVideo = capabilitiesJson.has("video") && capabilitiesJson.get("video").getAsBoolean();
            boolean supportsAudio = capabilitiesJson.has("audio") && capabilitiesJson.get("audio").getAsBoolean();
            boolean supportsCitations = capabilitiesJson.has("citations") && capabilitiesJson.get("citations").getAsBoolean();

            JsonArray chatTypes = meta.has("chat_type") ? meta.get("chat_type").getAsJsonArray() : new JsonArray();
            boolean supportsWebSearch = false;
            List<String> supportedChatTypes = new ArrayList<>();
            for (int j = 0; j < chatTypes.size(); j++) {
                String chatType = chatTypes.get(j).getAsString();
                supportedChatTypes.add(chatType);
                if ("search".equals(chatType)) {
                    supportsWebSearch = true;
                }
            }

            List<String> mcpTools = new ArrayList<>();
            if (meta.has("mcp")) {
                JsonArray mcpArray = meta.get("mcp").getAsJsonArray();
                for (int j = 0; j < mcpArray.size(); j++) {
                    mcpTools.add(mcpArray.get(j).getAsString());
                }
            }
            boolean supportsMCP = !mcpTools.isEmpty();

            List<String> supportedModalities = new ArrayList<>();
            if (meta.has("modality")) {
                JsonArray modalityArray = meta.get("modality").getAsJsonArray();
                for (int j = 0; j < modalityArray.size(); j++) {
                    supportedModalities.add(modalityArray.get(j).getAsString());
                }
            }

            int maxContextLength = meta.has("max_context_length") ? meta.get("max_context_length").getAsInt() : 0;
            int maxGenerationLength = meta.has("max_generation_length") ? meta.get("max_generation_length").getAsInt() : 0;
            int maxThinkingGenerationLength = meta.has("max_thinking_generation_length") ? meta.get("max_thinking_generation_length").getAsInt() : 0;
            int maxSummaryGenerationLength = meta.has("max_summary_generation_length") ? meta.get("max_summary_generation_length").getAsInt() : 0;

            java.util.Map<String, Integer> fileLimits = new java.util.HashMap<>();
            if (meta.has("file_limits")) {
                JsonObject fileLimitsJson = meta.getAsJsonObject("file_limits");
                for (String key : fileLimitsJson.keySet()) {
                    fileLimits.put(key, fileLimitsJson.get(key).getAsInt());
                }
            }

            java.util.Map<String, Integer> abilities = new java.util.HashMap<>();
            if (meta.has("abilities")) {
                JsonObject abilitiesJson = meta.getAsJsonObject("abilities");
                for (String key : abilitiesJson.keySet()) {
                    abilities.put(key, abilitiesJson.get(key).getAsInt());
                }
            }

            boolean isSingleRound = meta.has("is_single_round") ? meta.get("is_single_round").getAsInt() == 1 : false;

            com.codex.apk.ai.ModelCapabilities capabilities = new com.codex.apk.ai.ModelCapabilities(
                supportsThinking, supportsWebSearch, supportsVision, supportsDocument,
                supportsVideo, supportsAudio, supportsCitations, supportsThinkingBudget,
                supportsMCP, isSingleRound, maxContextLength, maxGenerationLength,
                maxThinkingGenerationLength, maxSummaryGenerationLength, fileLimits,
                supportedModalities, supportedChatTypes, mcpTools, abilities
            );

            return new AIModel(modelId, displayName, com.codex.apk.ai.AIProvider.ALIBABA, capabilities);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing model data", e);
            return null;
        }
    }
}