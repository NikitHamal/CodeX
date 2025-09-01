package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;

public class ZhipuApiClient implements ApiClient {
    private static final String TAG = "ZhipuApiClient";
    private static final String ZHIPU_BASE_URL = "https://chat.z.ai/api";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final AIAssistant.AIActionListener actionListener;

    private String apiKey;
    private List<AIModel> models;
    private Map<String, String> modelAliases;

    public ZhipuApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * NOTE: This authentication is based on reverse-engineering the Zhipu web client.
     * It is not an official API and may be unstable or subject to rate limiting.
     * This implementation follows the user's request for "unofficial free methods".
     */
    private void ensureAuthAndModels() throws IOException {
        if (apiKey != null && models != null && !models.isEmpty()) {
            return;
        }

        Request authRequest = new Request.Builder()
                .url(ZHIPU_BASE_URL + "/v1/auths/")
                .get()
                .build();

        try (Response authResponse = httpClient.newCall(authRequest).execute()) {
            if (!authResponse.isSuccessful()) {
                throw new IOException("Failed to get Zhipu API key: " + authResponse);
            }
            String responseBody = authResponse.body().string();
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            this.apiKey = responseJson.get("token").getAsString();
        }

        Request modelsRequest = new Request.Builder()
                .url(ZHIPU_BASE_URL + "/models")
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .get()
                .build();

        try (Response modelsResponse = httpClient.newCall(modelsRequest).execute()) {
            if (!modelsResponse.isSuccessful()) {
                throw new IOException("Failed to fetch Zhipu models: " + modelsResponse);
            }
            String modelsBody = modelsResponse.body().string();
            JsonObject modelsJson = JsonParser.parseString(modelsBody).getAsJsonObject();
            JsonArray data = modelsJson.getAsJsonArray("data");

            this.models = new ArrayList<>();
            this.modelAliases = new java.util.HashMap<>();

            for (int i = 0; i < data.size(); i++) {
                JsonObject modelData = data.get(i).getAsJsonObject();
                String modelId = modelData.get("id").getAsString();
                String modelName = modelData.get("name").getAsString();
                this.modelAliases.put(modelName, modelId);

                ModelCapabilities capabilities = new ModelCapabilities(true, false, false, false, false, false, false, false, false, false, 0, 0, 0, 0, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
                this.models.add(new AIModel(modelId, modelName, AIProvider.ZHIPU, capabilities));
            }
        }
    }

    @Override
    public List<AIModel> fetchModels() {
        try {
            ensureAuthAndModels();
        } catch (IOException e) {
            Log.e(TAG, "Error fetching Zhipu models", e);
            return Collections.emptyList();
        }
        return this.models;
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                ensureAuthAndModels();

                performCompletion(message, model, history);

            } catch (IOException e) {
                Log.e(TAG, "Error sending Zhipu message", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            }
        }).start();
    }

    private void performCompletion(String userMessage, AIModel model, List<ChatMessage> history) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("chat_id", "local");
        requestBody.addProperty("id", UUID.randomUUID().toString());
        requestBody.addProperty("stream", true);
        requestBody.addProperty("model", model.getModelId());

        JsonArray messages = new JsonArray();
        for (ChatMessage chatMessage : history) {
            messages.add(chatMessage.toJsonObject());
        }
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        requestBody.add("params", new JsonObject());
        requestBody.add("tool_servers", new JsonArray());
        JsonObject features = new JsonObject();
        features.addProperty("enable_thinking", true);
        requestBody.add("features", features);

        Request request = new Request.Builder()
                .url(ZHIPU_BASE_URL + "/chat/completions")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("x-fe-version", "prod-fe-1.0.57")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                processZhipuStreamResponse(response);
            } else {
                if (actionListener != null) actionListener.onAiError("Failed to send message: " + response.message());
            }
        }
    }

    private void processZhipuStreamResponse(Response response) throws IOException {
        StringBuilder answerContent = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder();
        StringBuilder rawResponse = new StringBuilder();

        String line;
        while ((line = response.body().source().readUtf8Line()) != null) {
            rawResponse.append(line).append("\n");
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (data.isEmpty()) continue;
                Log.d(TAG, "Zhipu SSE chunk: " + data);
                try {
                    JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                    if (json.has("type") && "chat:completion".equals(json.get("type").getAsString())) {
                        JsonObject dataObj = json.getAsJsonObject("data");
                        String phase = dataObj.has("phase") ? dataObj.get("phase").getAsString() : "";

                        if ("thinking".equals(phase)) {
                            if (dataObj.has("delta_content")) {
                                String delta = dataObj.get("delta_content").getAsString();
                                thinkingContent.append(extractContentFromHtml(delta));
                                if (actionListener != null) {
                                    actionListener.onAiStreamUpdate(thinkingContent.toString(), true);
                                }
                            }
                        } else {
                            if (dataObj.has("edit_content")) {
                                String editContent = dataObj.get("edit_content").getAsString();
                                answerContent.append(extractContentFromHtml(editContent));
                            } else if (dataObj.has("delta_content")) {
                                answerContent.append(dataObj.get("delta_content").getAsString());
                            }
                            if (actionListener != null) {
                                actionListener.onAiStreamUpdate(answerContent.toString(), false);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing stream data chunk: " + data, e);
                }
            }
        }
        if (actionListener != null) {
            actionListener.onAiActionsProcessed(rawResponse.toString(), answerContent.toString(), new ArrayList<>(), new ArrayList<>(), "Zhipu");
        }
    }

    private String extractContentFromHtml(String html) {
        if (html == null) return "";
        // This regex will remove all HTML tags.
        return html.replaceAll("<[^>]*>", "").trim();
    }
}
