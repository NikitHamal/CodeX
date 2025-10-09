package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.ModelCapabilities;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.WebSource;
import com.codex.apk.editor.AiAssistantManager;
import com.codex.apk.util.ResponseUtils;
import com.codex.apk.util.FileOps;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class QwenApiClient implements ApiClient {
    private static final String TAG = "QwenApiClient";
    private static final String PREFS_NAME = "ai_chat_prefs";
    private static final String QWEN_CONVERSATION_STATE_KEY_PREFIX = "qwen_conv_state_";
    private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";
    private static final String QWEN_BX_V = "2.5.31";
    private static final Pattern MIDTOKEN_PATTERN = Pattern.compile("(?:umx\\.wu|__fycb)\\('([^']+)'\\)");
    private static final String QWEN_MIDTOKEN_KEY = "qwen_midtoken";

    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SharedPreferences sharedPreferences;
    private final AIAssistant.AIActionListener actionListener;

    private final File projectDir;
    private volatile String midToken = null;
    private int midTokenUses = 0;
    // Shared cookie store to allow clearing on token invalidation
    private final Map<String, List<Cookie>> cookieStoreRef = new HashMap<>();

    public QwenApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.context = context;
        this.actionListener = actionListener;
        this.projectDir = projectDir;
        CookieJar cookieJar = new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStoreRef.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStoreRef.get(url.host());
                return cookies != null ? cookies : Collections.emptyList();
            }
        };

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
        this.gson = new Gson();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            this.midToken = sharedPreferences.getString(QWEN_MIDTOKEN_KEY, null);
            if (this.midToken != null) {
                this.midTokenUses = 0;
                Log.i(TAG, "Loaded persisted midtoken.");
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                String conversationId = startOrContinueConversation(state, model, webSearchEnabled);
                if (conversationId == null) {
                    if (actionListener != null) actionListener.onAiError("Failed to create conversation");
                    return;
                }
                state.setConversationId(conversationId);
                performCompletion(state, history, model, thinkingModeEnabled, webSearchEnabled, enabledTools, message);
            } catch (IOException e) {
                Log.e(TAG, "Error sending Qwen message", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            }
        }).start();
    }

    private String startOrContinueConversation(QwenConversationState state, AIModel model, boolean webSearchEnabled) throws IOException {
        if (state != null && state.getConversationId() != null) {
            return state.getConversationId();
        }
        return createQwenConversation(model, webSearchEnabled);
    }

    private String createQwenConversation(AIModel model, boolean webSearchEnabled) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("title", "New Chat");
        JsonArray modelsArray = new JsonArray();
        modelsArray.add(model.getModelId());
        requestBody.add("models", modelsArray);
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("chat_type", webSearchEnabled ? "search" : "t2t");
        requestBody.addProperty("timestamp", System.currentTimeMillis());

        String qwenToken = ensureMidToken(false);
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/chats/new")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .headers(buildQwenHeaders(qwenToken, null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                if (responseJson.get("success").getAsBoolean()) {
                    return responseJson.getAsJsonObject("data").get("id").getAsString();
                }
            } else {
                int code = response != null ? response.code() : 0;
                if (code == 401 || code == 429) {
                    // Refresh token in synchronized ensureMidToken and clear cookies
                    qwenToken = ensureMidToken(true);
                    Request retry = new Request.Builder()
                            .url(QWEN_BASE_URL + "/chats/new")
                            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                            .headers(buildQwenHeaders(qwenToken, null))
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

    private void performCompletion(QwenConversationState state, List<ChatMessage> history, AIModel model, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, String userMessage) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("stream", true);
        requestBody.addProperty("incremental_output", true);
        requestBody.addProperty("chat_id", state.getConversationId());
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("model", model.getModelId());
        requestBody.addProperty("parent_id", state.getLastParentId()); // Use last parent ID
        requestBody.addProperty("timestamp", System.currentTimeMillis());

        JsonArray messages = new JsonArray();
        // If this is the first message of a conversation, add the system prompt.
        if (state.getLastParentId() == null) {
            messages.add(createSystemMessage(enabledTools));
        }

        // Add the current user message (do not set per-message parentId; only use top-level parent_id)
        JsonObject userMsg = createUserMessage(userMessage, model, thinkingModeEnabled, webSearchEnabled);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        if (!enabledTools.isEmpty()) {
            requestBody.add("tools", ToolSpec.toJsonArray(enabledTools));
            JsonObject toolChoice = new JsonObject();
            toolChoice.addProperty("type", "auto");
            requestBody.add("tool_choice", toolChoice);
        }

        String qwenToken = ensureMidToken(false);
        Request request = new Request.Builder()
            .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
            .headers(buildQwenHeaders(qwenToken, state.getConversationId()))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                processQwenStreamResponse(response, state, model);
            } else {
                int code = response != null ? response.code() : 0;
                if (code == 401 || code == 429) {
                    // Refresh token and retry once
                    qwenToken = ensureMidToken(true);
                    Request retryReq = new Request.Builder()
                        .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
                        .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                        .headers(buildQwenHeaders(qwenToken, state.getConversationId()))
                        .build();
                    try (Response resp2 = httpClient.newCall(retryReq).execute()) {
                        if (resp2.isSuccessful() && resp2.body() != null) {
                            processQwenStreamResponse(resp2, state, model);
                            return;
                        }
                    }
                }
                if (actionListener != null) actionListener.onAiError("Failed to send message" + (code > 0 ? (" (HTTP " + code + ")") : ""));
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }
    }

    private void processQwenStreamResponse(Response response, QwenConversationState state, AIModel model) throws IOException {
        StringBuilder thinkingContent = new StringBuilder();
        StringBuilder answerContent = new StringBuilder();
        List<WebSource> webSources = new ArrayList<>();
        Set<String> seenWebUrls = new HashSet<>();
        StringBuilder rawStreamData = new StringBuilder();

        String line;
        while ((line = response.body().source().readUtf8Line()) != null) {
            rawStreamData.append(line).append("\n");
            String t = line.trim();
            if (t.isEmpty()) continue;
            String jsonData = null;
            if ("data: [DONE]".equals(t) || "[DONE]".equals(t)) {
                String finalContentDone = answerContent.length() > 0 ? answerContent.toString() : thinkingContent.toString();
                String trueRawResponse = rawStreamData.toString();
                if (actionListener != null) notifyAiActionsProcessed(trueRawResponse, finalContentDone, new ArrayList<>(), new ArrayList<>(), model.getDisplayName(), thinkingContent.toString(), webSources);
                if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
                break;
            }
            if (t.startsWith("data: ")) {
                jsonData = t.substring(6);
            } else if (t.startsWith("{")) {
                jsonData = t;
            }
            if (jsonData != null) {
                if (jsonData.trim().isEmpty()) continue;
                String trimmedJson = jsonData.trim();
                // Only attempt to parse proper JSON payloads; skip heartbeats/other tokens
                if (!(trimmedJson.startsWith("{") || trimmedJson.startsWith("["))) {
                    continue;
                }

                try {
                    JsonObject data = JsonParser.parseString(trimmedJson).getAsJsonObject();

                    // Check for conversation state updates
                    if (data.has("response.created")) {
                        JsonObject created = data.getAsJsonObject("response.created");
                        if (created.has("chat_id")) state.setConversationId(created.get("chat_id").getAsString());
                        if (created.has("response_id")) state.setLastParentId(created.get("response_id").getAsString());
                        // Persist state ASAP
                        if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
                        continue; // This line doesn't contain choices, so we skip to the next
                    }

                    if (data.has("choices")) {
                        JsonArray choices = data.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject delta = choice.getAsJsonObject("delta");
                            String status = delta.has("status") ? delta.get("status").getAsString() : "";
                            String content = delta.has("content") ? delta.get("content").getAsString() : "";
                            String phase = delta.has("phase") ? delta.get("phase").getAsString() : "";

                            // Accumulate per-phase content and signals
                            if ("think".equals(phase)) {
                                thinkingContent.append(content);
                                if (actionListener != null) actionListener.onAiStreamUpdate(thinkingContent.toString(), true);
                            } else if ("answer".equals(phase)) {
                                answerContent.append(content);
                                // Stream answer tokens too
                                if (actionListener != null) actionListener.onAiStreamUpdate(answerContent.toString(), false);
                            } else if ("web_search".equals(phase)) {
                                // Harvest web search sources from function delta extras when available
                                if (delta.has("extra") && delta.get("extra").isJsonObject()) {
                                    JsonObject extra = delta.getAsJsonObject("extra");
                                    if (extra.has("web_search_info") && extra.get("web_search_info").isJsonArray()) {
                                        JsonArray infos = extra.getAsJsonArray("web_search_info");
                                        for (int i = 0; i < infos.size(); i++) {
                                            try {
                                                JsonObject info = infos.get(i).getAsJsonObject();
                                                String url = info.has("url") ? info.get("url").getAsString() : "";
                                                if (url == null || url.isEmpty() || seenWebUrls.contains(url)) continue;
                                                String title = info.has("title") ? info.get("title").getAsString() : url;
                                                String snippet = info.has("snippet") ? info.get("snippet").getAsString() : "";
                                                String favicon = info.has("hostlogo") ? info.get("hostlogo").getAsString() : null;
                                                webSources.add(new WebSource(url, title, snippet, favicon));
                                                seenWebUrls.add(url);
                                            } catch (Exception ignored) {}
                                        }
                                    }
                                }
                            }

                            // Finalize when status is finished regardless of phase
                            if ("finished".equals(status)) {
                                String finalContent = answerContent.length() > 0 ? answerContent.toString() : thinkingContent.toString();
                                String trueRawResponse = rawStreamData.toString();
                                String jsonToParse = extractJsonFromCodeBlock(finalContent);
                                if (jsonToParse == null && QwenResponseParser.looksLikeJson(finalContent)) {
                                    jsonToParse = finalContent;
                                }

                                if (jsonToParse != null) {
                                    try {
                                        // Check for tool_call envelope
                                        JsonObject maybe = JsonParser.parseString(jsonToParse).getAsJsonObject();
                                        if (maybe.has("action") && "tool_call".equals(maybe.get("action").getAsString())) {
                                            // Execute tool calls, then continue in-loop by synthesizing a tool_result follow-up content
                                            JsonArray calls = maybe.getAsJsonArray("tool_calls");
                                            JsonArray results = new JsonArray();
                                            for (int i = 0; i < calls.size(); i++) {
                                                JsonObject c = calls.get(i).getAsJsonObject();
                                                String name = c.get("name").getAsString();
                                                JsonObject args = c.getAsJsonObject("args");
                                                String toolResult = executeToolCall(name, args);
                                                JsonObject res = new JsonObject();
                                                res.addProperty("name", name);
                                                try { res.add("result", JsonParser.parseString(toolResult)); }
                                                catch (Exception ex) { res.addProperty("result", toolResult); }
                                                results.add(res);
                                            }
                                            // Synthesize a continuation request by posting the tool_result back as a user message
                                            String continuation = buildToolResultContinuation(results);
                                            // Swap out answerContent and restart completion using the same chat
                                            performContinuation(state, model, continuation);
                                            continue;
                                        }

                                        QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(jsonToParse);
                                        if (parsed != null && parsed.isValid) {
                                            if ("plan".equals(parsed.action)) {
                                                if (actionListener != null) {
                                                    List<ChatMessage.PlanStep> planSteps = QwenResponseParser.toPlanSteps(parsed);
                                                    actionListener.onAiActionsProcessed(trueRawResponse, parsed.explanation, new ArrayList<>(), new ArrayList<>(), planSteps, model.getDisplayName());
                                                }
                                            } else if (parsed.action != null && parsed.action.contains("file")) {
                                                List<ChatMessage.FileActionDetail> details = QwenResponseParser.toFileActionDetails(parsed);
                                                enrichFileActionDetails(details);
                                                if (actionListener != null) notifyAiActionsProcessed(trueRawResponse, parsed.explanation, new ArrayList<>(), details, model.getDisplayName(), thinkingContent.toString(), webSources);
                                            } else {
                                                if (actionListener != null) notifyAiActionsProcessed(trueRawResponse, parsed.explanation, new ArrayList<>(), new ArrayList<>(), model.getDisplayName(), thinkingContent.toString(), webSources);
                                            }
                                        } else {
                                            if (actionListener != null) notifyAiActionsProcessed(trueRawResponse, finalContent, new ArrayList<>(), new ArrayList<>(), model.getDisplayName(), thinkingContent.toString(), webSources);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to parse extracted JSON, treating as text.", e);
                                        if (actionListener != null) notifyAiActionsProcessed(trueRawResponse, finalContent, new ArrayList<>(), new ArrayList<>(), model.getDisplayName(), thinkingContent.toString(), webSources);
                                    }
                                } else {
                                    if (actionListener != null) notifyAiActionsProcessed(trueRawResponse, finalContent, new ArrayList<>(), new ArrayList<>(), model.getDisplayName(), thinkingContent.toString(), webSources);
                                }

                                // Notify listener to save the updated state (final)
                                if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing stream data chunk", e);
                    if (actionListener != null) actionListener.onAiError("Stream error: " + e.getMessage());
                    break;
                }
            }
        }
        if (actionListener != null) actionListener.onAiRequestCompleted();
    }

    private String buildToolResultContinuation(JsonArray results) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "tool_result");
        payload.add("results", results);
        return payload.toString();
    }

    private void performContinuation(QwenConversationState state, AIModel model, String toolResultJson) throws IOException {
        // Continue the same conversation by sending tool_result as a user message
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("stream", true);
        requestBody.addProperty("incremental_output", true);
        requestBody.addProperty("chat_id", state.getConversationId());
        requestBody.addProperty("chat_mode", "normal");
        requestBody.addProperty("model", model.getModelId());
        requestBody.addProperty("parent_id", state.getLastParentId());
        requestBody.addProperty("timestamp", System.currentTimeMillis());

        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", "```json\n" + toolResultJson + "\n```\n");
        msg.addProperty("user_action", "chat");
        msg.add("files", new JsonArray());
        msg.addProperty("timestamp", System.currentTimeMillis());
        JsonArray modelsArray = new JsonArray();
        modelsArray.add(model.getModelId());
        msg.add("models", modelsArray);
        msg.addProperty("chat_type", "t2t");
        JsonObject featureConfig = new JsonObject();
        featureConfig.addProperty("thinking_enabled", false);
        featureConfig.addProperty("output_schema", "phase");
        msg.add("feature_config", featureConfig);
        msg.addProperty("fid", java.util.UUID.randomUUID().toString());
        msg.add("childrenIds", new JsonArray());
        messages.add(msg);
        requestBody.add("messages", messages);

        String qwenToken = ensureMidToken(false);
        Request request = new Request.Builder()
            .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
            .headers(buildQwenHeaders(qwenToken, state.getConversationId()))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                processQwenStreamResponse(response, state, model);
            }
        }
    }

    private JsonObject createSystemMessage(List<ToolSpec> enabledTools) {
        return PromptManager.createSystemMessage(enabledTools);
    }

    private JsonObject createUserMessage(String message, AIModel model, boolean thinkingModeEnabled, boolean webSearchEnabled) {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.addProperty("content", message);
        messageObj.addProperty("user_action", "chat");
        messageObj.add("files", new JsonArray());
        messageObj.addProperty("timestamp", System.currentTimeMillis());
        JsonArray modelsArray = new JsonArray();
        modelsArray.add(model.getModelId());
        messageObj.add("models", modelsArray);
        messageObj.addProperty("chat_type", webSearchEnabled ? "search" : "t2t");
        JsonObject featureConfig = new JsonObject();
        featureConfig.addProperty("thinking_enabled", thinkingModeEnabled);
        featureConfig.addProperty("output_schema", "phase");
        if (webSearchEnabled) {
            featureConfig.addProperty("search_version", "v2");
        }
        if (thinkingModeEnabled) {
            featureConfig.addProperty("thinking_budget", 38912);
        }
        messageObj.add("feature_config", featureConfig);
        messageObj.addProperty("fid", java.util.UUID.randomUUID().toString());
        messageObj.add("parentId", null); // This should be set in the main request body, not here
        messageObj.add("childrenIds", new JsonArray());
        return messageObj;
    }

    private okhttp3.Headers buildQwenHeaders(String midtoken, String conversationId) {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder()
                .add("Authorization", "Bearer")
                .add("Content-Type", "application/json")
                .add("Accept", "*/*")
                .add("bx-umidtoken", midtoken)
                .add("bx-v", QWEN_BX_V)
                .add("Accept-Language", "en-US,en;q=0.9")
                .add("Connection", "keep-alive")
                .add("Origin", "https://chat.qwen.ai")
                .add("Sec-Fetch-Dest", "empty")
                .add("Sec-Fetch-Mode", "cors")
                .add("Sec-Fetch-Site", "same-origin")
                .add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .add("Source", "web");

        if (conversationId != null) {
            builder.add("Referer", "https://chat.qwen.ai/c/" + conversationId);
        } else {
            builder.add("Referer", "https://chat.qwen.ai/");
        }

        return builder.build();
    }

    private synchronized String ensureMidToken() throws IOException {
        return ensureMidToken(false);
    }

    private synchronized String ensureMidToken(boolean forceRefresh) throws IOException {
        if (forceRefresh) {
            Log.w(TAG, "Force refreshing midtoken and clearing cookies");
            this.midToken = null;
            this.midTokenUses = 0;
            sharedPreferences.edit().remove(QWEN_MIDTOKEN_KEY).apply();
            try { cookieStoreRef.clear(); } catch (Exception ignore) {}
        }
        if (midToken != null) {
            midTokenUses++;
            Log.i(TAG, "Reusing midtoken. Use count: " + midTokenUses);
            return midToken;
        }

        Log.i(TAG, "No active midtoken. Fetching a new one...");
        Request req = new Request.Builder()
                .url("https://sg-wum.alibaba.com/w/wu.json")
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .addHeader("Accept", "*/*")
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new IOException("Failed to fetch midtoken: HTTP " + (resp != null ? resp.code() : 0));
            }
            String text = resp.body().string();
            Matcher m = MIDTOKEN_PATTERN.matcher(text);
            if (!m.find()) {
                throw new IOException("Failed to extract bx-umidtoken");
            }
            midToken = m.group(1);
            midTokenUses = 1;

            // Save to SharedPreferences
            sharedPreferences.edit().putString(QWEN_MIDTOKEN_KEY, midToken).apply();
            Log.i(TAG, "Obtained and saved new midtoken. Use count: 1");
            return midToken;
        }
    }

    private void invalidateMidToken() {
        Log.w(TAG, "Invalidating current midtoken due to a rate limit error.");
        this.midToken = null;
        this.midTokenUses = 0;
        sharedPreferences.edit().remove(QWEN_MIDTOKEN_KEY).apply();
        try { cookieStoreRef.clear(); } catch (Exception ignore) {}
    }

    private String executeToolCall(String name, JsonObject args) {
        JsonObject result = new JsonObject();
        try {
            switch (name) {
                case "listProjectTree": {
                    String path = args.has("path") ? args.get("path").getAsString() : ".";
                    int depth = args.has("depth") ? Math.max(0, Math.min(5, args.get("depth").getAsInt())) : 2;
                    int maxEntries = args.has("maxEntries") ? Math.max(10, Math.min(2000, args.get("maxEntries").getAsInt())) : 500;
                    String tree = FileOps.buildFileTree(new java.io.File(projectDir, path), depth, maxEntries);
                    result.addProperty("ok", true);
                    result.addProperty("tree", tree);
                    break;
                }
                case "searchInProject": {
                    String query = args.get("query").getAsString();
                    int maxResults = args.has("maxResults") ? Math.max(1, Math.min(2000, args.get("maxResults").getAsInt())) : 100;
                    boolean regex = args.has("regex") && args.get("regex").getAsBoolean();
                    JsonArray matches = FileOps.searchInProject(projectDir, query, maxResults, regex);
                    result.addProperty("ok", true);
                    result.add("matches", matches);
                    break;
                }
                case "createFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    FileOps.createFile(projectDir, path, content);
                    result.addProperty("ok", true);
                    result.addProperty("message", "File created: " + path);
                    break;
                }
                case "updateFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    FileOps.updateFile(projectDir, path, content);
                    result.addProperty("ok", true);
                    result.addProperty("message", "File updated: " + path);
                    break;
                }
                case "deleteFile": {
                    String path = args.get("path").getAsString();
                    boolean deleted = FileOps.deleteRecursively(new java.io.File(projectDir, path));
                    result.addProperty("ok", deleted);
                    result.addProperty("message", "Deleted: " + path);
                    break;
                }
                case "renameFile": {
                    String oldPath = args.get("oldPath").getAsString();
                    String newPath = args.get("newPath").getAsString();
                    boolean ok = FileOps.renameFile(projectDir, oldPath, newPath);
                    result.addProperty("ok", ok);
                    result.addProperty("message", "Renamed to: " + newPath);
                    break;
                }
                case "fixLint": {
                    String path = args.get("path").getAsString();
                    boolean aggressive = args.has("aggressive") && args.get("aggressive").getAsBoolean();
                    String fixed = FileOps.autoFix(projectDir, path, aggressive);
                    if (fixed == null) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "File not found");
                        break;
                    }
                    FileOps.updateFile(projectDir, path, fixed);
                    result.addProperty("ok", true);
                    result.addProperty("message", "Applied basic lint fixes");
                    break;
                }
                case "readFile": {
                    String path = args.get("path").getAsString();
                    String content = FileOps.readFile(projectDir, path);
                    if (content == null) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "File not found: " + path);
                    } else {
                        result.addProperty("ok", true);
                        result.addProperty("content", content);
                        result.addProperty("message", "File read: " + path);
                    }
                    break;
                }
                case "listFiles": {
                    String path = args.get("path").getAsString();
                    java.io.File dir = new java.io.File(projectDir, path);
                    if (!dir.exists() || !dir.isDirectory()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "Directory not found: " + path);
                    } else {
                        JsonArray files = new JsonArray();
                        java.io.File[] fileList = dir.listFiles();
                        if (fileList != null) {
                            for (java.io.File f : fileList) {
                                JsonObject fileInfo = new JsonObject();
                                fileInfo.addProperty("name", f.getName());
                                fileInfo.addProperty("type", f.isDirectory() ? "directory" : "file");
                                fileInfo.addProperty("size", f.length());
                                files.add(fileInfo);
                            }
                        }
                        result.addProperty("ok", true);
                        result.add("files", files);
                        result.addProperty("message", "Directory listed: " + path);
                    }
                    break;
                }
                case "readUrlContent": {
                    String url = args.get("url").getAsString();
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("Accept", "*/*")
                            .build();
                    try (Response resp = httpClient.newCall(request).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            String content = resp.body().string();
                            String type = resp.header("Content-Type", "");
                            // Truncate overly large responses to keep UI responsive
                            int max = 200_000;
                            if (content.length() > max) content = content.substring(0, max);
                            result.addProperty("ok", true);
                            result.addProperty("content", content);
                            result.addProperty("contentType", type);
                            result.addProperty("status", resp.code());
                        } else {
                            result.addProperty("ok", false);
                            result.addProperty("error", "HTTP " + (resp != null ? resp.code() : 0));
                        }
                    }
                    break;
                }
                case "grepSearch": {
                    String query = args.get("query").getAsString();
                    String relPath = args.has("path") ? args.get("path").getAsString() : ".";
                    boolean isRegex = args.has("isRegex") && args.get("isRegex").getAsBoolean();
                    boolean caseInsensitive = args.has("caseInsensitive") && args.get("caseInsensitive").getAsBoolean();

                    java.io.File start = new java.io.File(projectDir, relPath);
                    if (!start.exists()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "Path not found: " + relPath);
                        break;
                    }

                    int flags = caseInsensitive ? java.util.regex.Pattern.CASE_INSENSITIVE : 0;
                    java.util.regex.Pattern pattern = isRegex
                            ? java.util.regex.Pattern.compile(query, flags)
                            : java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(query), flags);

                    JsonArray matches = new JsonArray();
                    final int maxMatches = 2000;
                    grepWalk(start, projectDir, pattern, matches, new int[]{0}, maxMatches);
                    result.addProperty("ok", true);
                    result.add("matches", matches);
                    break;
                }
                default:
                    result.addProperty("ok", false);
                    result.addProperty("error", "Unknown tool: " + name);
            }
        } catch (Exception ex) {
            result.addProperty("ok", false);
            result.addProperty("error", ex.getMessage());
        }
        return result.toString();
    }
    
    // Helper methods to support grepSearch
    private void grepWalk(java.io.File file, java.io.File projectRoot, java.util.regex.Pattern pattern,
                          com.google.gson.JsonArray outMatches, int[] count, int maxMatches) {
        if (count[0] >= maxMatches || file == null || !file.exists()) return;
        if (file.isDirectory()) {
            String name = file.getName();
            if (shouldSkipDir(name)) return;
            java.io.File[] children = file.listFiles();
            if (children == null) return;
            for (java.io.File c : children) {
                if (count[0] >= maxMatches) break;
                grepWalk(c, projectRoot, pattern, outMatches, count, maxMatches);
            }
            return;
        }

        // Skip large/binary files
        long maxSize = 2_000_000; // 2MB
        if (file.length() > maxSize || looksBinary(file)) return;

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (pattern.matcher(line).find()) {
                    com.google.gson.JsonObject m = new com.google.gson.JsonObject();
                    m.addProperty("file", relPath(projectRoot, file));
                    m.addProperty("line", lineNo);
                    // Limit line length in output
                    String text = line;
                    int maxLen = 500;
                    if (text.length() > maxLen) text = text.substring(0, maxLen);
                    m.addProperty("text", text);
                    outMatches.add(m);
                    count[0]++;
                    if (count[0] >= maxMatches) break;
                }
            }
        } catch (Exception ignored) { }
    }

    private boolean shouldSkipDir(String name) {
        if (name == null) return true;
        String n = name.toLowerCase();
        return n.equals(".git") || n.equals(".gradle") || n.equals("build") || n.equals("dist") ||
               n.equals("node_modules") || n.equals(".idea") || n.equals("out") || n.equals(".next") ||
               n.equals(".nuxt") || n.equals("target");
    }

    private boolean looksBinary(java.io.File f) {
        // Heuristic: read first 4096 bytes; if there are NULs or many non-text chars, treat as binary
        int sample = 4096;
        byte[] buf = new byte[sample];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            int read = fis.read(buf);
            if (read <= 0) return false;
            int nonText = 0;
            for (int i = 0; i < read; i++) {
                int b = buf[i] & 0xFF;
                if (b == 0) return true; // NUL byte
                // Allow common text control chars: tab, CR, LF, FF
                if (b < 0x09 || (b > 0x0D && b < 0x20)) nonText++;
            }
            return nonText > read * 0.3; // >30% suspicious
        } catch (Exception e) {
            return false;
        }
    }

    private String relPath(java.io.File root, java.io.File file) {
        try {
            String rp = root.getCanonicalPath();
            String fp = file.getCanonicalPath();
            if (fp.startsWith(rp)) {
                String r = fp.substring(rp.length());
                if (r.startsWith(java.io.File.separator)) r = r.substring(1);
                return r.replace('\\', '/');
            }
        } catch (Exception ignored) {}
        return file.getPath().replace('\\', '/');
    }


    private String extractJsonFromCodeBlock(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // Look for ```json ... ``` pattern
        String jsonPattern = "```json\\s*([\\s\\S]*?)```";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Also check for ``` ... ``` pattern (without json specifier)
        String genericPattern = "```\\s*([\\s\\S]*?)```";
        pattern = java.util.regex.Pattern.compile(genericPattern);
        matcher = pattern.matcher(content);

        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            // Check if the extracted content looks like JSON
            if (QwenResponseParser.looksLikeJson(extracted)) {
                return extracted;
            }
        }

        return null;
    }

    private void processFileOperationsFromParsedResponse(QwenResponseParser.ParsedResponse parsedResponse, String modelDisplayName) {
        try {
            List<ChatMessage.FileActionDetail> fileActions = QwenResponseParser.toFileActionDetails(parsedResponse);
            enrichFileActionDetails(fileActions);
            if (actionListener != null) {
                actionListener.onAiActionsProcessed(
                        null,
                        parsedResponse.explanation,
                        new ArrayList<>(),
                        fileActions,
                        modelDisplayName
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing file operations", e);
            if (actionListener != null) {
                actionListener.onAiError("Error processing file operations: " + e.getMessage());
            }
        }
    }

    private void executeFileOperation(ChatMessage.FileActionDetail actionDetail) throws Exception {
        if (projectDir == null) {
            throw new IllegalStateException("Project directory not set");
        }

        AiProcessor processor = new AiProcessor(projectDir, context);
        processor.applyFileAction(actionDetail);
    }

    public List<AIModel> fetchModels() {
        String mid;
        try {
            mid = ensureMidToken();
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch midtoken for models", e);
            return Collections.emptyList();
        }
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/models")
                .headers(buildQwenHeaders(mid, null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                List<AIModel> models = new ArrayList<>();
                if (responseJson.has("data")) {
                    if (responseJson.get("data").isJsonArray()) {
                        JsonArray data = responseJson.getAsJsonArray("data");
                        for (int i = 0; i < data.size(); i++) {
                            JsonObject modelData = data.get(i).getAsJsonObject();
                            AIModel model = parseModelData(modelData);
                            if (model != null) {
                                models.add(model);
                            }
                        }
                    } else if (responseJson.get("data").isJsonObject()) {
                        // Handle the case where 'data' is a single object
                        JsonObject modelData = responseJson.getAsJsonObject("data");
                        AIModel model = parseModelData(modelData);
                        if (model != null) {
                            models.add(model);
                        }
                    }
                }
                return models;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching Qwen models", e);
        }
        return Collections.emptyList();
    }

    private AIModel parseModelData(JsonObject modelData) {
        try {
            String modelId = modelData.get("id").getAsString();
            String displayName = modelData.has("name") ? modelData.get("name").getAsString() : modelId;
            
            JsonObject info = modelData.getAsJsonObject("info");
            JsonObject meta = info.getAsJsonObject("meta");
            JsonObject capabilitiesJson = meta.getAsJsonObject("capabilities");

            // Parse basic capabilities
            boolean supportsThinking = capabilitiesJson.has("thinking") && capabilitiesJson.get("thinking").getAsBoolean();
            boolean supportsThinkingBudget = capabilitiesJson.has("thinking_budget") && capabilitiesJson.get("thinking_budget").getAsBoolean();
            boolean supportsVision = capabilitiesJson.has("vision") && capabilitiesJson.get("vision").getAsBoolean();
            boolean supportsDocument = capabilitiesJson.has("document") && capabilitiesJson.get("document").getAsBoolean();
            boolean supportsVideo = capabilitiesJson.has("video") && capabilitiesJson.get("video").getAsBoolean();
            boolean supportsAudio = capabilitiesJson.has("audio") && capabilitiesJson.get("audio").getAsBoolean();
            boolean supportsCitations = capabilitiesJson.has("citations") && capabilitiesJson.get("citations").getAsBoolean();

            // Parse chat types and check for web search
            JsonArray chatTypes = meta.has("chat_type") ? meta.get("chat_type").getAsJsonArray() : new JsonArray();
            boolean supportsWebSearch = false;
            List<String> supportedChatTypes = new ArrayList<>();
            for (int j = 0; j < chatTypes.size(); j++) {
                String chatType = chatTypes.get(j).getAsString();
                supportedChatTypes.add(chatType);
                if ("search".equals(chatType)) {
                    supportsWebSearch = true;
                }
            }

            // Parse MCP tools
            List<String> mcpTools = new ArrayList<>();
            if (meta.has("mcp")) {
                JsonArray mcpArray = meta.get("mcp").getAsJsonArray();
                for (int j = 0; j < mcpArray.size(); j++) {
                    mcpTools.add(mcpArray.get(j).getAsString());
                }
            }
            boolean supportsMCP = !mcpTools.isEmpty();

            // Parse modalities
            List<String> supportedModalities = new ArrayList<>();
            if (meta.has("modality")) {
                JsonArray modalityArray = meta.get("modality").getAsJsonArray();
                for (int j = 0; j < modalityArray.size(); j++) {
                    supportedModalities.add(modalityArray.get(j).getAsString());
                }
            }

            // Parse context and generation limits
            int maxContextLength = meta.has("max_context_length") ? meta.get("max_context_length").getAsInt() : 0;
            int maxGenerationLength = meta.has("max_generation_length") ? meta.get("max_generation_length").getAsInt() : 0;
            int maxThinkingGenerationLength = meta.has("max_thinking_generation_length") ? meta.get("max_thinking_generation_length").getAsInt() : 0;
            int maxSummaryGenerationLength = meta.has("max_summary_generation_length") ? meta.get("max_summary_generation_length").getAsInt() : 0;

            // Parse file limits
            Map<String, Integer> fileLimits = new HashMap<>();
            if (meta.has("file_limits")) {
                JsonObject fileLimitsJson = meta.getAsJsonObject("file_limits");
                for (String key : fileLimitsJson.keySet()) {
                    fileLimits.put(key, fileLimitsJson.get(key).getAsInt());
                }
            }

            // Parse abilities (numeric levels)
            Map<String, Integer> abilities = new HashMap<>();
            if (meta.has("abilities")) {
                JsonObject abilitiesJson = meta.getAsJsonObject("abilities");
                for (String key : abilitiesJson.keySet()) {
                    abilities.put(key, abilitiesJson.get(key).getAsInt());
                }
            }

            // Parse single round flag
            boolean isSingleRound = meta.has("is_single_round") ? meta.get("is_single_round").getAsInt() == 1 : false;

            // Create enhanced capabilities
            ModelCapabilities capabilities = new ModelCapabilities(
                supportsThinking, supportsWebSearch, supportsVision, supportsDocument,
                supportsVideo, supportsAudio, supportsCitations, supportsThinkingBudget,
                supportsMCP, isSingleRound, maxContextLength, maxGenerationLength,
                maxThinkingGenerationLength, maxSummaryGenerationLength, fileLimits,
                supportedModalities, supportedChatTypes, mcpTools, abilities
            );

            return new AIModel(modelId, displayName, AIProvider.ALIBABA, capabilities);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing model data", e);
            return null;
        }
    }

    

    private void notifyAiActionsProcessed(String rawAiResponseJson,
                                          String explanation,
                                          List<String> suggestions,
                                          List<ChatMessage.FileActionDetail> fileActions,
                                          String modelDisplayName,
                                          String thinking,
                                          List<WebSource> sources) {
        // Prefer the richer handler in AiAssistantManager when available
        if (actionListener instanceof AiAssistantManager) {
            ((AiAssistantManager) actionListener).onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, fileActions, modelDisplayName, thinking, sources);
        } else {
            // Fallback to legacy interface without separate thinking/sources
            String fallback = ResponseUtils.buildExplanationWithThinking(explanation, thinking);
            actionListener.onAiActionsProcessed(rawAiResponseJson, fallback, suggestions, fileActions, modelDisplayName);
        }
    }

    // Enrich proposed file actions with old/new content to enable accurate diff previews
    private void enrichFileActionDetails(List<ChatMessage.FileActionDetail> details) {
        if (details == null || projectDir == null) return;
        for (ChatMessage.FileActionDetail d : details) {
            try {
                switch (d.type) {
                    case "createFile": {
                        d.oldContent = "";
                        if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.newContent != null ? d.newContent : "";
                        if (d.newContent.isEmpty() && d.replaceWith != null) d.newContent = d.replaceWith; // fallback
                        break;
                    }
                    case "updateFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.newContent != null ? d.newContent : d.replaceWith != null ? d.replaceWith : d.newContent;
                        if (d.newContent == null) d.newContent = d.oldContent; // no change fallback
                        break;
                    }
                    case "searchAndReplace": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                        String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                        d.newContent = com.codex.apk.util.FileOps.applySearchReplace(old, pattern, repl);
                        break;
                    }
                    case "smartUpdate": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        String mode = d.updateType != null ? d.updateType : "full";
                        if ("append".equals(mode)) {
                            d.newContent = (old != null ? old : "") + (d.newContent != null ? d.newContent : d.contentType != null ? d.contentType : "");
                        } else if ("prepend".equals(mode)) {
                            d.newContent = (d.newContent != null ? d.newContent : "") + (old != null ? old : "");
                        } else if ("replace".equals(mode)) {
                            String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                            String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                            d.newContent = com.codex.apk.util.FileOps.applySearchReplace(old, pattern, repl);
                        } else {
                            // full or unknown
                            if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.replaceWith != null ? d.replaceWith : "";
                        }
                        break;
                    }
                    case "deleteFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        d.newContent = "";
                        break;
                    }
                    case "renameFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.oldPath));
                        d.oldContent = old;
                        d.newContent = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.newPath));
                        break;
                    }
                    case "modifyLines": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        d.newContent = com.codex.apk.util.FileOps.applyModifyLines(old, d.startLine, d.deleteCount, d.insertLines);
                        break;
                    }
                    case "patchFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        // Applying a unified diff is non-trivial; leave newContent empty to rely on diffPatch
                        break;
                    }
                    default: {
                        // No-op
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to enrich action detail for path " + d.path + ": " + e.getMessage());
            }
        }
    }

    private String getProjectStateKey() {
        if (projectDir == null) return null;
        // Sanitize the path to make it a valid preferences key
        return QWEN_CONVERSATION_STATE_KEY_PREFIX + projectDir.getAbsolutePath().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void saveConversationState(QwenConversationState state) {
        String key = getProjectStateKey();
        if (key == null || state == null) return;

        try {
            String jsonState = gson.toJson(state);
            sharedPreferences.edit().putString(key, jsonState).apply();
            Log.i(TAG, "Saved conversation state for project: " + projectDir.getName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save conversation state.", e);
        }
    }

    private QwenConversationState loadConversationState() {
        String key = getProjectStateKey();
        if (key == null) return new QwenConversationState();

        String jsonState = sharedPreferences.getString(key, null);
        if (jsonState != null) {
            try {
                Log.i(TAG, "Loaded conversation state for project: " + projectDir.getName());
                return gson.fromJson(jsonState, QwenConversationState.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load/parse conversation state.", e);
                // Fallback to a new state if parsing fails
                return new QwenConversationState();
            }
        }
        return new QwenConversationState();
    }
}
