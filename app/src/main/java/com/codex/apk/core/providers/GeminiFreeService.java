package com.codex.apk.core.providers;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.*;
import com.codex.apk.core.service.BaseAIService;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import okhttp3.*;

/**
 * Gemini Free service implementation using reverse-engineered cookie-based authentication.
 * This implementation uses __Secure-1PSID and __Secure-1PSIDTS cookies to access Gemini
 * without requiring an official API key.
 */
public class GeminiFreeService extends BaseAIService {
    
    private static final String INIT_URL = "https://gemini.google.com/app";
    private static final String GENERATE_URL = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String UPLOAD_URL = "https://content-push.googleapis.com/upload";
    
    private final Gson gson;
    private volatile String accessToken;
    private volatile Map<String, String> sessionCookies;
    
    public GeminiFreeService(ProviderConfig configuration, ProviderCapabilities capabilities) {
        super(configuration, capabilities);
        this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
        this.sessionCookies = new HashMap<>();
        initializeSession();
    }
    
    @Override
    protected Request buildHttpRequest(AIRequest request) throws RequestBuildException {
        try {
            // Ensure we have a valid session
            if (accessToken == null) {
                initializeSession();
                if (accessToken == null) {
                    throw new RequestBuildException("Failed to initialize Gemini session");
                }
            }
            
            String modelId = request.getModel() != null ? request.getModel() : "gemini-2.5-flash";
            
            // Build form data for Gemini's expected format
            FormBody.Builder formBuilder = new FormBody.Builder();
            formBuilder.add("at", accessToken);
            
            // Build the request payload in Gemini's expected format
            JsonObject requestData = buildGeminiFreeRequestData(request);
            formBuilder.add("f.req", requestData.toString());
            
            Headers requestHeaders = buildGeminiHeaders(modelId);
            
            Request.Builder builder = new Request.Builder()
                .url(GENERATE_URL)
                .post(formBuilder.build())
                .headers(requestHeaders)
                .header("Cookie", buildCookieHeader(sessionCookies));
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RequestBuildException("Failed to build Gemini Free request", e);
        }
    }
    
    @Override
    protected AIResponse parseResponse(Response response, String requestId) throws ParseException {
        try {
            String responseBody = response.body().string();
            return parseGeminiFreeResponse(responseBody, requestId, false, true);
            
        } catch (Exception e) {
            throw new ParseException("Failed to parse Gemini Free response", e);
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
        // Return static models for COOKIES provider from AIModel registry
        List<AIModel> allModels = com.codex.apk.ai.AIModel.values();
        List<AIModel> geminiModels = new ArrayList<>();
        
        for (AIModel model : allModels) {
            if (model.getProvider() == AIProvider.COOKIES) {
                geminiModels.add(model);
            }
        }
        
        return geminiModels;
    }
    
    @Override
    protected boolean performHealthCheck() throws Exception {
        try {
            // Check if we can get access token
            if (accessToken == null) {
                initializeSession();
            }
            return accessToken != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void initializeSession() {
        try {
            // Get cookies from configuration
            String psid = configuration.getProviderSpecificConfig("psid", null);
            String psidts = configuration.getProviderSpecificConfig("psidts", null);
            
            if (psid == null || psid.isEmpty()) {
                logError("__Secure-1PSID cookie not configured", null);
                return;
            }
            
            sessionCookies.clear();
            sessionCookies.put("__Secure-1PSID", psid);
            if (psidts != null && !psidts.isEmpty()) {
                sessionCookies.put("__Secure-1PSIDTS", psidts);
            }
            
            // Warmup session and fetch access token
            warmupSession();
            fetchAccessToken();
            
        } catch (Exception e) {
            logError("Failed to initialize Gemini session", e);
        }
    }
    
    private void warmupSession() throws Exception {
        // Visit Google.com to establish session
        Request warmupRequest = new Request.Builder()
            .url("https://www.google.com")
            .header("Cookie", buildCookieHeader(sessionCookies))
            .build();
        
        try (Response response = httpClient.newCall(warmupRequest).execute()) {
            updateCookiesFromResponse(response);
        }
        
        // Visit Gemini app to initialize
        Request initRequest = new Request.Builder()
            .url(INIT_URL)
            .header("Cookie", buildCookieHeader(sessionCookies))
            .build();
        
        try (Response response = httpClient.newCall(initRequest).execute()) {
            updateCookiesFromResponse(response);
        }
    }
    
    private void fetchAccessToken() throws Exception {
        Request tokenRequest = new Request.Builder()
            .url(INIT_URL)
            .header("Cookie", buildCookieHeader(sessionCookies))
            .build();
        
        try (Response response = httpClient.newCall(tokenRequest).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                this.accessToken = extractAccessTokenFromHtml(body);
                updateCookiesFromResponse(response);
            }
        }
    }
    
    private String extractAccessTokenFromHtml(String html) {
        // Extract access token from HTML using various patterns
        String[] patterns = {
            "\"SNlM0e\":\"([^\"]+)\"",
            "'SNlM0e':'([^']+)'",
            "SNlM0e:\"([^\"]+)\"",
            "SNlM0e:'([^']+)'"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        }
        
        return null;
    }
    
    private JsonObject buildGeminiFreeRequestData(AIRequest request) {
        JsonArray requestArray = new JsonArray();
        
        // Build the complex nested structure Gemini expects
        JsonArray innerArray = new JsonArray();
        
        // Message content
        String messageContent = buildMessageContent(request.getMessages());
        innerArray.add(messageContent);
        
        // Conversation metadata (if available)
        String conversationMeta = configuration.getProviderSpecificConfig("conversation_meta", "[]");
        try {
            JsonArray metaArray = JsonParser.parseString(conversationMeta).getAsJsonArray();
            innerArray.add(metaArray);
        } catch (Exception e) {
            innerArray.add(new JsonArray()); // Empty array as fallback
        }
        
        // Additional parameters
        JsonArray paramsArray = new JsonArray();
        paramsArray.add(JsonNull.INSTANCE); // Usually null
        paramsArray.add(JsonNull.INSTANCE); // Usually null
        paramsArray.add(JsonNull.INSTANCE); // Usually null
        paramsArray.add(new JsonArray()); // Empty array for extensions
        innerArray.add(paramsArray);
        
        requestArray.add(innerArray);
        
        return new JsonObject(); // Simplified - would need full Gemini protocol implementation
    }
    
    private String buildMessageContent(List<Message> messages) {
        StringBuilder content = new StringBuilder();
        
        for (Message message : messages) {
            switch (message.getRole()) {
                case SYSTEM:
                    // Prepend system message as context
                    if (content.length() > 0) content.append("\n\n");
                    content.append(message.getContent());
                    break;
                case USER:
                    if (content.length() > 0) content.append("\n\n");
                    content.append(message.getContent());
                    break;
                case ASSISTANT:
                    // Skip assistant messages in request building
                    break;
            }
        }
        
        return content.toString();
    }
    
    private Headers buildGeminiHeaders(String modelId) {
        return new Headers.Builder()
            .add("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .add("Accept", "*/*")
            .add("Accept-Language", "en-US,en;q=0.9")
            .add("Accept-Encoding", "gzip, deflate, br")
            .add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .add("Origin", "https://gemini.google.com")
            .add("Referer", "https://gemini.google.com/app")
            .add("X-Same-Domain", "1")
            .build();
    }
    
    private String buildCookieHeader(Map<String, String> cookies) {
        StringBuilder cookieHeader = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (cookieHeader.length() > 0) {
                cookieHeader.append("; ");
            }
            cookieHeader.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return cookieHeader.toString();
    }
    
    private void updateCookiesFromResponse(Response response) {
        List<String> setCookieHeaders = response.headers("Set-Cookie");
        for (String setCookie : setCookieHeaders) {
            String[] parts = setCookie.split(";")[0].split("=", 2);
            if (parts.length == 2) {
                sessionCookies.put(parts[0], parts[1]);
            }
        }
    }
    
    private AIResponse parseGeminiFreeResponse(String responseBody, String requestId, boolean isStreaming, boolean isComplete) {
        AIResponse.Builder builder = AIResponse.builder()
            .withRequestId(requestId)
            .isStreaming(isStreaming)
            .isComplete(isComplete);
        
        try {
            // Parse Gemini's complex response format
            ParsedOutput parsed = parseOutputFromStream(responseBody);
            
            if (parsed.text != null && !parsed.text.isEmpty()) {
                builder.withContent(parsed.text);
            }
            
            if (parsed.thoughts != null && !parsed.thoughts.isEmpty()) {
                ThinkingContent thinking = new ThinkingContent(parsed.thoughts, true);
                builder.withThinking(thinking);
            }
            
            builder.withFinishReason(AIResponse.FinishReason.STOP);
            
        } catch (Exception e) {
            logError("Failed to parse Gemini Free response", e);
            builder.withContent(""); // Return empty response on parse failure
        }
        
        return builder.build();
    }
    
    private AIResponse parseStreamingLine(String line, String requestId, StringBuilder contentBuffer) {
        // This would need specific implementation for Gemini's streaming format
        // For now, return null to indicate no delta content
        return null;
    }
    
    private ParsedOutput parseOutputFromStream(String responseBody) {
        // Simplified parsing - would need full implementation of Gemini's response format
        ParsedOutput output = new ParsedOutput();
        
        try {
            // Look for text content in various response patterns
            if (responseBody.contains("\"text\":")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");
                java.util.regex.Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    output.text = matcher.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                }
            }
            
            // Look for thinking content
            if (responseBody.contains("\"thinking\":")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"thinking\"\\s*:\\s*\"([^\"]+)\"");
                java.util.regex.Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    output.thoughts = matcher.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                }
            }
            
        } catch (Exception e) {
            logError("Error parsing Gemini response stream", e);
        }
        
        return output;
    }
    
    /**
     * Helper class for parsed output.
     */
    private static class ParsedOutput {
        String text = "";
        String thoughts = "";
    }
}