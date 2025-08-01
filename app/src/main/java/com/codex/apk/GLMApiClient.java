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

/**
 * GLM API Client for Z provider integration
 * Supports GLM-4, GLM-4V, and other GLM models
 */
public class GLMApiClient {
    
    private static final String TAG = "GLMApiClient";
    
    // GLM API Configuration
    private static final String GLM_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private static final String GLM_CHAT_ENDPOINT = "/chat/completions";
    private static final String GLM_MODELS_ENDPOINT = "/models";
    
    private final OkHttpClient httpClient;
    private String apiKey;
    
    // GLM Model definitions
    public enum GLMModel {
        GLM_4_PLUS("glm-4-plus", "GLM-4-Plus", true, false, true, true, false, false, true, 128000, 4096),
        GLM_4_0520("glm-4-0520", "GLM-4-0520", true, false, true, true, false, false, true, 128000, 4096),
        GLM_4_LONG("glm-4-long", "GLM-4-Long", false, false, false, true, false, false, false, 1000000, 4096),
        GLM_4_AIRX("glm-4-airx", "GLM-4-AirX", false, false, true, true, false, false, true, 128000, 4096),
        GLM_4_AIR("glm-4-air", "GLM-4-Air", false, false, true, true, false, false, true, 128000, 4096),
        GLM_4_FLASH("glm-4-flash", "GLM-4-Flash", false, false, true, true, false, false, true, 128000, 4096),
        GLM_4V_PLUS("glm-4v-plus", "GLM-4V-Plus", true, false, true, true, true, false, true, 128000, 4096),
        GLM_4V("glm-4v", "GLM-4V", false, false, true, true, true, false, true, 128000, 4096),
        COGVIEW_3_PLUS("cogview-3-plus", "CogView-3-Plus", false, false, false, false, false, false, false, 0, 0),
        COGVIDEOX("cogvideox", "CogVideoX", false, false, false, false, false, true, false, 0, 0),
        GLM_4_ALLTOOLS("glm-4-alltools", "GLM-4-AllTools", false, false, true, true, false, false, true, 128000, 4096);
        
        private final String modelId;
        private final String displayName;
        private final boolean supportsThinking;
        private final boolean supportsWebSearch;
        private final boolean supportsVision;
        private final boolean supportsDocument;
        private final boolean supportsVideo;
        private final boolean supportsAudio;
        private final boolean supportsCitations;
        private final int maxContextLength;
        private final int maxGenerationLength;
        
        GLMModel(String modelId, String displayName, boolean supportsThinking, boolean supportsWebSearch,
                boolean supportsVision, boolean supportsDocument, boolean supportsVideo, 
                boolean supportsAudio, boolean supportsCitations, int maxContextLength, int maxGenerationLength) {
            this.modelId = modelId;
            this.displayName = displayName;
            this.supportsThinking = supportsThinking;
            this.supportsWebSearch = supportsWebSearch;
            this.supportsVision = supportsVision;
            this.supportsDocument = supportsDocument;
            this.supportsVideo = supportsVideo;
            this.supportsAudio = supportsAudio;
            this.supportsCitations = supportsCitations;
            this.maxContextLength = maxContextLength;
            this.maxGenerationLength = maxGenerationLength;
        }
        
        public String getModelId() { return modelId; }
        public String getDisplayName() { return displayName; }
        public boolean supportsThinking() { return supportsThinking; }
        public boolean supportsWebSearch() { return supportsWebSearch; }
        public boolean supportsVision() { return supportsVision; }
        public boolean supportsDocument() { return supportsDocument; }
        public boolean supportsVideo() { return supportsVideo; }
        public boolean supportsAudio() { return supportsAudio; }
        public boolean supportsCitations() { return supportsCitations; }
        public int getMaxContextLength() { return maxContextLength; }
        public int getMaxGenerationLength() { return maxGenerationLength; }
        
        public ModelCapabilities toModelCapabilities() {
            return new ModelCapabilities(
                supportsThinking, supportsWebSearch, supportsVision, supportsDocument,
                supportsVideo, supportsAudio, supportsCitations, maxContextLength, maxGenerationLength
            );
        }
    }
    
    public interface GLMResponseListener {
        void onResponse(String response, boolean isThinking, List<WebSource> webSources);
        void onError(String error);
        void onStreamUpdate(String partialResponse, boolean isThinking);
    }
    
    public interface GLMModelsCallback {
        void onModelsLoaded(List<GLMModel> models);
        void onError(String error);
    }
    
    public GLMApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * Sends a chat completion request to GLM API
     */
    public void sendChatCompletion(GLMModel model, String message, boolean enableThinking, 
                                 boolean enableWebSearch, GLMResponseListener listener) {
        new Thread(() -> {
            try {
                JsonObject requestBody = buildChatRequest(model, message, enableThinking, enableWebSearch);
                
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
                    processGLMResponse(responseBody, listener);
                } else {
                    listener.onError("GLM API request failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending GLM chat completion", e);
                listener.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Sends a streaming chat completion request to GLM API
     */
    public void sendStreamingChatCompletion(GLMModel model, String message, boolean enableThinking,
                                          boolean enableWebSearch, GLMResponseListener listener) {
        new Thread(() -> {
            try {
                JsonObject requestBody = buildChatRequest(model, message, enableThinking, enableWebSearch);
                requestBody.addProperty("stream", true);
                
                Request request = new Request.Builder()
                        .url(GLM_BASE_URL + GLM_CHAT_ENDPOINT)
                        .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "text/event-stream")
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    processGLMStreamResponse(response, listener);
                } else {
                    listener.onError("GLM streaming request failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending GLM streaming completion", e);
                listener.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Fetches available GLM models
     */
    public void fetchModels(GLMModelsCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(GLM_BASE_URL + GLM_MODELS_ENDPOINT)
                        .get()
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Accept", "application/json")
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    List<GLMModel> models = parseModelsResponse(responseBody);
                    callback.onModelsLoaded(models);
                } else {
                    callback.onError("Failed to fetch GLM models: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching GLM models", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Builds the chat request JSON for GLM API
     */
    private JsonObject buildChatRequest(GLMModel model, String message, boolean enableThinking, boolean enableWebSearch) {
        JsonObject request = new JsonObject();
        request.addProperty("model", model.getModelId());
        request.addProperty("temperature", 0.7);
        request.addProperty("max_tokens", model.getMaxGenerationLength());
        
        // Add messages array
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);
        request.add("messages", messages);
        
        // Add thinking mode if supported and enabled
        if (enableThinking && model.supportsThinking()) {
            JsonObject tools = new JsonObject();
            tools.addProperty("thinking", true);
            request.add("tools", tools);
        }
        
        // Add web search if supported and enabled
        if (enableWebSearch && model.supportsWebSearch()) {
            JsonObject webSearch = new JsonObject();
            webSearch.addProperty("enable", true);
            request.add("web_search", webSearch);
        }
        
        return request;
    }
    
    /**
     * Processes GLM API response
     */
    private void processGLMResponse(String responseBody, GLMResponseListener listener) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            
            if (response.has("error")) {
                JsonObject error = response.getAsJsonObject("error");
                listener.onError("GLM API Error: " + error.get("message").getAsString());
                return;
            }
            
            if (response.has("choices")) {
                JsonArray choices = response.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    String content = message.get("content").getAsString();
                    
                    // Check for thinking content
                    boolean isThinking = message.has("thinking") && message.get("thinking").getAsBoolean();
                    
                    // Extract web sources if available
                    List<WebSource> webSources = extractWebSources(response);
                    
                    listener.onResponse(content, isThinking, webSources);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing GLM response", e);
            listener.onError("Error processing response: " + e.getMessage());
        }
    }
    
    /**
     * Processes GLM streaming API response
     */
    private void processGLMStreamResponse(Response response, GLMResponseListener listener) throws IOException {
        StringBuilder fullContent = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder();
        List<WebSource> webSources = new ArrayList<>();
        
        String line;
        while ((line = response.body().source().readUtf8Line()) != null) {
            if (line.startsWith("data: ")) {
                String jsonData = line.substring(6);
                if (jsonData.trim().equals("[DONE]")) break;
                if (jsonData.trim().isEmpty()) continue;
                
                try {
                    JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();
                    
                    if (data.has("choices")) {
                        JsonArray choices = data.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject delta = choice.getAsJsonObject("delta");
                            
                            if (delta.has("content")) {
                                String content = delta.get("content").getAsString();
                                boolean isThinking = delta.has("thinking") && delta.get("thinking").getAsBoolean();
                                
                                if (isThinking) {
                                    thinkingContent.append(content);
                                    listener.onStreamUpdate(thinkingContent.toString(), true);
                                } else {
                                    fullContent.append(content);
                                    listener.onStreamUpdate(fullContent.toString(), false);
                                }
                            }
                            
                            if (choice.has("finish_reason") && choice.get("finish_reason").getAsString().equals("stop")) {
                                listener.onResponse(fullContent.toString(), thinkingContent.length() > 0, webSources);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse streaming JSON: " + jsonData);
                }
            }
        }
    }
    
    /**
     * Extracts web sources from GLM response
     */
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
                    "" // GLM doesn't provide favicons
                ));
            }
        }
        
        return sources;
    }
    
    /**
     * Parses models response from GLM API
     */
    private List<GLMModel> parseModelsResponse(String responseBody) {
        List<GLMModel> models = new ArrayList<>();
        
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            if (response.has("data")) {
                JsonArray modelsArray = response.getAsJsonArray("data");
                for (int i = 0; i < modelsArray.size(); i++) {
                    JsonObject modelObj = modelsArray.get(i).getAsJsonObject();
                    String modelId = modelObj.get("id").getAsString();
                    
                    // Map API response to our GLMModel enum
                    for (GLMModel model : GLMModel.values()) {
                        if (model.getModelId().equals(modelId)) {
                            models.add(model);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing models response", e);
            // Return default models if parsing fails
            for (GLMModel model : GLMModel.values()) {
                models.add(model);
            }
        }
        
        return models;
    }
    
    /**
     * Gets all available GLM models
     */
    public static List<GLMModel> getAllModels() {
        List<GLMModel> models = new ArrayList<>();
        for (GLMModel model : GLMModel.values()) {
            models.add(model);
        }
        return models;
    }
}