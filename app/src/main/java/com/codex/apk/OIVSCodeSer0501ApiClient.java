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

public class OIVSCodeSer0501ApiClient extends AnyProviderApiClient {
    private static final String API_ENDPOINT = "https://oi-vscode-server-0501.onrender.com/v1/chat/completions";
    private final Random random = new Random();

    public OIVSCodeSer0501ApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        super(context, actionListener);
    }

    private String generateUserId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(21);
        for (int i = 0; i < 21; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    protected JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history, boolean thinkingModeEnabled) {
        JsonObject body = super.buildOpenAIStyleBody(modelId, userMessage, history, thinkingModeEnabled);
        body.remove("referrer");
        return body;
    }

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());
                String providerModel = mapToProviderModel(request.getModel() != null ? request.getModel().getModelId() : null);
                JsonObject body = buildOpenAIStyleBody(providerModel, request.getMessage(), request.getHistory(), request.isThinkingModeEnabled());

                Request req = new Request.Builder()
                        .url(API_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "text/event-stream")
                        .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                        .addHeader("userid", generateUserId())
                        .build();

                SseClient sse = new SseClient(httpClient);
                activeStreams.put(request.getRequestId(), sse);
                final StringBuilder finalText = new StringBuilder();
                final StringBuilder rawSse = new StringBuilder();

                sse.postStream(API_ENDPOINT, req.headers(), body, new SseClient.Listener() {
                    @Override public void onOpen() {}
                    @Override public void onDelta(JsonObject chunk) {
                        rawSse.append("data: ").append(chunk.toString()).append('\n');
                        try {
                            if (chunk.has("choices")) {
                                JsonArray choices = chunk.getAsJsonArray("choices");
                                for (int i = 0; i < choices.size(); i++) {
                                    JsonObject c = choices.get(i).getAsJsonObject();
                                    if (c.has("delta") && c.get("delta").isJsonObject()) {
                                        JsonObject delta = c.getAsJsonObject("delta");
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            finalText.append(delta.get("content").getAsString());
                                            listener.onStreamPartialUpdate(request.getRequestId(), finalText.toString(), false);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                    @Override public void onUsage(JsonObject usage) {}
                    @Override public void onError(String message, int code) {
                        listener.onStreamError(request.getRequestId(), "HTTP " + code + ": " + message, null);
                    }
                    @Override public void onComplete() {
                        activeStreams.remove(request.getRequestId());
                        QwenResponseParser.ParsedResponse finalResponse = new QwenResponseParser.ParsedResponse();
                        finalResponse.action = "message";
                        finalResponse.explanation = finalText.toString();
                        finalResponse.rawResponse = rawSse.toString();
                        finalResponse.isValid = true;
                        listener.onStreamCompleted(request.getRequestId(), finalResponse);
                    }
                });

            } catch (Exception e) {
                listener.onStreamError(request.getRequestId(), "Error: " + e.getMessage(), e);
            }
        }).start();
    }
}
