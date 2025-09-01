package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import java.util.Random;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChataiApiClient extends AnyProviderApiClient {
    private static final String API_ENDPOINT = "https://chatai.aritek.app/stream";
    private final Random random = new Random();

    public ChataiApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        super(context, actionListener);
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            Response response = null;
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                JsonArray messages = new JsonArray();
                for (ChatMessage msg : history) {
                    messages.add(msg.toJsonObject());
                }
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", message);
                messages.add(userMsg);

                JsonObject body = new JsonObject();
                body.addProperty("machineId", generateMachineId());
                body.add("msg", messages);
                body.addProperty("token", "eyJzdWIiOiIyMzQyZmczNHJ0MzR0MzQiLCJuYW1lIjoiSm9objM0NTM0NTM=");
                body.addProperty("type", 0);

                Request request = new Request.Builder()
                        .url(API_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("Accept", "text/event-stream")
                        .addHeader("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 7.1.2; SM-G935F Build/N2G48H)")
                        .addHeader("Host", "chatai.aritek.app")
                        .addHeader("Connection", "Keep-Alive")
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

    private String generateMachineId() {
        StringBuilder part1 = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            part1.append(random.nextInt(10));
        }
        StringBuilder part2 = new StringBuilder(25);
        String chars = "0123456789.";
        for (int i = 0; i < 25; i++) {
            part2.append(chars.charAt(random.nextInt(chars.length())));
        }
        return part1.toString() + "." + part2.toString();
    }
}
