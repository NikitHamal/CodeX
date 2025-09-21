package com.codex.apk.core.providers;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.*;
import com.codex.apk.core.service.BaseAIService;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ToolSpec;

import com.google.gson.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import okhttp3.*;

/**
 * OpenAI-compatible service implementation that works with multiple providers
 * that support the OpenAI chat completions API format. This includes providers
 * like DeepInfra, ApiAirforce, GptOss, and others.
 */
public class OpenAICompatibleService extends BaseAIService {
    
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String MODELS_ENDPOINT = "/models";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private final Gson gson;
    
    public OpenAICompatibleService(ProviderConfig configuration, ProviderCapabilities capabilities) {
        super(configuration, capabilities);
        this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }
    
    @Override
    protected Request buildHttpRequest(AIRequest request) throws RequestBuildException {
        try {
            String url = buildUrl(CHAT_COMPLETIONS_ENDPOINT);
            JsonObject requestBody = buildRequestBody(request);
            
            Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", request.isStreaming() ? "text/event-stream" : "application/json");
            
            // Add authorization header if API key is available
            if (configuration.hasApiKey()) {
                builder.addHeader("Authorization", "Bearer " + configuration.getApiKey());
            }
            
            // Add provider-specific headers
            addProviderSpecificHeaders(builder, request);
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RequestBuildException("Failed to build HTTP request", e);
        }
    }
    
    @Override
    protected AIResponse parseResponse(Response response, String requestId) throws ParseException {
        try {
            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            return parseOpenAIResponse(jsonResponse, requestId, false, true);
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse response", e);
        }
    }
    
    @Override
    protected CompletableFuture<Void> handleStreamingResponse(Response response, String requestId,
                                                              Consumer<AIResponse> onResponse,
                                                              Consumer<Throwable> onError) {
        return CompletableFuture.runAsync(() -> {
            try {
                String responseBody = response.body().string();
                // Simple implementation - parse the whole response and send as one chunk
                AIResponse aiResponse = parseResponse(response, requestId);
                onResponse.accept(aiResponse);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }
    
    @Override
    protected List<AIModel> fetchAvailableModels() throws Exception {
        String url = buildUrl(MODELS_ENDPOINT);
        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build();
        
        if (configuration.hasApiKey()) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer " + configuration.getApiKey())
                .build();
        }
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch models: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            return parseModelsResponse(jsonResponse);
        }
    }
    
    @Override
    protected boolean performHealthCheck() throws Exception {
        // Simple health check by trying to fetch models or making a minimal request
        try {
            List<AIModel> models = fetchAvailableModels();
            return models != null;
        } catch (Exception e) {
            // If models endpoint fails, try a simple chat request
            return performSimpleHealthCheck();
        }
    }
    
    private boolean performSimpleHealthCheck() {
        try {
            AIRequest healthRequest = AIRequest.builder()
                .withModel("gpt-3.5-turbo") // Generic model name
                .addMessage(Message.user("health"))
                .withParameters(RequestParameters.builder()
                    .withMaxTokens(1)
                    .withTemperature(0.0)
                    .build())
                .build();
            
            Request httpRequest = buildHttpRequest(healthRequest);
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private String buildUrl(String endpoint) {
        String baseUrl = configuration.getBaseUrl();
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl + endpoint.substring(1);
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        } else {
            return baseUrl + endpoint;
        }
    }
    
    private JsonObject buildRequestBody(AIRequest request) {
        JsonObject body = new JsonObject();
        
        // Model
        body.addProperty("model", request.getModel());
        
        // Messages
        JsonArray messages = new JsonArray();
        for (Message message : request.getMessages()) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", message.getRole().name().toLowerCase());
            msgObj.addProperty("content", message.getContent());
            
            // Add name if present (for tool messages)
            if (message.getName() != null) {
                msgObj.addProperty("name", message.getName());
            }
            
            // Add tool calls if present
            if (message.hasToolCalls()) {
                JsonArray toolCalls = new JsonArray();
                for (ToolCall toolCall : message.getToolCalls()) {
                    JsonObject tcObj = new JsonObject();
                    tcObj.addProperty("id", toolCall.getId());
                    tcObj.addProperty("type", "function");
                    
                    JsonObject function = new JsonObject();
                    function.addProperty("name", toolCall.getName());
                    function.addProperty("arguments", toolCall.getArguments());
                    tcObj.add("function", function);
                    
                    toolCalls.add(tcObj);
                }
                msgObj.add("tool_calls", toolCalls);
            }
            
            messages.add(msgObj);
        }
        body.add("messages", messages);
        
        // Parameters
        RequestParameters params = request.getParameters();
        if (params.hasTemperature()) {
            body.addProperty("temperature", params.getTemperature());
        }
        if (params.hasMaxTokens()) {
            body.addProperty("max_tokens", params.getMaxTokens());
        }
        if (params.hasTopP()) {
            body.addProperty("top_p", params.getTopP());
        }
        if (params.hasPresencePenalty()) {
            body.addProperty("presence_penalty", params.getPresencePenalty());
        }
        if (params.hasFrequencyPenalty()) {
            body.addProperty("frequency_penalty", params.getFrequencyPenalty());
        }
        if (params.hasStopSequences()) {
            JsonArray stop = new JsonArray();
            params.getStopSequences().forEach(stop::add);
            body.add("stop", stop);
        }
        
        // Streaming
        body.addProperty("stream", params.isStream());
        
        // Tools
        if (request.hasTools()) {
            JsonArray tools = ToolSpec.toJsonArray(request.getTools());
            body.add("tools", tools);
        }
        
        // Provider-specific parameters
        addProviderSpecificParameters(body, request);
        
        return body;
    }
    
    private void addProviderSpecificHeaders(Request.Builder builder, AIRequest request) {
        AIProvider provider = configuration.getProviderType();
        
        switch (provider) {
            case DEEPINFRA:
                builder.addHeader("User-Agent", "CodeX-Android/1.0");
                break;
            case FREE:
                builder.addHeader("Origin", "https://pollinations.ai");
                builder.addHeader("Referer", "https://pollinations.ai/");
                break;
        }
    }
    
    private void addProviderSpecificParameters(JsonObject body, AIRequest request) {
        AIProvider provider = configuration.getProviderType();
        
        switch (provider) {
            case FREE:
                // Pollinations-specific parameters
                body.addProperty("seed", System.currentTimeMillis() % Integer.MAX_VALUE);
                body.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
                break;
        }
    }
    
    private AIResponse parseOpenAIResponse(JsonObject jsonResponse, String requestId, boolean isStreaming, boolean isComplete) {
        AIResponse.Builder builder = AIResponse.builder()
            .withRequestId(requestId)
            .isStreaming(isStreaming)
            .isComplete(isComplete);
        
        // Extract choices
        if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content") && !message.get("content").isJsonNull()) {
                        builder.withContent(message.get("content").getAsString());
                    }
                    
                    // Parse tool calls
                    if (message.has("tool_calls")) {
                        List<ToolCall> toolCalls = parseToolCalls(message.getAsJsonArray("tool_calls"));
                        builder.withToolCalls(toolCalls);
                    }
                }
                
                // Parse finish reason
                if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
                    String finishReason = choice.get("finish_reason").getAsString();
                    builder.withFinishReason(mapFinishReason(finishReason));
                }
            }
        }
        
        // Extract usage
        if (jsonResponse.has("usage")) {
            JsonObject usage = jsonResponse.getAsJsonObject("usage");
            TokenUsage tokenUsage = new TokenUsage(
                usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0,
                usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0,
                usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : 0
            );
            builder.withUsage(tokenUsage);
        }
        
        // Extract model
        if (jsonResponse.has("model")) {
            builder.withModel(jsonResponse.get("model").getAsString());
        }
        
        return builder.build();
    }
    
    private AIResponse parseStreamingDelta(JsonObject deltaObject, String requestId, StringBuilder contentBuffer) {
        if (!deltaObject.has("choices") || !deltaObject.get("choices").isJsonArray()) {
            return null;
        }
        
        JsonArray choices = deltaObject.getAsJsonArray("choices");
        if (choices.size() == 0) return null;
        
        JsonObject choice = choices.get(0).getAsJsonObject();
        if (!choice.has("delta")) return null;
        
        JsonObject delta = choice.getAsJsonObject("delta");
        String content = "";
        
        if (delta.has("content") && !delta.get("content").isJsonNull()) {
            content = delta.get("content").getAsString();
            contentBuffer.append(content);
        }
        
        return AIResponse.builder()
            .withRequestId(requestId)
            .withContent(content)
            .isStreaming(true)
            .isComplete(false)
            .build();
    }
    
    private List<ToolCall> parseToolCalls(JsonArray toolCallsArray) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        for (JsonElement element : toolCallsArray) {
            JsonObject tcObj = element.getAsJsonObject();
            String id = tcObj.get("id").getAsString();
            JsonObject function = tcObj.getAsJsonObject("function");
            String name = function.get("name").getAsString();
            String arguments = function.get("arguments").getAsString();
            
            toolCalls.add(new ToolCall(id, name, arguments, null));
        }
        
        return toolCalls;
    }
    
    private AIResponse.FinishReason mapFinishReason(String reason) {
        switch (reason.toLowerCase()) {
            case "stop": return AIResponse.FinishReason.STOP;
            case "length": return AIResponse.FinishReason.LENGTH;
            case "tool_calls": return AIResponse.FinishReason.TOOL_CALLS;
            case "content_filter": return AIResponse.FinishReason.CONTENT_FILTER;
            default: return AIResponse.FinishReason.STOP;
        }
    }
    
    private List<AIModel> parseModelsResponse(JsonObject jsonResponse) {
        List<AIModel> models = new ArrayList<>();
        
        if (jsonResponse.has("data") && jsonResponse.get("data").isJsonArray()) {
            JsonArray data = jsonResponse.getAsJsonArray("data");
            
            for (JsonElement element : data) {
                JsonObject modelObj = element.getAsJsonObject();
                String id = modelObj.get("id").getAsString();
                
                // Create basic capabilities for OpenAI-compatible models
                com.codex.apk.ai.ModelCapabilities capabilities = new com.codex.apk.ai.ModelCapabilities(
                    false, false, false, true, false, false, false, 4096, 2048
                );
                
                AIModel model = new AIModel(id, id, configuration.getProviderType(), capabilities);
                models.add(model);
            }
        }
        
        return models;
    }
}