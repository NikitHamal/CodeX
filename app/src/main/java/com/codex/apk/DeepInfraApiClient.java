package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.codex.apk.util.JsonUtils;
import okio.BufferedSource;

/**
 * DeepInfraApiClient
 * OpenAI-compatible client for DeepInfra web-embed endpoints.
 * - Chat completions: https://api.deepinfra.com/v1/openai/chat/completions
 * - Models (featured): https://api.deepinfra.com/models/featured
 */
public class DeepInfraApiClient implements StreamingApiClient {
    private static final String TAG = "DeepInfraApiClient";
    private static final String DI_CHAT = "https://api.deepinfra.com/v1/openai/chat/completions";
    private static final String DI_MODELS_FEATURED = "https://api.deepinfra.com/models/featured";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private final java.util.Map<String, SseClient> activeStreams = new java.util.HashMap<>();

    public DeepInfraApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }


    private JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history) {
        JsonArray messages = new JsonArray();
        if (history != null) {
            for (ChatMessage m : history) {
                String role = m.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant";
                String content = m.getContent() != null ? m.getContent() : "";
                if (content.isEmpty()) continue;
                JsonObject msg = new JsonObject();
                msg.addProperty("role", role);
                msg.addProperty("content", content);
                messages.add(msg);
            }
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        JsonObject root = new JsonObject();
        root.addProperty("model", modelId);
        root.add("messages", messages);
        root.addProperty("stream", true);
        return root;
    }


    @Override
    public List<AIModel> fetchModels() {
        List<AIModel> out = new ArrayList<>();
        try {
            OkHttpClient client = httpClient.newBuilder().readTimeout(30, TimeUnit.SECONDS).build();
            Request req = new Request.Builder()
                    .url(DI_MODELS_FEATURED)
                    .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                    .addHeader("accept", "*/*")
                    .build();
            try (Response r = client.newCall(req).execute()) {
                if (r.isSuccessful() && r.body() != null) {
                    String body = new String(r.body().bytes(), StandardCharsets.UTF_8);
                    try {
                        SharedPreferences sp = context.getSharedPreferences("ai_deepinfra_models", Context.MODE_PRIVATE);
                        sp.edit().putString("di_models_json", body).putLong("di_models_ts", System.currentTimeMillis()).apply();
                    } catch (Exception ignore) {}

                    try {
                        JsonElement root = JsonParser.parseString(body);
                        if (root.isJsonArray()) {
                            JsonArray arr = root.getAsJsonArray();
                            for (JsonElement e : arr) {
                                if (!e.isJsonObject()) continue;
                                JsonObject m = e.getAsJsonObject();
                                String type = m.has("type") ? m.get("type").getAsString() : "";
                                String id = m.has("model_name") ? m.get("model_name").getAsString() : null;
                                if (id == null || id.isEmpty()) continue;
                                boolean chatCapable = "text-generation".equalsIgnoreCase(type);
                                boolean vision = m.has("reported_type") && "text-to-image".equalsIgnoreCase(m.get("reported_type").getAsString());
                                ModelCapabilities caps = new ModelCapabilities(false, false, vision, true, false, false, false, 131072, 8192);
                                if (chatCapable) {
                                    out.add(new AIModel(id, toDisplayName(id), AIProvider.DEEPINFRA, caps));
                                }
                            }
                        }
                    } catch (Exception parseErr) {
                        Log.w(TAG, "DeepInfra models parse failed", parseErr);
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "DeepInfra models fetch failed", ex);
        }
        if (out.isEmpty()) {
            out.add(new AIModel("deepseek-v3", "DeepInfra DeepSeek V3", AIProvider.DEEPINFRA,
                    new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        }
        return out;
    }


    private String toDisplayName(String id) {
        String s = id.replace('-', ' ').replace('_', ' ');
        if (s.isEmpty()) return "DeepInfra";
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());
                String modelId = request.getModel() != null ? request.getModel().getModelId() : "deepseek-v3";
                JsonObject body = buildOpenAIStyleBody(modelId, request.getMessage(), request.getHistory());
                Request req = new Request.Builder()
                        .url(DI_CHAT)
                        .addHeader("accept", "text/event-stream")
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .build();

                SseClient sse = new SseClient(httpClient);
                activeStreams.put(request.getRequestId(), sse);
                final StringBuilder finalText = new StringBuilder();
                final StringBuilder rawSse = new StringBuilder();

                sse.postStream(DI_CHAT, req.headers(), body, new SseClient.Listener() {
                    @Override public void onOpen() {}
                    @Override public void onDelta(JsonObject chunk) {
                        rawSse.append("data: ").append(chunk.toString()).append('\n');
                        try {
                            if (chunk.has("choices") && chunk.getAsJsonArray("choices").size() > 0) {
                                JsonObject choice = chunk.getAsJsonArray("choices").get(0).getAsJsonObject();
                                if (choice.has("delta") && choice.get("delta").isJsonObject()) {
                                    JsonObject delta = choice.getAsJsonObject("delta");
                                    if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                        finalText.append(delta.get("content").getAsString());
                                        listener.onStreamPartialUpdate(request.getRequestId(), finalText.toString(), false);
                                    }
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                    @Override public void onUsage(JsonObject usage) {}
                    @Override public void onError(String message, int code) {
                        listener.onStreamError(request.getRequestId(), "HTTP " + code + ": " + message, null);
                    }
                    @Override public void onComplete() {
                        activeStreams.remove(request.getRequestId());
                        QwenResponseParser.ParsedResponse finalResponse = new QwenResponseParser.ParsedResponse();
                        finalResponse.action = "message";
                        finalResponse.explanation = finalText.toString();
                        finalResponse.rawResponse = rawSse.toString();
                        finalResponse.isValid = true;
                        listener.onStreamCompleted(request.getRequestId(), finalResponse);
                    }
                });

            } catch (Exception e) {
                listener.onStreamError(request.getRequestId(), "Error: " + e.getMessage(), e);
            }
        }).start();
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
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(pool)
                .build();
    }
}
