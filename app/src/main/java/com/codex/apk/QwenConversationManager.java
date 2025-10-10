package com.codex.apk;

import com.codex.apk.ai.AIModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QwenConversationManager {
    private static final String TAG = "QwenConversationManager";
    private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";

    private final OkHttpClient httpClient;
    private final QwenMidTokenManager midTokenManager;

    public QwenConversationManager(OkHttpClient httpClient, QwenMidTokenManager midTokenManager) {
        this.httpClient = httpClient;
        this.midTokenManager = midTokenManager;
    }

    public String startOrContinueConversation(QwenConversationState state, AIModel model, boolean webSearchEnabled) throws IOException {
        if (state != null && state.getConversationId() != null) {
            return state.getConversationId();
        }
        return createQwenConversation(model, webSearchEnabled);
    }

    private String createQwenConversation(AIModel model, boolean webSearchEnabled) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("title", "New Chat");
        requestBody.add("models", new com.google.gson.JsonArray());
        requestBody.getAsJsonArray("models").add(model.getModelId());
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("chat_type", webSearchEnabled ? "search" : "t2t");
        requestBody.addProperty("timestamp", System.currentTimeMillis());

        String qwenToken = midTokenManager.ensureMidToken(false);
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/chats/new")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .headers(QwenRequestFactory.buildQwenHeaders(qwenToken, null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                if (responseJson.get("success").getAsBoolean()) {
                    return responseJson.getAsJsonObject("data").get("id").getAsString();
                }
            } else {
                int code = response.code();
                if (code == 401 || code == 429) {
                    qwenToken = midTokenManager.ensureMidToken(true);
                    Request retry = new Request.Builder()
                            .url(QWEN_BASE_URL + "/chats/new")
                            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                            .headers(QwenRequestFactory.buildQwenHeaders(qwenToken, null))
                            .build();
                    try (Response resp2 = httpClient.newCall(retry).execute()) {
                        if (resp2.isSuccessful() && resp2.body() != null) {
                            String responseBody2 = resp2.body().string();
                            JsonObject responseJson2 = JsonParser.parseString(responseBody2).getAsJsonObject();
                            if (responseJson2.get("success").getAsBoolean()) {
                                return responseJson2.getAsJsonObject("data").get("id").getAsString();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}