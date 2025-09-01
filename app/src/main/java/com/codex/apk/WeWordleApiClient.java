package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeWordleApiClient extends AnyProviderApiClient {
    private static final String API_ENDPOINT = "https://wewordle.org/gptapi/v1/web/turbo";

    public WeWordleApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        super(context, actionListener);
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState state, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            Response response = null;
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                JsonObject body = buildOpenAIStyleBody(model.getModelId(), message, history, thinkingModeEnabled);

                Request request = new Request.Builder()
                        .url(API_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "*/*")
                        .addHeader("accept-language", "en-US,en;q=0.9")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("content-type", "application/json")
                        .addHeader("dnt", "1")
                        .addHeader("origin", "https://chat-gpt.com")
                        .addHeader("pragma", "no-cache")
                        .addHeader("priority", "u=1, i")
                        .addHeader("referer", "https://chat-gpt.com/")
                        .addHeader("sec-ch-ua", "\"Not.A/Brand\";v=\"99\", \"Chromium\";v=\"136\"")
                        .addHeader("sec-ch-ua-mobile", "?0")
                        .addHeader("sec-ch-ua-platform", "\"Linux\"")
                        .addHeader("sec-fetch-dest", "empty")
                        .addHeader("sec-fetch-mode", "cors")
                        .addHeader("sec-fetch-site", "cross-site")
                        .addHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                        .build();

                response = httpClient.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errBody = null;
                    try { if (response != null && response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
                    String snippet = errBody != null ? (errBody.length() > 400 ? errBody.substring(0, 400) + "..." : errBody) : null;
                    if (actionListener != null) actionListener.onAiError("API request failed: " + (response != null ? response.code() : -1) + (snippet != null ? (" | body: " + snippet) : ""));
                    return;
                }

                String responseBody = response.body().string();
                if (actionListener != null) {
                    actionListener.onAiActionsProcessed(responseBody, responseBody, new java.util.ArrayList<>(), new java.util.ArrayList<>(), model.getDisplayName());
                }

            } catch (Exception e) {
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                try { if (response != null) response.close(); } catch (Exception ignore) {}
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }
}
