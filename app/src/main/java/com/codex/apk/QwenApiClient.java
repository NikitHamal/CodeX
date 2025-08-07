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
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class QwenApiClient implements ApiClient {
    private static final String TAG = "QwenApiClient";
    private static final String PREFS_NAME = "ai_chat_prefs";
    private static final String QWEN_CONVERSATION_STATE_KEY_PREFIX = "qwen_conv_state_";
    private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";
    private static final String QWEN_DEFAULT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4";
    private static final String QWEN_BX_UA = "231!E3/3FAmU8Mz+joZDE+3YnMEjUq/YvqY2leOxacSC80vTPuB9lMZY9mRWFzrwLEV0PmcfY4rL2JFHeKTCyUQdACIzKZBlsbAyXt6Iz8TQ7ck9CIZgeOBOVaRK66GQqw/G9pMRBb28c0I6pXxeCmhbzzfCtEk+xPyXnSGpo+LsaU/+OPHDQCrVM2z4ya7TrTguCmR87np6YdSH3DIn3jhgnFcEQHlSogvwTYlxfUid3koX0QD3yk8jHFx4EMK5QlFHH3v+++3+qAS+oPts+DQWqi++68uR+K6y+8ix9NvuqCz++I4A+4mYN/A46wSw+KgU++my9k3+2XgETIrb8AIWYA++78Ej+yzIk464F3uHo+4oSn28DfRCb7bb4RdlvedIjC5f8MUt1jGNx1IaH10EiLcJPTR6LPJWUj+1hA2bQgJ5wThA3dmWf7dsh4bWmR1rcU3OV14ljhHOENSBKjzoqihnuxql9adxbf7qHFc6ERi7pfFSMd/92mFibzH2549YNTjfOFvgo+FS1/uN+QpL0WxeXRvcFOwCFuku+u1WTAzJmXLU2obdBrZmsVL+GISL5RDin6H1n6RnV2iLE0SOZlAQT/ccm2CtJ9AhpCquek0adxkY3+TOhSPkW/r2RN+U5SbMBBFWpRqQGE0G8uG8gdRiGM+DhV5nzxB+VDkJpZTnF2C/bS8Lkogquz3Mv9hboXZORvx7WxTEhU3rXpCaVGNHzWIPFXp5shUkyscUlWQq9ZgzkhuFHR8vAwNqWLDCiab6sVoOIP1C9gwo+jAGoxgtAXU0xOWuURnWGG7aemef+Fu4s7FfkGO9kMIal6ScRRKJq+YgiTC6oj6rhJYPEgY9xX+JNv2Cp9TratLC5/7bQCpgO4+BFqW25tBh61NeNVNMS9JTFLysevVVQcfxugYJCGMv6wJ1FYvUgqX/Ag4Y4evHRbWKHp88RhqHXOYNPuBenD1xlAMyNTEOvVCDdCxeDHOzMR7cRSlKUiyGcgA7Kg/Xb9gfN/cu6ve82uefIrQg1b1zfpYgl9lExsVQv6dJPUduyTT3sUwzjlkVPkIxZ0Se5PweURQwVPEAtHYlbPAKjTEmDZ65nvieN96Z/hGl8sTm5YpgeHmDZKK4Qi/4LYK5KIpTEgONMcOqQTWReopT00zJiYw7jcNchb8t9GOTdU0RQLAZnDV8YszRmcd8gSTXrCueqrqdxxmjm1OLnNdSOjczQeyG1h/FRUXgsog9WEp1ggdbuFm3xGcHPcYaA95f6szELKvjRGPEu1gdlUYxBPQ3sWMBE152VWjWNd8SVFUrmWDizlmc0QzlmnzXa2CpNJJMMibqYd3bZ2aOENvhhXgjuRgDv5K46hVP/N2xaM/GYJgPfP1D+JnS7LPhnIUCSoTvrKwabbVOisan8s7AGz1Xse5ocJiEsXhsqSQqTaDNTWLvHxkgQYmOIRuKAeAdyUx0SfwgawTqNMC/mnbGQi/RUKwg69RqfJBYFI3SChkgl9xX9mp+ni1XrPFGSonRl4V4LuUsE7XIs5U4EDAhSJfzh+5KhRk=";
    private static final String QWEN_BX_UMIDTOKEN = "T2gAJllnldFiRL-u8mC_CoRJu4UkeSmmDyAGdRWHSDDtxGpwCLykAm6gn7JppTggooY=";
    private static final String QWEN_BX_V = "2.5.31";
    private static final String QWEN_COOKIE = "_gcl_au=1.1.1766988768.1752579263; _bl_uid=nXm6qd3q4zpg6tgm02909kvmFUpm; acw_tc=0a03e54317532565292955177e493bd17cb6ab0297793d17257e4afc7bf42b; x-ap=ap-southeast-1; token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4; tfstk=g26ZgsaVhdo2fDL9s99qzj0MwtJ9pKzS_tTXmijDfFYgWt92meSq51sfmKy2-dAA1ET6uKSAzka7F8sOXKpgPzw7wJZtXLHiIn0Xxpxv3h4mOM1lXKp0RbHESRSOW-xr1Gv0YpxXDjvDjhm3Yn-yoKYMi2cHqevDnIDmx2xJqhDDmKqFxeKXnEbDskRHJnxt_a_0zhdgx9OWGMnuVCYljekmEV-9sUeJ5xDfIIBvrGVxnxXebCBHdIJMEK5c2sJDLrlvo9LVIsSUJfYGB9IW5ta-GFjCtBX99mZ9o1jCLQ63qX8fw9W26TzI3E55A9RFOgWqkHXCttBYHjAMvH87Yko6Tuw5pVSFyjhv6C-ePkcoMjdMvH87YklxMCyeYUZnZ; isg=BP7-CDNoGikWBk775LCGxejTTxZAP8K5TbnYJKgHacE8S5klEs5CyL4txkkhzbrR; ssxmod_itna=eq0xcDgCGQYDq4e9igDmhxnD3q7u40dGM9Deq7tdGcD8Ox0PGOzojD5DU2Yz2Ak52qqGRmgKD/KQCqDy7xA3DTx+ajQq5nxvqq35mCxteqDPLwwweCngAOnBKmgY8nUTXUZgw0=KqeDIDY=IDAtD0qDi+DDgDA=DjwDD7qe4DxDADB=bFeDLDi3eVQTDtw0=ieGwDAY4BOhwDYEKwGnxwDDS4QTIieDf9DG2DD=IRWRbqCwTDOxgCKe589bS3Th0BR3VRYIjSYq4SgIA5H8D8+lxm9YUqocQdabWwpEGsERk7wUgILQCFBQ/GD+xe7r5l05oQKiAGxgkVuDhi+YiDD; ssxmod_itna2=eq0xcDgCGQYDq4e9igDmhxnD3q7u40dGM9Deq7tdGcD8Ox0PGOzojD5DU2Yz2Ak52qqGRmxeGIDgDn6Pq+Ee03t1Q6TnxtwxGXxT5W12cxqQj6SG+THGZOQ412fzxk4BtN=FjAO01cDxOPy4S2vsrri5BxIH1iD8Bj01z27Wt4g1aEyaODFW2DAq26osz+i53rvxinaO+Si+6/er3aMigjTNVlTQiWMbqOmq4D";

    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final AIAssistant.AIActionListener actionListener;

    private final File projectDir;

    public QwenApiClient(Context context, AIAssistant.AIActionListener actionListener, File projectDir) {
        this.context = context;
        this.actionListener = actionListener;
        this.projectDir = projectDir;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
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

        String qwenToken = getQwenToken();
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

        // Add the current user message
        messages.add(createUserMessage(userMessage, model, thinkingModeEnabled, webSearchEnabled));

        requestBody.add("messages", messages);

        if (!enabledTools.isEmpty()) {
            requestBody.add("tools", ToolSpec.toJsonArray(enabledTools));
            JsonObject toolChoice = new JsonObject();
            toolChoice.addProperty("type", "auto");
            requestBody.add("tool_choice", toolChoice);
        }

        String qwenToken = getQwenToken();
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + state.getConversationId())
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .headers(buildQwenHeaders(qwenToken, state.getConversationId()))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                processQwenStreamResponse(response, state, model);
            } else {
                if (actionListener != null) actionListener.onAiError("Failed to send message");
            }
        }
    }

    private void processQwenStreamResponse(Response response, QwenConversationState state, AIModel model) throws IOException {
        StringBuilder thinkingContent = new StringBuilder();
        StringBuilder answerContent = new StringBuilder();
        List<WebSource> webSources = new ArrayList<>();

        String line;
        while ((line = response.body().source().readUtf8Line()) != null) {
            if (line.startsWith("data: ")) {
                String jsonData = line.substring(6);
                if (jsonData.trim().isEmpty()) continue;

                try {
                    JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();

                    // Check for conversation state updates
                    if (data.has("response.created")) {
                        JsonObject created = data.getAsJsonObject("response.created");
                        if (created.has("chat_id")) state.setConversationId(created.get("chat_id").getAsString());
                        if (created.has("response_id")) state.setLastParentId(created.get("response_id").getAsString());
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

                            if ("think".equals(phase)) {
                                thinkingContent.append(content);
                                if (actionListener != null) actionListener.onAiStreamUpdate(thinkingContent.toString(), true);
                            } else {
                                answerContent.append(content);
                            }

                            if ("finished".equals(status)) {
                                String finalContent = answerContent.toString();
                                String jsonToParse = extractJsonFromCodeBlock(finalContent);
                                if (jsonToParse == null && QwenResponseParser.looksLikeJson(finalContent)) {
                                    jsonToParse = finalContent;
                                }

                                if (jsonToParse != null) {
                                    try {
                                        QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(jsonToParse);
                                        if (parsed != null && parsed.isValid && parsed.action != null && parsed.action.contains("file")) {
                                            // Convert to details, enrich previews, and include thinking text in explanation
                                            List<ChatMessage.FileActionDetail> details = QwenResponseParser.toFileActionDetails(parsed);
                                            enrichFileActionDetails(details);
                                            String explanation = buildExplanationWithThinking(parsed.explanation, thinkingContent.toString());
                                            if (actionListener != null) actionListener.onAiActionsProcessed(jsonToParse, explanation, parsed.suggestions, details, model.getDisplayName());
                                        } else {
                                            String explanation = parsed != null ? parsed.explanation : "Could not fully parse response.";
                                            explanation = buildExplanationWithThinking(explanation, thinkingContent.toString());
                                            List<String> suggestions = parsed != null ? parsed.suggestions : new ArrayList<>();
                                            if (actionListener != null) actionListener.onAiActionsProcessed(jsonToParse, explanation, suggestions, new ArrayList<>(), model.getDisplayName());
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to parse extracted JSON, treating as text.", e);
                                        String explanation = buildExplanationWithThinking(finalContent, thinkingContent.toString());
                                        if (actionListener != null) actionListener.onAiActionsProcessed(null, explanation, new ArrayList<>(), new ArrayList<>(), model.getDisplayName());
                                    }
                                } else {
                                    String explanation = buildExplanationWithThinking(finalContent, thinkingContent.toString());
                                    if (actionListener != null) actionListener.onAiActionsProcessed(null, explanation, new ArrayList<>(), new ArrayList<>(), model.getDisplayName());
                                }

                                // Notify listener to save the updated state
                                if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing stream data chunk", e);
                }
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

    private okhttp3.Headers buildQwenHeaders(String token, String conversationId) {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder()
                .add("authorization", "Bearer " + token)
                .add("content-type", "application/json")
                .add("accept", "*/*")
                .add("Cookie", QWEN_COOKIE)
                .add("bx-ua", QWEN_BX_UA)
                .add("bx-umidtoken", QWEN_BX_UMIDTOKEN)
                .add("bx-v", QWEN_BX_V)
                .add("Accept-Language", "en-US,en;q=0.9")
                .add("Connection", "keep-alive")
                .add("Origin", "https://chat.qwen.ai")
                .add("Sec-Fetch-Dest", "empty")
                .add("Sec-Fetch-Mode", "cors")
                .add("Sec-Fetch-Site", "same-origin")
                .add("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel A662LM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36")
                .add("sec-ch-ua", "\"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
                .add("sec-ch-ua-mobile", "?1")
                .add("sec-ch-ua-platform", "\"Android\"")
                .add("source", "h5")
                .add("timezone", "Wed Jul 23 2025 13:27:47 GMT+0545")
                .add("x-accel-buffering", "no");

        if (conversationId != null) {
            builder.add("Referer", "https://chat.qwen.ai/c/" + conversationId);
        }

        return builder.build();
    }

    private String getQwenToken() {
        String customToken = SettingsActivity.getQwenApiToken(context);
        return customToken.isEmpty() ? QWEN_DEFAULT_TOKEN : customToken;
    }

    private String executeToolCall(String name, JsonObject args) {
        JsonObject result = new JsonObject();
        try {
            switch (name) {
                case "createFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    file.getParentFile().mkdirs();
                    java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    result.addProperty("ok", true);
                    result.addProperty("message", "File created: " + path);
                    break;
                }
                case "updateFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    result.addProperty("ok", true);
                    result.addProperty("message", "File updated: " + path);
                    break;
                }
                case "deleteFile": {
                    String path = args.get("path").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    boolean deleted = deleteRecursively(file);
                    result.addProperty("ok", deleted);
                    result.addProperty("message", "Deleted: " + path);
                    break;
                }
                case "renameFile": {
                    String oldPath = args.get("oldPath").getAsString();
                    String newPath = args.get("newPath").getAsString();
                    java.io.File oldFile = new java.io.File(projectDir, oldPath);
                    java.io.File newFile = new java.io.File(projectDir, newPath);
                    newFile.getParentFile().mkdirs();
                    boolean ok = oldFile.renameTo(newFile);
                    result.addProperty("ok", ok);
                    result.addProperty("message", "Renamed to: " + newPath);
                    break;
                }
                case "readFile": {
                    String path = args.get("path").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    if (!file.exists()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "File not found: " + path);
                    } else {
                        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                                java.nio.charset.StandardCharsets.UTF_8);
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

    private boolean deleteRecursively(java.io.File f) {
        if (f.isDirectory()) {
            for (java.io.File c : java.util.Objects.requireNonNull(f.listFiles())) {
                deleteRecursively(c);
            }
        }
        return f.delete();
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
                        parsedResponse.suggestions,
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

    private void processFileOperationsFromJson(JsonObject jsonObj) {
        try {
            String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
            List<String> suggestions = new ArrayList<>();
            if (jsonObj.has("suggestions")) {
                JsonArray suggestionsArray = jsonObj.getAsJsonArray("suggestions");
                for (int i = 0; i < suggestionsArray.size(); i++) {
                    suggestions.add(suggestionsArray.get(i).getAsString());
                }
            }
            List<ChatMessage.FileActionDetail> fileActions = new ArrayList<>();
            if (jsonObj.has("operations")) {
                JsonArray operations = jsonObj.getAsJsonArray("operations");
                for (int i = 0; i < operations.size(); i++) {
                    JsonObject operation = operations.get(i).getAsJsonObject();
                    String type = operation.has("type") ? operation.get("type").getAsString() : "";
                    String path = operation.has("path") ? operation.get("path").getAsString() : "";
                    String content = operation.has("content") ? operation.get("content").getAsString() : "";
                    String oldPath = operation.has("oldPath") ? operation.get("oldPath").getAsString() : "";
                    String newPath = operation.has("newPath") ? operation.get("newPath").getAsString() : "";
                    ChatMessage.FileActionDetail actionDetail = new ChatMessage.FileActionDetail(
                            type, path, oldPath, newPath, "", content, 0, 0, null, null, null
                    );
                    fileActions.add(actionDetail);
                }
            }
            // Enrich with previews
            enrichFileActionDetails(fileActions);
            // If there are no operations but the JSON is valid, still notify the UI with explanation/suggestions
            if (actionListener != null) {
                actionListener.onAiActionsProcessed(
                        jsonObj.toString(),
                        explanation,
                        suggestions,
                        fileActions,
                        "Qwen"
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process file operations from JSON", e);
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
        Request request = new Request.Builder()
                .url(QWEN_BASE_URL + "/models")
                .headers(buildQwenHeaders(getQwenToken(), null))
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

    // Build final explanation including thinking content if available
    private String buildExplanationWithThinking(String baseExplanation, String thinking) {
        if (thinking == null || thinking.trim().isEmpty()) return baseExplanation != null ? baseExplanation : "";
        StringBuilder sb = new StringBuilder();
        if (baseExplanation != null && !baseExplanation.trim().isEmpty()) {
            sb.append(baseExplanation.trim()).append("\n\n");
        }
        sb.append("[Thinking]\n").append(thinking.trim());
        return sb.toString();
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
                        String old = readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.newContent != null ? d.newContent : d.replaceWith != null ? d.replaceWith : d.newContent;
                        if (d.newContent == null) d.newContent = d.oldContent; // no change fallback
                        break;
                    }
                    case "searchAndReplace": {
                        String old = readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                        String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                        d.newContent = applySearchReplace(old, pattern, repl);
                        break;
                    }
                    case "smartUpdate": {
                        String old = readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        String mode = d.updateType != null ? d.updateType : "full";
                        if ("append".equals(mode)) {
                            d.newContent = (old != null ? old : "") + (d.newContent != null ? d.newContent : d.contentType != null ? d.contentType : "");
                        } else if ("prepend".equals(mode)) {
                            d.newContent = (d.newContent != null ? d.newContent : "") + (old != null ? old : "");
                        } else if ("replace".equals(mode)) {
                            String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                            String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                            d.newContent = applySearchReplace(old, pattern, repl);
                        } else {
                            // full or unknown
                            if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.replaceWith != null ? d.replaceWith : "";
                        }
                        break;
                    }
                    case "deleteFile": {
                        String old = readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        d.newContent = "";
                        break;
                    }
                    case "renameFile": {
                        String old = readFileSafe(new File(projectDir, d.oldPath));
                        d.oldContent = old;
                        d.newContent = readFileSafe(new File(projectDir, d.newPath));
                        break;
                    }
                    case "modifyLines": {
                        String old = readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        d.newContent = applyModifyLines(old, d.startLine, d.deleteCount, d.insertLines);
                        break;
                    }
                    case "patchFile": {
                        String old = readFileSafe(new File(projectDir, d.path));
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

    private String readFileSafe(File f) {
        try {
            if (f != null && f.exists()) {
                return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String applySearchReplace(String input, String pattern, String replacement) {
        if (input == null) return "";
        if (pattern == null || pattern.isEmpty()) return input;
        String repl = replacement != null ? replacement : "";
        try {
            return input.replaceAll(pattern, repl);
        } catch (Exception e) {
            // Fallback to plain replace if regex fails
            return input.replace(pattern, repl);
        }
    }

    private String applyModifyLines(String content, int startLine, int deleteCount, List<String> insertLines) {
        if (content == null) return "";
        String[] lines = content.split("\n", -1);
        List<String> out = new ArrayList<>();
        for (String l : lines) out.add(l);
        int idx = Math.max(0, Math.min(out.size(), startLine > 0 ? startLine - 1 : 0));
        int toDelete = Math.max(0, Math.min(deleteCount, out.size() - idx));
        for (int i = 0; i < toDelete; i++) {
            out.remove(idx);
        }
        if (insertLines != null && !insertLines.isEmpty()) {
            out.addAll(idx, insertLines);
        }
        return String.join("\n", out);
    }
}
