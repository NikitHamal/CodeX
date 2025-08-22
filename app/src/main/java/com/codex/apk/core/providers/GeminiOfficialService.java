package com.codex.apk.core.providers;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.*;
import com.codex.apk.core.service.BaseAIService;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ToolSpec;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.*;

/**
 * Gemini Official API service implementation using the Generative Language API.
 * Supports both streaming and non-streaming responses with vision capabilities.
 */
public class GeminiOfficialService extends BaseAIService {
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private final Gson gson;
    
    public GeminiOfficialService(ProviderConfig configuration, ProviderCapabilities capabilities) {
        super(configuration, capabilities);
        this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }
    
    @Override
    protected Request buildHttpRequest(AIRequest request) throws RequestBuildException {
        try {
            String modelId = request.getModel() != null ? request.getModel() : "gemini-1.5-flash";
            String endpoint = request.isStreaming() ? ":streamGenerateContent" : ":generateContent";
            String url = BASE_URL + "/models/" + modelId + endpoint;
            
            // Add API key as query parameter
            if (configuration.hasApiKey()) {
                url += "?key=" + configuration.getApiKey();
            } else {
                throw new RequestBuildException("API key is required for Gemini Official API");
            }
            
            JsonObject requestBody = buildGeminiRequestBody(request);
            
            Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json");
            
            if (request.isStreaming()) {
                builder.addHeader("Accept", "text/event-stream");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RequestBuildException("Failed to build Gemini request", e);
        }
    }
    
    @Override
    protected AIResponse parseResponse(Response response, String requestId) throws ParseException {
        try {
            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            return parseGeminiResponse(jsonResponse, requestId, false, true);
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Gemini response", e);
        }
    }
    
    @Override
    protected Observable<AIResponse> handleStreamingResponse(Response response, String requestId) {
        return Observable.create(emitter -> {
            try (BufferedSource source = response.body().source()) {
                source.timeout().timeout(60, TimeUnit.SECONDS);
                
                StringBuilder contentBuffer = new StringBuilder();
                String line;
                
                while (!emitter.isDisposed() && (line = source.readUtf8LineStrict()) != null) {
                    if (line.isEmpty()) continue;
                    
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        
                        if ("[DONE]".equals(data)) {
                            // Emit final complete response
                            AIResponse finalResponse = AIResponse.builder()
                                .withRequestId(requestId)
                                .withContent(contentBuffer.toString())
                                .withFinishReason(AIResponse.FinishReason.STOP)
                                .isStreaming(false)
                                .isComplete(true)
                                .build();
                            emitter.onNext(finalResponse);
                            emitter.onComplete();
                            break;
                        }
                        
                        try {
                            JsonObject streamChunk = JsonParser.parseString(data).getAsJsonObject();
                            AIResponse deltaResponse = parseStreamingChunk(streamChunk, requestId, contentBuffer);
                            
                            if (deltaResponse != null) {
                                emitter.onNext(deltaResponse);
                            }
                            
                        } catch (JsonSyntaxException e) {
                            logError("Failed to parse streaming chunk: " + data, e);
                        }
                    }
                }
                
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }
    
    @Override
    protected List<AIModel> fetchAvailableModels() throws Exception {
        // Return static models for GOOGLE provider from AIModel registry
        List<AIModel> allModels = com.codex.apk.ai.AIModel.values();
        List<AIModel> geminiModels = new ArrayList<>();
        
        for (AIModel model : allModels) {
            if (model.getProvider() == AIProvider.GOOGLE) {
                geminiModels.add(model);
            }
        }
        
        return geminiModels;
    }
    
    @Override
    protected boolean performHealthCheck() throws Exception {
        // Simple health check by making a minimal request
        try {
            String url = BASE_URL + "/models/gemini-1.5-flash:generateContent";
            if (configuration.hasApiKey()) {
                url += "?key=" + configuration.getApiKey();
            }
            
            JsonObject healthRequest = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", "health");
            parts.add(textPart);
            userMessage.add("parts", parts);
            contents.add(userMessage);
            healthRequest.add("contents", contents);
            
            // Add generation config to limit response
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("maxOutputTokens", 1);
            healthRequest.add("generationConfig", generationConfig);
            
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(healthRequest.toString(), JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private JsonObject buildGeminiRequestBody(AIRequest request) {
        JsonObject body = new JsonObject();
        
        // Build contents array
        JsonArray contents = new JsonArray();
        
        for (Message message : request.getMessages()) {
            JsonObject content = new JsonObject();
            content.addProperty("role", mapMessageRole(message.getRole()));
            
            JsonArray parts = new JsonArray();
            
            // Add text content
            if (message.hasContent()) {
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", message.getContent());
                parts.add(textPart);
            }
            
            // Add attachments (images, documents)
            for (Attachment attachment : message.getAttachments()) {
                if ("image".equals(attachment.getType())) {
                    JsonObject imagePart = new JsonObject();
                    JsonObject inlineData = new JsonObject();
                    inlineData.addProperty("mimeType", attachment.getMimeType());
                    
                    if (attachment.getData() != null) {
                        // Base64 encode image data
                        String base64Data = java.util.Base64.getEncoder().encodeToString(attachment.getData());
                        inlineData.addProperty("data", base64Data);
                    } else if (attachment.getUrl() != null) {
                        // For URL-based images, we'd need to fetch and encode
                        // For now, skip URL-based images
                        continue;
                    }
                    
                    imagePart.add("inlineData", inlineData);
                    parts.add(imagePart);
                }
            }
            
            content.add("parts", parts);
            contents.add(content);
        }
        
        body.add("contents", contents);
        
        // Add generation configuration
        JsonObject generationConfig = new JsonObject();
        RequestParameters params = request.getParameters();
        
        if (params.hasTemperature()) {
            generationConfig.addProperty("temperature", params.getTemperature());
        }
        if (params.hasMaxTokens()) {
            generationConfig.addProperty("maxOutputTokens", params.getMaxTokens());
        }
        if (params.hasTopP()) {
            generationConfig.addProperty("topP", params.getTopP());
        }
        if (params.hasTopK()) {
            generationConfig.addProperty("topK", params.getTopK());
        }
        if (params.hasStopSequences()) {
            JsonArray stopSequences = new JsonArray();
            params.getStopSequences().forEach(stopSequences::add);
            generationConfig.add("stopSequences", stopSequences);
        }
        
        body.add("generationConfig", generationConfig);
        
        // Add tools if present
        if (request.hasTools()) {
            JsonArray tools = new JsonArray();
            for (ToolSpec toolSpec : request.getTools()) {
                JsonObject tool = new JsonObject();
                JsonObject functionDeclaration = new JsonObject();
                functionDeclaration.addProperty("name", toolSpec.getName());
                functionDeclaration.addProperty("description", toolSpec.toJson().getAsJsonObject("function").get("description").getAsString());
                functionDeclaration.add("parameters", toolSpec.toJson().getAsJsonObject("function").get("parameters"));
                tool.add("functionDeclaration", functionDeclaration);
                tools.add(tool);
            }
            body.add("tools", tools);
        }
        
        // Add safety settings for permissive content
        JsonArray safetySettings = new JsonArray();
        String[] categories = {
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_DANGEROUS_CONTENT",
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT"
        };
        
        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_NONE");
            safetySettings.add(setting);
        }
        body.add("safetySettings", safetySettings);
        
        return body;
    }
    
    private String mapMessageRole(Message.MessageRole role) {
        switch (role) {
            case USER:
                return "user";
            case ASSISTANT:
                return "model";
            case SYSTEM:
                // Gemini doesn't have explicit system role, prepend to first user message
                return "user";
            case TOOL:
                return "function";
            default:
                return "user";
        }
    }
    
    private AIResponse parseGeminiResponse(JsonObject jsonResponse, String requestId, boolean isStreaming, boolean isComplete) {
        AIResponse.Builder builder = AIResponse.builder()
            .withRequestId(requestId)
            .isStreaming(isStreaming)
            .isComplete(isComplete);
        
        // Parse candidates
        if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isJsonArray()) {
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                
                // Parse content
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    StringBuilder textContent = new StringBuilder();
                    
                    if (content.has("parts") && content.get("parts").isJsonArray()) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        for (JsonElement partElement : parts) {
                            JsonObject part = partElement.getAsJsonObject();
                            
                            if (part.has("text")) {
                                textContent.append(part.get("text").getAsString());
                            }
                            
                            // Parse function calls
                            if (part.has("functionCall")) {
                                JsonObject functionCall = part.getAsJsonObject("functionCall");
                                String name = functionCall.get("name").getAsString();
                                JsonObject args = functionCall.getAsJsonObject("args");
                                
                                ToolCall toolCall = new ToolCall(
                                    java.util.UUID.randomUUID().toString(),
                                    name,
                                    args.toString(),
                                    null
                                );
                                builder.addToolCall(toolCall);
                            }
                        }
                    }
                    
                    builder.withContent(textContent.toString());
                }
                
                // Parse finish reason
                if (candidate.has("finishReason")) {
                    String finishReason = candidate.get("finishReason").getAsString();
                    builder.withFinishReason(mapFinishReason(finishReason));
                }
            }
        }
        
        // Parse usage metadata
        if (jsonResponse.has("usageMetadata")) {
            JsonObject usage = jsonResponse.getAsJsonObject("usageMetadata");
            TokenUsage tokenUsage = new TokenUsage(
                usage.has("promptTokenCount") ? usage.get("promptTokenCount").getAsInt() : 0,
                usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").getAsInt() : 0,
                usage.has("totalTokenCount") ? usage.get("totalTokenCount").getAsInt() : 0
            );
            builder.withUsage(tokenUsage);
        }
        
        return builder.build();
    }
    
    private AIResponse parseStreamingChunk(JsonObject chunk, String requestId, StringBuilder contentBuffer) {
        if (!chunk.has("candidates") || !chunk.get("candidates").isJsonArray()) {
            return null;
        }
        
        JsonArray candidates = chunk.getAsJsonArray("candidates");
        if (candidates.size() == 0) return null;
        
        JsonObject candidate = candidates.get(0).getAsJsonObject();
        if (!candidate.has("content")) return null;
        
        JsonObject content = candidate.getAsJsonObject("content");
        if (!content.has("parts") || !content.get("parts").isJsonArray()) return null;
        
        StringBuilder deltaContent = new StringBuilder();
        JsonArray parts = content.getAsJsonArray("parts");
        
        for (JsonElement partElement : parts) {
            JsonObject part = partElement.getAsJsonObject();
            if (part.has("text")) {
                String text = part.get("text").getAsString();
                deltaContent.append(text);
                contentBuffer.append(text);
            }
        }
        
        return AIResponse.builder()
            .withRequestId(requestId)
            .withContent(deltaContent.toString())
            .isStreaming(true)
            .isComplete(false)
            .build();
    }
    
    private AIResponse.FinishReason mapFinishReason(String reason) {
        switch (reason.toUpperCase()) {
            case "STOP":
                return AIResponse.FinishReason.STOP;
            case "MAX_TOKENS":
                return AIResponse.FinishReason.LENGTH;
            case "SAFETY":
                return AIResponse.FinishReason.CONTENT_FILTER;
            case "RECITATION":
                return AIResponse.FinishReason.CONTENT_FILTER;
            default:
                return AIResponse.FinishReason.STOP;
        }
    }
}