package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CohereApiClient implements ApiClient {
    private static final String TAG = "CohereApiClient";
    private static final String BASE_URL = "https://coherelabs-c4ai-command.hf.space";
    private static final String CONVERSATION_URL = BASE_URL + "/conversation";

    private final OkHttpClient httpClient;
    private final AIAssistant.AIActionListener actionListener;
    private final Gson gson = new Gson();

    public CohereApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public List<AIModel> fetchModels() {
        // Models are hardcoded as per the python script
        java.util.List<AIModel> models = new java.util.ArrayList<>();
        models.add(new AIModel("command-a-03-2025", "command-a-03-2025", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("command-r-plus-08-2024", "command-r-plus-08-2024", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("command-r-08-2024", "command-r-08-2024", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("command-r-plus", "command-r-plus", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("command-r", "command-r", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("command-r7b-12-2024", "command-r7b-12-2024", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("command-r7b-arabic-02-2025", "command-r7b-arabic-02-2025", AIProvider.COHERE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
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
                convBody.addProperty("preprompt", ""); // System prompt can be added here if needed

                Request convRequest = new Request.Builder()
                        .url(CONVERSATION_URL)
                        .post(RequestBody.create(convBody.toString(), MediaType.parse("application/json")))
                        .addHeader("Origin", BASE_URL)
                        .addHeader("Referer", BASE_URL + "/")
                        .build();

                String conversationId;
                try (Response convResponse = httpClient.newCall(convRequest).execute()) {
                    if (!convResponse.isSuccessful()) {
                        throw new IOException("Failed to create Cohere conversation: " + convResponse);
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
                        throw new IOException("Failed to get Cohere message ID: " + dataResponse);
                    }
                    String dataResponseBody = dataResponse.body().string();
                    // This parsing is complex and brittle, based on the python script's logic
                    try {
                        JsonObject dataJson = JsonParser.parseString(dataResponseBody.split("\n")[0]).getAsJsonObject();
                        JsonArray nodes = dataJson.getAsJsonArray("nodes");
                        if (nodes != null && nodes.size() > 1) {
                            JsonObject node = nodes.get(1).getAsJsonObject();
                            if (node != null && node.has("data")) {
                                JsonArray dataArray = node.getAsJsonArray("data");
                                if (dataArray != null && dataArray.size() > 0) {
                                    JsonObject firstElement = dataArray.get(0).getAsJsonObject();
                                    if (firstElement != null && firstElement.has("id")) {
                                        messageId = firstElement.get("id").getAsString();
                                    }
                                }
                            }
                        }
                        if (messageId == null) {
                            throw new IOException("Could not find message ID in Cohere response structure.");
                        }
                    } catch (Exception e) {
                        throw new IOException("Failed to parse message ID from Cohere response: " + e.getMessage(), e);
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
                        throw new IOException("Failed to send Cohere message: " + msgResponse);
                    }

                    // Process stream
                    // ... (stream processing logic would go here, similar to other clients)
                    // For now, just read the whole body for simplicity
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
}
