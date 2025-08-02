package com.codex.apk;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.ModelCapabilities;
import com.codex.apk.ai.WebSource;

/**
 * GLM API Client for Z provider integration
 * Supports GLM-4, GLM-4V, and other GLM models
 */
public class GLMApiClient implements ApiClient {
    
    private static final String TAG = "GLMApiClient";
    
    // GLM API Configuration
    private static final String GLM_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private static final String GLM_CHAT_ENDPOINT = "/chat/completions";
    private static final String GLM_MODELS_ENDPOINT = "/models";
    
    private final OkHttpClient httpClient;
    private String apiKey;
    private AIAssistant.AIActionListener actionListener;
    
    public GLMApiClient(AIAssistant.AIActionListener actionListener) {
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> chatHistory, QwenConversationState qwenState,
                            boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> tools, List<java.io.File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                JsonObject requestBody = buildChatRequest(model, message, thinkingModeEnabled, webSearchEnabled);
                
                Request request = new Request.Builder()
                        .url(GLM_BASE_URL + GLM_CHAT_ENDPOINT)
                        .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    processGLMResponse(responseBody);
                } else {
                    if (actionListener != null) actionListener.onAiError("GLM API request failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending GLM chat completion", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }
    
    private JsonObject buildChatRequest(AIModel model, String message, boolean enableThinking, boolean enableWebSearch) {
        JsonObject request = new JsonObject();
        request.addProperty("model", model.getModelId());
        request.addProperty("temperature", 0.7);
        request.addProperty("max_tokens", model.getCapabilities().maxGenerationLength);
        
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);
        request.add("messages", messages);
        
        if (enableThinking && model.getCapabilities().supportsThinking) {
            JsonObject tools = new JsonObject();
            tools.addProperty("thinking", true);
            request.add("tools", tools);
        }
        
        if (enableWebSearch && model.getCapabilities().supportsWebSearch) {
            JsonObject webSearch = new JsonObject();
            webSearch.addProperty("enable", true);
            request.add("web_search", webSearch);
        }
        
        return request;
    }
    
    private void processGLMResponse(String responseBody) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            
            if (response.has("error")) {
                JsonObject error = response.getAsJsonObject("error");
                if (actionListener != null) actionListener.onAiError("GLM API Error: " + error.get("message").getAsString());
                return;
            }
            
            if (response.has("choices")) {
                JsonArray choices = response.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    String content = message.get("content").getAsString();
                    
                    boolean isThinking = message.has("thinking") && message.get("thinking").getAsBoolean();
                    
                    List<WebSource> webSources = extractWebSources(response);
                    
                    if (actionListener != null) {
                        actionListener.onAiActionsProcessed(responseBody, content, new ArrayList<>(), new ArrayList<>(), "GLM");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing GLM response", e);
            if (actionListener != null) actionListener.onAiError("Error processing response: " + e.getMessage());
        }
    }
    
    private List<WebSource> extractWebSources(JsonObject response) {
        List<WebSource> sources = new ArrayList<>();
        
        if (response.has("web_search") && response.getAsJsonObject("web_search").has("results")) {
            JsonArray results = response.getAsJsonObject("web_search").getAsJsonArray("results");
            for (int i = 0; i < results.size(); i++) {
                JsonObject result = results.get(i).getAsJsonObject();
                sources.add(new WebSource(
                    result.get("url").getAsString(),
                    result.get("title").getAsString(),
                    result.has("snippet") ? result.get("snippet").getAsString() : "",
                    ""
                ));
            }
        }
        
        return sources;
    }

    @Override
    public List<AIModel> fetchModels() {
        // Not implemented for GLM
        return new ArrayList<>();
    }
}