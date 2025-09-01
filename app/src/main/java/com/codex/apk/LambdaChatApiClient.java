package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LambdaChatApiClient implements ApiClient {
    private static final String TAG = "LambdaChatApiClient";
    private static final String BASE_URL = "https://lambda.chat";
    private static final String CONVERSATION_URL = BASE_URL + "/conversation";

    private final OkHttpClient httpClient;
    private final AIAssistant.AIActionListener actionListener;
    private final Gson gson = new Gson();

    public LambdaChatApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.actionListener = actionListener;
        CookieJar cookieJar = new CookieJar() {
            private final java.util.Map<String, List<Cookie>> cookieStore = new java.util.HashMap<>();
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }
            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                if (cookies == null) {
                    cookies = new java.util.ArrayList<>();
                    cookies.add(new Cookie.Builder().name("hf-chat").value(UUID.randomUUID().toString()).domain("lambda.chat").build());
                }
                return cookies;
            }
        };
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
    }

    @Override
    public List<AIModel> fetchModels() {
        // Models are hardcoded as per the python script
        java.util.List<AIModel> models = new java.util.ArrayList<>();
        models.add(new AIModel("deepseek-llama3.3-70b", "deepseek-llama3.3-70b", AIProvider.LAMBDA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("apriel-5b-instruct", "apriel-5b-instruct", AIProvider.LAMBDA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("hermes-3-llama-3.1-405b-fp8", "hermes-3-llama-3.1-405b-fp8", AIProvider.LAMBDA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("llama3.3-70b-instruct-fp8", "llama3.3-70b-instruct-fp8", AIProvider.LAMBDA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("qwen3-32b-fp8", "qwen3-32b-fp8", AIProvider.LAMBDA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        return models;
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                // Step 1: Create conversation
                JsonObject convBody = new JsonObject();
                convBody.addProperty("model", model.getModelId());

                Request convRequest = new Request.Builder()
                        .url(CONVERSATION_URL)
                        .post(RequestBody.create(convBody.toString(), MediaType.parse("application/json")))
                        .addHeader("Origin", BASE_URL)
                        .addHeader("Referer", BASE_URL + "/")
                        .build();

                String conversationId;
                try (Response convResponse = httpClient.newCall(convRequest).execute()) {
                    if (!convResponse.isSuccessful()) {
                        throw new IOException("Failed to create Lambda conversation: " + convResponse);
                    }
                    String convResponseBody = convResponse.body().string();
                    JsonObject convJson = JsonParser.parseString(convResponseBody).getAsJsonObject();
                    conversationId = convJson.get("conversationId").getAsString();
                }

                // Step 2: Get message ID
                Request dataRequest = new Request.Builder()
                        .url(CONVERSATION_URL + "/" + conversationId + "/__data.json?x-sveltekit-invalidated=11")
                        .get()
                        .build();

                String messageId;
                try (Response dataResponse = httpClient.newCall(dataRequest).execute()) {
                    if (!dataResponse.isSuccessful()) {
                        throw new IOException("Failed to get Lambda message ID: " + dataResponse);
                    }
                    String dataResponseBody = dataResponse.body().string();
                    // This parsing is complex and brittle, based on the python script's logic
                    messageId = extractMessageId(dataResponseBody);
                    if (messageId == null) {
                        throw new IOException("Could not find message ID in response");
                    }
                }

                // Step 3: Send message
                JsonObject dataField = new JsonObject();
                dataField.addProperty("inputs", message);
                dataField.addProperty("id", messageId);
                dataField.addProperty("is_retry", false);
                dataField.addProperty("is_continue", false);
                dataField.add("web_search", new JsonArray());
                dataField.add("tools", new JsonArray());

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("data", dataField.toString())
                        .build();

                Request msgRequest = new Request.Builder()
                        .url(CONVERSATION_URL + "/" + conversationId)
                        .post(requestBody)
                        .build();

                try (Response msgResponse = httpClient.newCall(msgRequest).execute()) {
                    if (!msgResponse.isSuccessful()) {
                        throw new IOException("Failed to send Lambda message: " + msgResponse);
                    }

                    // Process stream
                    // ... (stream processing logic would go here)
                    String responseBody = msgResponse.body().string();
                     if (actionListener != null) {
                        actionListener.onAiActionsProcessed(responseBody, responseBody, new java.util.ArrayList<>(), new java.util.ArrayList<>(), model.getDisplayName());
                    }
                }

            } catch (Exception e) {
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    private String extractMessageId(String json) {
        try {
            for (String line : json.split("\n")) {
                if (line.trim().isEmpty()) continue;
                JsonObject dataJson = JsonParser.parseString(line).getAsJsonObject();
                if ("data".equals(dataJson.get("type").getAsString()) && dataJson.has("nodes")) {
                    for (com.google.gson.JsonElement node : dataJson.getAsJsonArray("nodes")) {
                        if ("data".equals(node.getAsJsonObject().get("type").getAsString()) && node.getAsJsonObject().has("data")) {
                            for (com.google.gson.JsonElement item : node.getAsJsonObject().getAsJsonArray("data")) {
                                if (item.isJsonObject() && item.getAsJsonObject().has("from") && "system".equals(item.getAsJsonObject().get("from").getAsString())) {
                                    return item.getAsJsonObject().get("id").getAsString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // fallback to regex
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(json);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
