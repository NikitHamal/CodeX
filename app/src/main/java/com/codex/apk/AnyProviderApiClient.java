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
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import com.codex.apk.util.JsonUtils;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * AnyProviderApiClient
 * Generic client for free endpoints (starting with Pollinations text API).
 * - Streams OpenAI-style SSE deltas from https://text.pollinations.ai/openai
 * - No API key required
 * - Gracefully maps unknown FREE models to a default provider model
 */
public class AnyProviderApiClient implements StreamingApiClient {

    private static final String TAG = "AnyProviderApiClient";
    private static final String OPENAI_ENDPOINT = "https://text.pollinations.ai/openai";

    private final Context context;
    protected final AIAssistant.AIActionListener actionListener;
    protected OkHttpClient httpClient;
    protected final Gson gson = new Gson();
    protected final Random random = new Random();
    protected final java.util.Map<String, SseClient> activeStreams = new java.util.HashMap<>();

    public AnyProviderApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // stream
                .build();
    }


    protected JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history, boolean thinkingModeEnabled) {
        JsonArray messages = new JsonArray();
        // Convert existing history (keep it light)
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
        // Append the new user message (AIAssistant has already prepended system prompt when needed)
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        JsonObject root = new JsonObject();
        root.addProperty("model", modelId);
        root.add("messages", messages);
        root.addProperty("stream", true);
        // Pollinations supports seed and referrer; seed helps cache-busting and diversity
        root.addProperty("seed", random.nextInt(Integer.MAX_VALUE));
        root.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
        // If provider supports reasoning visibility, prefer default (no explicit toggle); keep payload minimal
        if (thinkingModeEnabled) {
            // Some providers accept x-show-reasoning header; we rely on minimal body here
        }
        return root;
    }


    protected String mapToProviderModel(String modelId) {
        if (modelId == null || modelId.isEmpty()) return "openai"; // sensible default
        String lower = modelId.toLowerCase(Locale.ROOT);
        // Pollinations exposes many backends by name; pass through most names.
        return lower;
    }

    @Override
    public List<AIModel> fetchModels() {
        return new ArrayList<>();
    }

    protected static String toDisplay(String id) {
        if (id == null || id.isEmpty()) return "Unnamed Model";
        String s = id.replace('-', ' ').trim();
        if (s.isEmpty()) return id;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());
                String providerModel = mapToProviderModel(request.getModel() != null ? request.getModel().getModelId() : null);
                JsonObject body = buildOpenAIStyleBody(providerModel, request.getMessage(), request.getHistory(), request.isThinkingModeEnabled());

                Request req = new Request.Builder()
                        .url(OPENAI_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "text/event-stream")
                        .build();

                SseClient sse = new SseClient(httpClient);
                activeStreams.put(request.getRequestId(), sse);
                final StringBuilder finalText = new StringBuilder();
                final StringBuilder rawSse = new StringBuilder();

                sse.postStream(OPENAI_ENDPOINT, req.headers(), body, new SseClient.Listener() {
                    @Override public void onOpen() {}
                    @Override public void onDelta(JsonObject chunk) {
                        rawSse.append("data: ").append(chunk.toString()).append('\n');
                        try {
                            if (chunk.has("choices")) {
                                JsonArray choices = chunk.getAsJsonArray("choices");
                                for (int i = 0; i < choices.size(); i++) {
                                    JsonObject c = choices.get(i).getAsJsonObject();
                                    if (c.has("delta") && c.get("delta").isJsonObject()) {
                                        JsonObject delta = c.getAsJsonObject("delta");
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            finalText.append(delta.get("content").getAsString());
                                            listener.onStreamPartialUpdate(request.getRequestId(), finalText.toString(), false);
                                        }
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .connectionPool(pool)
                .build();
    }
}
