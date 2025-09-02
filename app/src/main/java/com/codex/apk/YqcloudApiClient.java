package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import android.util.Log;
import okio.BufferedSource;

public class YqcloudApiClient extends AnyProviderApiClient {
    private static final String API_ENDPOINT = "https://api.binjie.fun/api/generateStream";

    public YqcloudApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        super(context, actionListener);
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            Response response = null;
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                String systemMessage = "";
                JsonArray messages = new JsonArray();
                for (ChatMessage msg : history) {
                    if (msg.getSender() == ChatMessage.SENDER_USER) {
                        messages.add(msg.toJsonObject());
                    } else {
                        messages.add(msg.toJsonObject());
                    }
                }
                if (!history.isEmpty() && history.get(0).getSender() == 0 && history.get(0).getContent().startsWith("System prompt:")) {
                    systemMessage = history.get(0).getContent();
                }

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", message);
                messages.add(userMsg);

                StringBuilder promptBuilder = new StringBuilder();
                for (int i = 0; i < messages.size(); i++) {
                    JsonObject msg = messages.get(i).getAsJsonObject();
                    promptBuilder.append(msg.get("role").getAsString()).append(": ").append(msg.get("content").getAsString()).append("\n");
                }

                JsonObject body = new JsonObject();
                body.addProperty("prompt", promptBuilder.toString());
                body.addProperty("userId", "#/chat/" + System.currentTimeMillis());
                body.addProperty("network", true);
                body.addProperty("system", systemMessage);
                body.addProperty("withoutContext", false);
                body.addProperty("stream", true);

                Request request = new Request.Builder()
                        .url(API_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "application/json, text/plain, */*")
                        .addHeader("accept-language", "en-US,en;q=0.9")
                        .addHeader("content-type", "application/json")
                        .addHeader("origin", "https://chat9.yqcloud.top")
                        .addHeader("referer", "https://chat9.yqcloud.top/")
                        .addHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
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
                streamYqcloudResponse(response, finalText, rawSse);

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

    private void streamYqcloudResponse(Response response, StringBuilder finalText, StringBuilder rawAnswer) throws IOException {
        if (response.body() == null) return;
        BufferedSource source = response.body().source();
        try { source.timeout().timeout(60, TimeUnit.SECONDS); } catch (Exception ignore) {}

        while (true) {
            String line;
            try {
                line = source.readUtf8LineStrict();
            } catch (EOFException eof) { break; }
            catch (java.io.InterruptedIOException timeout) { Log.w("YqcloudApiClient", "Yqcloud read timed out"); break; }
            if (line == null || line.isEmpty()) continue;

            // The response is not SSE, it's just a stream of strings.
            // The python script just yields the line.
            finalText.append(line);
            if (rawAnswer != null) rawAnswer.append(line).append("\n");

            if (actionListener != null) {
                actionListener.onAiStreamUpdate(finalText.toString(), false);
            }
        }
        // Final update
        if (actionListener != null) {
            actionListener.onAiStreamUpdate(finalText.toString(), false);
        }
    }
}
