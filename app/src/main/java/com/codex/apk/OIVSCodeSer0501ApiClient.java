package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import java.util.Random;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OIVSCodeSer0501ApiClient extends AnyProviderApiClient {
    private static final String API_ENDPOINT = "https://oi-vscode-server-0501.onrender.com/v1/chat/completions";
    private final Random random = new Random();

    public OIVSCodeSer0501ApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        super(context, actionListener);
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            Response response = null;
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                JsonObject body = buildOpenAIStyleBody(model.getModelId(), message, history, thinkingModeEnabled);

                String userid = generateUserId();

                Request request = new Request.Builder()
                        .url(API_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "text/event-stream")
                        .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                        .addHeader("userid", userid)
                        .build();

                response = httpClient.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errBody = null;
                    try { if (response != null && response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
                    String snippet = errBody != null ? (errBody.length() > 400 ? errBody.substring(0, 400) + "..." : errBody) : null;
                    if (actionListener != null) actionListener.onAiError("API request failed: " + (response != null ? response.code() : -1) + (snippet != null ? (" | body: " + snippet) : ""));
                    return;
                }

                StringBuilder finalText = new StringBuilder();
                StringBuilder rawSse = new StringBuilder();
                streamOpenAiSse(response, finalText, rawSse);

                if (finalText.length() > 0) {
                    if (actionListener != null) {
                        actionListener.onAiActionsProcessed(rawSse.toString(), finalText.toString(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), model.getDisplayName());
                    }
                } else {
                    if (actionListener != null) actionListener.onAiError("No response from provider");
                }
            } catch (Exception e) {
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                try { if (response != null) response.close(); } catch (Exception ignore) {}
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    private String generateUserId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(21);
        for (int i = 0; i < 21; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
