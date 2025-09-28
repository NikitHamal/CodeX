package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
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
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenRouterApiClient implements ApiClient {
    private static final String TAG = "OpenRouterApiClient";
    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient http;

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
    public void sendMessage(String message,
                            AIModel model,
                            List<ChatMessage> history,
                            QwenConversationState state,
                            boolean thinkingModeEnabled,
                            boolean webSearchEnabled,
                            List<ToolSpec> enabledTools,
                            List<File> attachments) {
        new Thread(() -> {
            if (actionListener != null) actionListener.onAiRequestStarted();
            try {
                String apiKey = SettingsActivity.getOpenRouterApiKey(context);
                if (apiKey == null || apiKey.isEmpty()) {
                    if (actionListener != null) actionListener.onAiError("OpenRouter API key is missing. Set it in Settings.");
                    return;
                }

                String modelId = model != null ? model.getModelId() : "openai/gpt-3.5-turbo";
                String url = BASE_URL + "/chat/completions";

                JsonObject req = new JsonObject();
                req.addProperty("model", modelId);
                JsonArray messages = new JsonArray();
                for (ChatMessage msg : history) {
                    JsonObject messageObject = new JsonObject();
                    messageObject.addProperty("role", msg.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant");
                    messageObject.addProperty("content", msg.getContent());
                    messages.add(messageObject);
                }
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", message);
                messages.add(userMessage);

                req.add("messages", messages);

                Request httpReq = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(req.toString(), JSON))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();

                try (Response resp = http.newCall(httpReq).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        String err = resp.body() != null ? resp.body().string() : null;
                        if (actionListener != null) actionListener.onAiError("OpenRouter API error: " + resp.code() + (err != null ? ": " + err : ""));
                        return;
                    }
                    String body = resp.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(body).getAsJsonObject();
                    String content = "";
                    if (jsonResponse.has("choices")) {
                        JsonArray choices = jsonResponse.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            if (choice.has("message")) {
                                JsonObject messageObj = choice.getAsJsonObject("message");
                                if (messageObj.has("content")) {
                                    content = messageObj.get("content").getAsString();
                                }
                            }
                        }
                    }

                    if (actionListener != null) {
                        actionListener.onAiActionsProcessed(content, content, new ArrayList<>(), new ArrayList<>(), model.getDisplayName());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling OpenRouter API", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
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
}
