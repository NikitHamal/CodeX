package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.ModelCapabilities;
import com.codex.apk.ai.AIProvider;

import java.util.Collections;

public class KimiApiClient implements ApiClient {
    private static final String TAG = "KimiApiClient";
    private static final String KIMI_BASE_URL = "https://www.kimi.com/api";

    private final OkHttpClient httpClient;
    private final AIAssistant.AIActionListener actionListener;
    private String accessToken;
    private String deviceId;

    public KimiApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.deviceId = String.valueOf(new Random().nextLong());
    }

    /**
     * NOTE: This authentication is based on reverse-engineering the Kimi web client.
     * It is not an official API and may be unstable or subject to rate limiting.
     * This implementation follows the user's request for "unofficial free methods".
     */
    private void ensureAuthenticated() throws IOException {
        if (accessToken != null) {
            return;
        }

        Request request = new Request.Builder()
                .url(KIMI_BASE_URL + "/device/register")
                .post(RequestBody.create("{}", MediaType.parse("application/json")))
                .addHeader("x-msh-device-id", deviceId)
                .addHeader("x-msh-platform", "web")
                .addHeader("x-traffic-id", deviceId)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to register device: " + response);
            }
            String responseBody = response.body().string();
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            if (responseJson.has("access_token")) {
                this.accessToken = responseJson.get("access_token").getAsString();
            } else {
                throw new IOException("No access token received");
            }
        }
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                ensureAuthenticated();

                // Always create a new conversation for Kimi to avoid "chat not found" errors.
                String conversationId = createKimiConversation();

                if (conversationId == null) {
                    if (actionListener != null) actionListener.onAiError("Failed to create conversation");
                    return;
                }

                performCompletion(conversationId, message, history, webSearchEnabled);

            } catch (IOException e) {
                Log.e(TAG, "Error sending Kimi message", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            }
        }).start();
    }

    private String createKimiConversation() throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", "Untitled Conversation");
        requestBody.addProperty("born_from", "home");
        requestBody.addProperty("kimiplus_id", "kimi");
        requestBody.addProperty("is_example", false);
        requestBody.addProperty("source", "web");
        requestBody.add("tags", new JsonArray());

        Request request = new Request.Builder()
                .url(KIMI_BASE_URL + "/chat")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (responseBody.contains("匿名聊天使用次数超过")) {
                    throw new IOException("Anonymous chat usage limit exceeded");
                }
                throw new IOException("Failed to create Kimi conversation: " + response.code() + " " + responseBody);
            }
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            return responseJson.get("id").getAsString();
        }
    }

    private void performCompletion(String conversationId, String userMessage, List<ChatMessage> history, boolean webSearchEnabled) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("kimiplus_id", "kimi");
        JsonObject extend = new JsonObject();
        extend.addProperty("sidebar", true);
        requestBody.add("extend", extend);
        requestBody.addProperty("model", "k2");
        requestBody.addProperty("use_search", webSearchEnabled);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        requestBody.add("refs", new JsonArray());
        requestBody.add("history", new JsonArray());
        requestBody.add("scene_labels", new JsonArray());
        requestBody.addProperty("use_semantic_memory", false);
        requestBody.addProperty("use_deep_research", false);

        Request request = new Request.Builder()
                .url(KIMI_BASE_URL + "/chat/" + conversationId + "/completion/stream")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                processKimiStreamResponse(response);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e(TAG, "Kimi API error: " + response.code() + " " + errorBody);
                if (actionListener != null) actionListener.onAiError("Kimi API Error: " + response.code() + "\n" + errorBody);
            }
        }
    }

    private void processKimiStreamResponse(Response response) throws IOException {
        StringBuilder answerContent = new StringBuilder();
        StringBuilder rawResponse = new StringBuilder();
        String line;
        while ((line = response.body().source().readUtf8Line()) != null) {
            rawResponse.append(line).append("\n");
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                try {
                    JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                    if (json.has("event")) {
                        String event = json.get("event").getAsString();
                        if (event.equals("cmpl")) {
                            String text = json.get("text").getAsString();
                            answerContent.append(text);
                            if (actionListener != null) {
                                actionListener.onAiStreamUpdate(answerContent.toString(), false);
                            }
                        } else if (event.equals("rename")) {
                            // Title generation, could be handled if needed
                        } else if (event.equals("all_done")) {
                            if (actionListener != null) {
                                actionListener.onAiActionsProcessed(rawResponse.toString(), answerContent.toString(), new ArrayList<>(), new ArrayList<>(), "Kimi");
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing stream data chunk", e);
                }
            }
        }
    }

    @Override
    public List<AIModel> fetchModels() {
        List<AIModel> models = new ArrayList<>();
        ModelCapabilities capabilities = new ModelCapabilities(false, true, false, false, false, false, false, false, false, false, 0, 0, 0, 0, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        models.add(new AIModel("k2", "Kimi K2", AIProvider.KIMI, capabilities));
        return models;
    }
}
