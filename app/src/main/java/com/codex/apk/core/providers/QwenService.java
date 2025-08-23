package com.codex.apk.core.providers;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.*;
import com.codex.apk.core.service.BaseAIService;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ToolSpec;
import com.codex.apk.QwenConversationState;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import okhttp3.*;
import okio.BufferedSource;

/**
 * Qwen/Alibaba service implementation with conversation state management.
 * Supports streaming responses, tools, and maintains conversation context
 * across multiple requests using QwenConversationState.
 */
public class QwenService extends BaseAIService {
    
    private static final String BASE_URL = "https://chat.qwen.ai/api/v2";
    private static final String QWEN_BX_V = "2.5.31";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern MIDTOKEN_PATTERN = Pattern.compile("(?:umx\\.wu|__fycb)\\('([^']+)'\\)");
    
    private final Gson gson;
    private final Map<String, QwenConversationState> conversationStates;
    private volatile String midToken;
    private int midTokenUses = 0;
    
    public QwenService(ProviderConfig configuration, ProviderCapabilities capabilities) {
        super(configuration, capabilities);
        this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
        this.conversationStates = new ConcurrentHashMap<>();
        this.midToken = configuration.getProviderSpecificConfig("midtoken", null);
    }
    
    @Override
    protected Request buildHttpRequest(AIRequest request) throws RequestBuildException {
        try {
            // Get or create conversation state
            QwenConversationState state = getOrCreateConversationState(request);
            
            String modelId = request.getModel() != null ? request.getModel() : "qwen3-coder-plus";
            
            // Ensure conversation exists
            if (state.getConversationId() == null) {
                String conversationId = createConversation(modelId, request.requiresCapability("websearch"));
                if (conversationId == null) {
                    throw new RequestBuildException("Failed to create Qwen conversation");
                }
                state.setConversationId(conversationId);
            }
            
            String url = BASE_URL + "/chat/completions?chat_id=" + state.getConversationId();
            JsonObject requestBody = buildQwenRequestBody(request, state);
            
            String qwenToken = ensureMidToken();
            
            Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .headers(buildQwenHeaders(qwenToken, state.getConversationId()));
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RequestBuildException("Failed to build Qwen request", e);
        }
    }
    
    @Override
    protected AIResponse parseResponse(Response response, String requestId) throws ParseException {
        try {
            String responseBody = response.body().string();
            return parseQwenStreamResponse(responseBody, requestId, false, true);
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Qwen response", e);
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
        // Return static models for ALIBABA provider from AIModel registry
        List<AIModel> allModels = com.codex.apk.ai.AIModel.values();
        List<AIModel> qwenModels = new ArrayList<>();
        
        for (AIModel model : allModels) {
            if (model.getProvider() == AIProvider.ALIBABA) {
                qwenModels.add(model);
            }
        }
        
        return qwenModels;
    }
    
    @Override
    protected boolean performHealthCheck() throws Exception {
        try {
            // Simple health check by trying to create a conversation
            String testConversationId = createConversation("qwen3-coder-plus", false);
            return testConversationId != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private QwenConversationState getOrCreateConversationState(AIRequest request) {
        String sessionId = request.getContext() != null ? request.getContext().getSessionId() : "default";
        return conversationStates.computeIfAbsent(sessionId, k -> new QwenConversationState());
    }
    
    private String createConversation(String modelId, boolean webSearchEnabled) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("title", "New Chat");
        
        JsonArray modelsArray = new JsonArray();
        modelsArray.add(modelId);
        requestBody.add("models", modelsArray);
        
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("chat_type", webSearchEnabled ? "search" : "t2t");
        requestBody.addProperty("timestamp", System.currentTimeMillis());
        
        String qwenToken = ensureMidToken();
        Request request = new Request.Builder()
            .url(BASE_URL + "/chats/new")
            .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
            .headers(buildQwenHeaders(qwenToken, null))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                
                if (responseJson.has("success") && responseJson.get("success").getAsBoolean()) {
                    JsonObject data = responseJson.getAsJsonObject("data");
                    return data.get("id").getAsString();
                }
            }
        }
        
        return null;
    }
    
    private JsonObject buildQwenRequestBody(AIRequest request, QwenConversationState state) {
        JsonObject body = new JsonObject();
        
        body.addProperty("stream", request.isStreaming());
        body.addProperty("incremental_output", true);
        body.addProperty("chat_id", state.getConversationId());
        body.addProperty("chat_mode", "normal");
        body.addProperty("model", request.getModel());
        body.addProperty("parent_id", state.getLastParentId());
        body.addProperty("timestamp", System.currentTimeMillis());
        
        // Build messages array
        JsonArray messages = new JsonArray();
        
        // Add system message if this is the first message
        if (state.getLastParentId() == null && request.hasTools()) {
            JsonObject systemMessage = createSystemMessage(request.getTools());
            messages.add(systemMessage);
        }
        
        // Convert universal messages to Qwen format
        for (Message message : request.getMessages()) {
            if (message.getRole() == Message.MessageRole.USER) {
                JsonObject qwenMessage = createQwenUserMessage(message, request);
                qwenMessage.addProperty("parentId", state.getLastParentId());
                messages.add(qwenMessage);
            }
        }
        
        body.add("messages", messages);
        
        // Add tools if present
        if (request.hasTools()) {
            JsonArray tools = ToolSpec.toJsonArray(request.getTools());
            body.add("tools", tools);
            
            JsonObject toolChoice = new JsonObject();
            toolChoice.addProperty("type", "auto");
            body.add("tool_choice", toolChoice);
        }
        
        return body;
    }
    
    private JsonObject createSystemMessage(List<ToolSpec> tools) {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", buildSystemPrompt(tools));
        return systemMessage;
    }
    
    private String buildSystemPrompt(List<ToolSpec> tools) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful AI assistant with access to tools for file operations and code analysis. ");
        prompt.append("When performing file operations, always explain what you're doing and why. ");
        prompt.append("Provide clear, concise responses and use tools when appropriate to help the user.\n\n");
        
        if (!tools.isEmpty()) {
            prompt.append("Available tools:\n");
            for (ToolSpec tool : tools) {
                prompt.append("- ").append(tool.getName()).append(": ").append(tool.toJson().getAsJsonObject("function").get("description").getAsString()).append("\n");
            }
        }
        
        return prompt.toString();
    }
    
    private JsonObject createQwenUserMessage(Message message, AIRequest request) {
        JsonObject qwenMessage = new JsonObject();
        qwenMessage.addProperty("role", "user");
        qwenMessage.addProperty("content", message.getContent());
        
        // Add thinking mode if enabled
        if (request.requiresCapability("thinking")) {
            qwenMessage.addProperty("thinking_enabled", true);
        }
        
        // Add web search if enabled
        if (request.requiresCapability("websearch")) {
            qwenMessage.addProperty("enable_search", true);
        }
        
        return qwenMessage;
    }
    
    private Headers buildQwenHeaders(String midToken, String conversationId) {
        Headers.Builder builder = new Headers.Builder()
            .add("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .add("Accept", "text/event-stream")
            .add("Accept-Language", "en-US,en;q=0.9")
            .add("Accept-Encoding", "gzip, deflate, br")
            .add("Content-Type", "application/json")
            .add("Origin", "https://chat.qwen.ai")
            .add("Referer", "https://chat.qwen.ai/")
            .add("X-Xsrf-Token", midToken != null ? midToken : "")
            .add("Bx-V", QWEN_BX_V);
        
        if (conversationId != null) {
            builder.add("X-Chat-Id", conversationId);
        }
        
        return builder.build();
    }
    
    private String ensureMidToken() throws Exception {
        if (midToken != null && midTokenUses < 50) {
            midTokenUses++;
            return midToken;
        }
        
        // Fetch new midtoken
        midToken = fetchMidToken();
        midTokenUses = 1;
        
        if (midToken == null) {
            throw new Exception("Failed to fetch Qwen midtoken");
        }
        
        return midToken;
    }
    
    private String fetchMidToken() throws Exception {
        Request request = new Request.Builder()
            .url("https://chat.qwen.ai/")
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String html = response.body().string();
                Matcher matcher = MIDTOKEN_PATTERN.matcher(html);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        
        return null;
    }
    
    private AIResponse parseQwenStreamResponse(String responseBody, String requestId, boolean isStreaming, boolean isComplete) {
        AIResponse.Builder builder = AIResponse.builder()
            .withRequestId(requestId)
            .isStreaming(isStreaming)
            .isComplete(isComplete);
        
        try {
            // Parse Qwen's response format
            StringBuilder content = new StringBuilder();
            StringBuilder thinking = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();
            
            String[] lines = responseBody.split("\n");
            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (!"[DONE]".equals(data)) {
                        try {
                            JsonObject deltaObject = JsonParser.parseString(data).getAsJsonObject();
                            parseQwenDelta(deltaObject, content, thinking, toolCalls);
                        } catch (Exception e) {
                            // Continue processing other lines
                        }
                    }
                }
            }
            
            builder.withContent(content.toString());
            
            if (thinking.length() > 0) {
                builder.withThinking(new ThinkingContent(thinking.toString(), true));
            }
            
            if (!toolCalls.isEmpty()) {
                builder.withToolCalls(toolCalls);
                builder.withFinishReason(AIResponse.FinishReason.TOOL_CALLS);
            } else {
                builder.withFinishReason(AIResponse.FinishReason.STOP);
            }
            
        } catch (Exception e) {
            logError("Failed to parse Qwen response", e);
            builder.withContent(""); // Return empty response on parse failure
        }
        
        return builder.build();
    }
    
    private AIResponse parseStreamingDelta(JsonObject deltaObject, String requestId, 
                                         StringBuilder contentBuffer, StringBuilder thinkingBuffer) {
        String deltaContent = "";
        String deltaThinking = "";
        
        if (deltaObject.has("choices") && deltaObject.get("choices").isJsonArray()) {
            JsonArray choices = deltaObject.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                
                if (choice.has("delta")) {
                    JsonObject delta = choice.getAsJsonObject("delta");
                    
                    if (delta.has("content") && !delta.get("content").isJsonNull()) {
                        deltaContent = delta.get("content").getAsString();
                        contentBuffer.append(deltaContent);
                    }
                    
                    if (delta.has("thinking") && !delta.get("thinking").isJsonNull()) {
                        deltaThinking = delta.get("thinking").getAsString();
                        thinkingBuffer.append(deltaThinking);
                    }
                }
            }
        }
        
        AIResponse.Builder builder = AIResponse.builder()
            .withRequestId(requestId)
            .withContent(deltaContent)
            .isStreaming(true)
            .isComplete(false);
        
        if (!deltaThinking.isEmpty()) {
            builder.withThinking(new ThinkingContent(deltaThinking, true));
        }
        
        return builder.build();
    }
    
    private void parseQwenDelta(JsonObject deltaObject, StringBuilder content, 
                              StringBuilder thinking, List<ToolCall> toolCalls) {
        if (deltaObject.has("choices") && deltaObject.get("choices").isJsonArray()) {
            JsonArray choices = deltaObject.getAsJsonArray("choices");
            for (JsonElement choiceElement : choices) {
                JsonObject choice = choiceElement.getAsJsonObject();
                
                if (choice.has("delta")) {
                    JsonObject delta = choice.getAsJsonObject("delta");
                    
                    if (delta.has("content") && !delta.get("content").isJsonNull()) {
                        content.append(delta.get("content").getAsString());
                    }
                    
                    if (delta.has("thinking") && !delta.get("thinking").isJsonNull()) {
                        thinking.append(delta.get("thinking").getAsString());
                    }
                    
                    if (delta.has("tool_calls") && delta.get("tool_calls").isJsonArray()) {
                        JsonArray toolCallsArray = delta.getAsJsonArray("tool_calls");
                        for (JsonElement toolCallElement : toolCallsArray) {
                            JsonObject toolCallObj = toolCallElement.getAsJsonObject();
                            
                            String id = toolCallObj.has("id") ? toolCallObj.get("id").getAsString() : 
                                java.util.UUID.randomUUID().toString();
                            String name = toolCallObj.getAsJsonObject("function").get("name").getAsString();
                            String arguments = toolCallObj.getAsJsonObject("function").get("arguments").getAsString();
                            
                            toolCalls.add(new ToolCall(id, name, arguments, null));
                        }
                    }
                }
            }
        }
    }
}