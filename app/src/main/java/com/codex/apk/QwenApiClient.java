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
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QwenApiClient implements StreamingApiClient {
    private static final String TAG = "QwenApiClient";
    private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";

    private final AIAssistant.AIActionListener actionListener;
    private OkHttpClient httpClient;
    private final QwenConversationManager conversationManager;
    private final QwenMidTokenManager midTokenManager;
    private final File projectDir;
    private final Map<String, SseClient> activeStreams = new HashMap<>();

    public QwenApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.actionListener = actionListener;
        this.projectDir = projectDir;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .cookieJar(new InMemoryCookieJar())
                .build();
        this.midTokenManager = new QwenMidTokenManager(context, this.httpClient);
        this.conversationManager = new QwenConversationManager(this.httpClient, this.midTokenManager);
    }



    // Simple in-memory cookie jar for Qwen continuity
    private static class InMemoryCookieJar implements CookieJar {
        private final java.util.List<Cookie> store = new java.util.ArrayList<>();
        @Override public synchronized void saveFromResponse(HttpUrl url, java.util.List<Cookie> cookies) {
            for (Cookie c : cookies) {
                // replace existing cookie with same name/domain/path
                store.removeIf(k -> k.name().equals(c.name()) && k.domain().equals(c.domain()) && k.path().equals(c.path()));
                store.add(c);
            }
        }
        @Override public synchronized java.util.List<Cookie> loadForRequest(HttpUrl url) {
            long now = System.currentTimeMillis();
            java.util.List<Cookie> out = new java.util.ArrayList<>();
            java.util.Iterator<Cookie> it = store.iterator();
            while (it.hasNext()) {
                Cookie c = it.next();
                if (c.expiresAt() < now) { it.remove(); continue; }
                if (c.matches(url)) out.add(c);
            }
            return out;
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

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());
                QwenConversationState state = (QwenConversationState) request.getConversationState();
                String conversationId = conversationManager.startOrContinueConversation(state, request.getModel(), request.isWebSearchEnabled());
                if (conversationId == null) {
                    listener.onStreamError(request.getRequestId(), "Failed to create conversation", null);
                    return;
                }
                state.setConversationId(conversationId);

                performStreamingCompletion(request, state, listener);

            } catch (IOException e) {
                listener.onStreamError(request.getRequestId(), "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    private void performStreamingCompletion(MessageRequest request, QwenConversationState state, StreamListener listener) throws IOException {
        JsonObject requestBody = QwenRequestFactory.buildCompletionRequestBody(state, request.getModel(), request.isThinkingModeEnabled(), request.isWebSearchEnabled(), request.getEnabledTools(), request.getMessage());
        String qwenToken = midTokenManager.ensureMidToken(false);
        okhttp3.Headers headers = QwenRequestFactory.buildQwenHeaders(qwenToken, state.getConversationId())
                .newBuilder().set("Accept", "text/event-stream").build();

        SseClient sse = new SseClient(httpClient);
        activeStreams.put(request.getRequestId(), sse);

        final StringBuilder finalText = new StringBuilder();
        final StringBuilder rawSse = new StringBuilder();
        final boolean[] retriedJsonError = new boolean[]{false};
        final boolean[] retriedHttpError = new boolean[]{false};
        final boolean[] aborted = new boolean[]{false};

        sse.postStreamWithRetry(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId(), headers, requestBody, 3, 500L, new SseClient.Listener() {
            @Override
            public void onOpen() {}

            @Override
            public void onDelta(JsonObject chunk) {
                if (aborted[0]) return;
                rawSse.append("data: ").append(chunk.toString()).append('\n');

                if (QwenStreamProcessor.isErrorChunk(chunk)) {
                    if (!retriedJsonError[0]) {
                        retriedJsonError[0] = true;
                        aborted[0] = true;
                        try { midTokenManager.ensureMidToken(true); } catch (Exception ignore) {}
                        new Thread(() -> {
                            try { performStreamingCompletion(request, state, listener); } catch (IOException ignore) {}
                        }).start();
                        return;
                    } else {
                        listener.onStreamError(request.getRequestId(), "Qwen API Error after retry", null);
                        return;
                    }
                }

                QwenStreamProcessor.processChunk(chunk, state, finalText, (partialResult, isThinking) -> {
                    listener.onStreamPartialUpdate(request.getRequestId(), partialResult, isThinking);
                }, actionListener);
            }

            @Override
            public void onUsage(JsonObject usage) {}

            @Override
            public void onError(String message, int code) {
                if ((code == 401 || code == 403 || code == 429) && !retriedHttpError[0]) {
                    retriedHttpError[0] = true;
                    aborted[0] = true;
                    try { midTokenManager.ensureMidToken(true); } catch (Exception ignore) {}
                    new Thread(() -> {
                        try { performStreamingCompletion(request, state, listener); } catch (IOException ignore) {}
                    }).start();
                    return;
                }
                listener.onStreamError(request.getRequestId(), "HTTP " + code + ": " + message, null);
            }

            @Override
            public void onComplete() {
                activeStreams.remove(request.getRequestId());
                String completedText = finalText.toString();

                if (completedText.trim().isEmpty()) {
                    new Thread(() -> {
                        try {
                            String fallbackText = performNonStreamingCompletion(request, state);
                            processFinalText(fallbackText != null ? fallbackText : "", rawSse.toString(), listener, request, state);
                        } catch (IOException e) {
                            listener.onStreamError(request.getRequestId(), "Non-streaming fallback failed", e);
                        }
                    }).start();
                } else {
                    processFinalText(completedText, rawSse.toString(), listener, request, state);
                }
            }
        });
    }

    private void processFinalText(String completedText, String rawSse, StreamListener listener, MessageRequest request, QwenConversationState state) {
        String jsonToParse = com.codex.apk.util.JsonUtils.extractJsonFromCodeBlock(completedText);
        if (jsonToParse == null && com.codex.apk.util.JsonUtils.looksLikeJson(completedText)) {
            jsonToParse = completedText;
        }

        if (jsonToParse != null) {
            try {
                JsonObject maybe = JsonParser.parseString(jsonToParse).getAsJsonObject();
                if (maybe.has("action") && "tool_call".equalsIgnoreCase(maybe.get("action").getAsString()) && maybe.has("tool_calls")) {
                    new Thread(() -> {
                        performToolContinuation(maybe.getAsJsonArray("tool_calls"), request, state, listener);
                    }).start();
                    return;
                }
            } catch (Exception ignore) {}
        }

        QwenResponseParser.parseResponseAsync(completedText, rawSse, new QwenResponseParser.ParseResultListener() {
            @Override
            public void onParseSuccess(QwenResponseParser.ParsedResponse parsedResponse) {
                listener.onStreamCompleted(request.getRequestId(), parsedResponse);
            }

            @Override
            public void onParseFailed() {
                QwenResponseParser.ParsedResponse fallback = new QwenResponseParser.ParsedResponse();
                fallback.action = "message";
                fallback.explanation = completedText;
                fallback.rawResponse = rawSse;
                fallback.isValid = true;
                listener.onStreamCompleted(request.getRequestId(), fallback);
            }
        });
    }

    private void performToolContinuation(JsonArray toolCalls, MessageRequest originalRequest, QwenConversationState state, StreamListener listener) {
        ParallelToolExecutor executor = new ParallelToolExecutor(projectDir);
        List<ChatMessage.ToolUsage> toolUsages = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            try {
                JsonObject call = toolCalls.get(i).getAsJsonObject();
                String toolName = call.get("name").getAsString();
                JsonObject args = call.has("args") && call.get("args").isJsonObject() ? call.getAsJsonObject("args") : new JsonObject();
                ChatMessage.ToolUsage usage = new ChatMessage.ToolUsage(toolName);
                usage.argsJson = args.toString();
                toolUsages.add(usage);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse tool call from JSON", e);
            }
        }

        executor.executeTools(toolUsages).thenAccept(results -> {
            try {
                JsonArray jsonResults = new JsonArray();
                for (ParallelToolExecutor.ToolResult result : results) {
                    JsonObject res = new JsonObject();
                    res.addProperty("toolName", result.toolName);
                    res.add("result", result.result);
                    jsonResults.add(res);
                }

                String continuation = ToolExecutor.buildToolResultContinuation(jsonResults);
                JsonObject requestBody = QwenRequestFactory.buildContinuationRequestBody(state, originalRequest.getModel(), continuation);

                // Continue with the streaming logic here...
                continueStreamingWithToolResults(requestBody, originalRequest, state, listener);

            } catch (Exception e) {
                listener.onStreamError(originalRequest.getRequestId(), "Failed to process tool results", e);
            }
        });
    }

    private void continueStreamingWithToolResults(JsonObject requestBody, MessageRequest originalRequest, QwenConversationState state, StreamListener listener) throws IOException {
        String qwenToken = midTokenManager.ensureMidToken(false);
        okhttp3.Headers headers = QwenRequestFactory.buildQwenHeaders(qwenToken, state.getConversationId())
                .newBuilder().add("Accept", "text/event-stream").build();

        SseClient sse = new SseClient(httpClient);
        activeStreams.put(originalRequest.getRequestId(), sse);

        final StringBuilder finalText = new StringBuilder();
        final StringBuilder rawSse = new StringBuilder();

        sse.postStreamWithRetry(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId(), headers, requestBody, 3, 500L, new SseClient.Listener() {
            @Override public void onOpen() {}
            @Override public void onDelta(JsonObject chunk) {
                rawSse.append("data: ").append(chunk.toString()).append('\n');
                QwenStreamProcessor.processChunk(chunk, state, finalText, (partialResult, isThinking) -> {
                    listener.onStreamPartialUpdate(originalRequest.getRequestId(), partialResult, isThinking);
                }, actionListener);
            }
            @Override public void onUsage(JsonObject usage) {}
            @Override public void onError(String message, int code) {
                listener.onStreamError(originalRequest.getRequestId(), "HTTP " + code + ": " + message, null);
            }
            @Override public void onComplete() {
                activeStreams.remove(originalRequest.getRequestId());
                processFinalText(finalText.toString(), rawSse.toString(), listener, originalRequest, state);
            }
        });
    }

    private String performNonStreamingCompletion(MessageRequest request, QwenConversationState state) throws IOException {
        JsonObject body = QwenRequestFactory.buildCompletionRequestBody(state, request.getModel(), request.isThinkingModeEnabled(), request.isWebSearchEnabled(), request.getEnabledTools(), request.getMessage());
        body.addProperty("stream", false);
        body.addProperty("incremental_output", false);

        String qwenToken = midTokenManager.ensureMidToken(false);
        okhttp3.Headers headers = QwenRequestFactory.buildQwenHeaders(qwenToken, state.getConversationId())
                .newBuilder()
                .set("Accept", "application/json")
                .build();

        Request req = new Request.Builder()
                .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
                .headers(headers)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                return null;
            }
            String text = resp.body().string();
            try {
                JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
                if (obj.has("choices") && obj.getAsJsonArray("choices").size() > 0) {
                    JsonObject choice = obj.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("message") && choice.get("message").isJsonObject()) {
                        JsonObject msg = choice.getAsJsonObject("message");
                        if (msg.has("content") && !msg.get("content").isJsonNull()) {
                            return msg.get("content").getAsString();
                        }
                    }
                }
                return text;
            } catch (Exception ignore) {
                return text;
            }
        }
    }


    @Override
    public void cancelStreaming(String requestId) {
        if (activeStreams.containsKey(requestId)) {
            SseClient sse = activeStreams.get(requestId);
            if (sse != null) {
                sse.cancel();
            }
            activeStreams.remove(requestId);
        }
    }

    @Override
    public void setConnectionPool(okhttp3.ConnectionPool pool) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .cookieJar(new InMemoryCookieJar())
                .connectionPool(pool)
                .build();
    }
}