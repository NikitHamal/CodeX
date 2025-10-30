package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;
import com.codex.apk.ai.ModelCapabilities;
import com.codex.apk.ai.ModelCapabilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import com.codex.apk.util.JsonUtils;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import okhttp3.ConnectionPool;

public class OpenRouterApiClient implements StreamingApiClient {
    private static final String TAG = "OpenRouterApiClient";
    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private OkHttpClient http;
    private final java.util.Map<String, SseClient> activeStreams = new java.util.HashMap<>();

    public OpenRouterApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }


    @Override
    public List<AIModel> fetchModels() {
        String apiKey = SettingsActivity.getOpenRouterApiKey(context);
        if (apiKey == null || apiKey.isEmpty()) {
            return new ArrayList<>();
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/models")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray data = jsonResponse.getAsJsonArray("data");

            List<AIModel> models = new ArrayList<>();
            for (JsonElement element : data) {
                JsonObject modelObject = element.getAsJsonObject();
                if (modelObject.has("pricing")) {
                    JsonObject pricing = modelObject.getAsJsonObject("pricing");
                    String promptPrice = pricing.has("prompt") ? pricing.get("prompt").getAsString() : "1";
                    String completionPrice = pricing.has("completion") ? pricing.get("completion").getAsString() : "1";
                    if ("0".equals(promptPrice) || "0.0".equals(promptPrice) && "0".equals(completionPrice) || "0.0".equals(completionPrice)) {
                        String modelId = modelObject.get("id").getAsString();
                        String displayName = modelObject.has("name") ? modelObject.get("name").getAsString() : modelId;
                        models.add(new AIModel(modelId, displayName, AIProvider.OPENROUTER, new ModelCapabilities(true, false, false, true, false, false, false, 0, 0)));
                    }
                }
            }

            if (!models.isEmpty()) {
                AIModel.updateModelsForProvider(AIProvider.OPENROUTER, models);
            }

            return models;
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch models from OpenRouter", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());
                String apiKey = SettingsActivity.getOpenRouterApiKey(context);
                if (apiKey == null || apiKey.isEmpty()) {
                    listener.onStreamError(request.getRequestId(), "OpenRouter API key is missing", null);
                    return;
                }
                String modelId = request.getModel() != null ? request.getModel().getModelId() : "openai/gpt-3.5-turbo";

                JsonObject reqBody = new JsonObject();
                reqBody.addProperty("model", modelId);
                JsonArray messages = new JsonArray();
                for (ChatMessage msg : request.getHistory()) {
                    JsonObject messageObject = new JsonObject();
                    messageObject.addProperty("role", msg.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant");
                    messageObject.addProperty("content", msg.getContent());
                    messages.add(messageObject);
                }
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", request.getMessage());
                messages.add(userMessage);
                reqBody.add("messages", messages);

                Request httpReq = new Request.Builder()
                        .url(BASE_URL + "/chat/completions")
                        .post(RequestBody.create(reqBody.toString(), JSON))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Accept", "text/event-stream")
                        .build();

                SseClient sse = new SseClient(http);
                activeStreams.put(request.getRequestId(), sse);
                final StringBuilder finalText = new StringBuilder();
                final StringBuilder rawSse = new StringBuilder();

                sse.postStream(BASE_URL + "/chat/completions", httpReq.headers(), reqBody, new SseClient.Listener() {
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
    public void setConnectionPool(ConnectionPool pool) {
        this.http = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .connectionPool(pool)
                .build();
    }
}
