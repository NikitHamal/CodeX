package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * ApiAirforceApiClient
 * OpenAI-compatible streaming client targeting Api.Airforce free provider.
 * - Endpoint: https://api.airforce/v1/chat/completions
 * - No API key assumed (free route); add key header in future if needed
 */
public class ApiAirforceApiClient implements ApiClient {

    private static final String TAG = "ApiAirforceApiClient";
    private static final String AIRFORCE_ENDPOINT = "https://api.airforce/v1/chat/completions";
    private static final String AIRFORCE_MODELS_ENDPOINT = "https://api.airforce/v1/models";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public ApiAirforceApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // stream
                .build();
    }

    @Override
    public void sendMessage(String message,
                            AIModel model,
                            List<ChatMessage> history,
                            QwenConversationState state,
                            boolean thinkingModeEnabled,
                            boolean webSearchEnabled,
                            List<ToolSpec> enabledTools,
                            List<File> attachments) {
        new Thread(() -> {
            Response response = null;
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();

                String modelId = model != null ? model.getModelId() : "openai";
                JsonObject body = buildOpenAIStyleBody(modelId, message, history, thinkingModeEnabled);

                Request request = new Request.Builder()
                        .url(AIRFORCE_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "text/event-stream")
                        .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                        .addHeader("accept-encoding", "identity")
                        .build();

                response = httpClient.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errBody = null;
                    try { if (response != null && response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
                    String snippet = errBody != null ? (errBody.length() > 400 ? errBody.substring(0, 400) + "..." : errBody) : null;
                    if (actionListener != null) actionListener.onAiError("Api.Airforce request failed: " + (response != null ? response.code() : -1) + (snippet != null ? (" | body: " + snippet) : ""));
                    return;
                }

                StringBuilder finalText = new StringBuilder();
                StringBuilder rawSse = new StringBuilder();
                streamOpenAiSse(response, finalText, rawSse);

                if (finalText.length() > 0) {
                    if (actionListener != null) {
                        actionListener.onAiActionsProcessed(
                                rawSse.toString(),
                                finalText.toString(),
                                new ArrayList<String>(),
                                new ArrayList<ChatMessage.FileActionDetail>(),
                                model != null ? model.getDisplayName() : "Api.Airforce"
                        );
                    }
                } else {
                    if (actionListener != null) actionListener.onAiError("No response from Api.Airforce");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Api.Airforce", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                try { if (response != null) response.close(); } catch (Exception ignore) {}
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    private JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history, boolean thinkingModeEnabled) {
        JsonArray messages = new JsonArray();
        if (history != null) {
            for (ChatMessage m : history) {
                String role = m.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant";
                String content = m.getContent() != null ? m.getContent() : "";
                if (content.isEmpty()) continue;
                JsonObject msg = new JsonObject();
                msg.addProperty("role", role);
                msg.addProperty("content", content);
                messages.add(msg);
            }
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        JsonObject root = new JsonObject();
        root.addProperty("model", modelId);
        root.add("messages", messages);
        root.addProperty("stream", true);
        // optional reasoning config left minimal
        return root;
    }

    private void streamOpenAiSse(Response response, StringBuilder finalText, StringBuilder rawAnswer) throws IOException {
        BufferedSource source = response.body().source();
        try { source.timeout().timeout(60, TimeUnit.SECONDS); } catch (Exception ignore) {}
        StringBuilder eventBuf = new StringBuilder();
        long[] lastEmitNs = new long[]{0L};
        int[] lastSentLen = new int[]{0};
        while (true) {
            String line;
            try {
                line = source.readUtf8LineStrict();
            } catch (EOFException eof) { break; }
            catch (java.io.InterruptedIOException timeout) { Log.w(TAG, "Airforce SSE read timed out"); break; }
            if (line == null) break;
            if (line.isEmpty()) {
                handleOpenAiEvent(eventBuf.toString(), finalText, rawAnswer, lastEmitNs, lastSentLen);
                eventBuf.setLength(0);
                continue;
            }
            eventBuf.append(line).append('\n');
        }
        if (eventBuf.length() > 0) {
            handleOpenAiEvent(eventBuf.toString(), finalText, rawAnswer, lastEmitNs, lastSentLen);
        }
        if (actionListener != null && finalText.length() != lastSentLen[0]) {
            actionListener.onAiStreamUpdate(finalText.toString(), false);
        }
    }

    private void handleOpenAiEvent(String rawEvent, StringBuilder finalText, StringBuilder rawAnswer, long[] lastEmitNs, int[] lastSentLen) {
        String prefix = "data:";
        int idx = rawEvent.indexOf(prefix);
        if (idx < 0) return;
        String jsonPart = rawEvent.substring(idx + prefix.length()).trim();
        if (jsonPart.isEmpty() || jsonPart.equals("[DONE]")) return;
        try {
            if (rawAnswer != null) rawAnswer.append(jsonPart).append('\n');
            JsonElement elem = JsonParser.parseString(jsonPart);
            if (!elem.isJsonObject()) return;
            JsonObject obj = elem.getAsJsonObject();
            if (obj.has("choices") && obj.get("choices").isJsonArray()) {
                JsonArray choices = obj.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject delta = choice.has("delta") && choice.get("delta").isJsonObject() ? choice.getAsJsonObject("delta") : null;
                    if (delta != null) {
                        if (delta.has("content")) {
                            String content = delta.get("content").isJsonNull() ? null : delta.get("content").getAsString();
                            if (content != null && !content.isEmpty()) {
                                finalText.append(content);
                                maybeEmit(finalText, lastEmitNs, lastSentLen);
                            }
                        }
                    } else if (choice.has("message") && choice.get("message").isJsonObject()) {
                        JsonObject msg = choice.getAsJsonObject("message");
                        if (msg.has("content") && !msg.get("content").isJsonNull()) {
                            finalText.append(msg.get("content").getAsString());
                            maybeEmit(finalText, lastEmitNs, lastSentLen);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to parse Api.Airforce SSE: " + ex.getMessage());
        }
    }

    private void maybeEmit(StringBuilder buf, long[] lastEmitNs, int[] lastSentLen) {
        if (actionListener == null) return;
        int len = buf.length();
        if (len == lastSentLen[0]) return;
        long now = System.nanoTime();
        long last = lastEmitNs[0];
        boolean timeReady = (last == 0L) || (now - last) >= 40_000_000L; // ~40ms
        boolean sizeReady = (len - lastSentLen[0]) >= 24;
        boolean boundaryReady = len > 0 && buf.charAt(len - 1) == '\n';
        if (timeReady || sizeReady || boundaryReady) {
            actionListener.onAiStreamUpdate(buf.toString(), false);
            lastEmitNs[0] = now;
            lastSentLen[0] = len;
        }
    }

    @Override
    public List<AIModel> fetchModels() {
        List<AIModel> out = new ArrayList<>();
        try {
            OkHttpClient client = httpClient.newBuilder().readTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build();
            Request req = new Request.Builder()
                    .url(AIRFORCE_MODELS_ENDPOINT)
                    .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                    .addHeader("accept", "*/*")
                    .build();
            try (Response r = client.newCall(req).execute()) {
                if (r.isSuccessful() && r.body() != null) {
                    String body = new String(r.body().bytes(), java.nio.charset.StandardCharsets.UTF_8);
                    // Cache raw JSON for fast reloads
                    try {
                        SharedPreferences sp = context.getSharedPreferences("ai_airforce_models", Context.MODE_PRIVATE);
                        sp.edit()
                          .putString("airforce_models_json", body)
                          .putLong("airforce_models_ts", System.currentTimeMillis())
                          .apply();
                    } catch (Exception ignored) {}

                    try {
                        com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(body);
                        if (root.isJsonObject()) {
                            com.google.gson.JsonObject obj = root.getAsJsonObject();
                            if (obj.has("data") && obj.get("data").isJsonArray()) {
                                com.google.gson.JsonArray data = obj.getAsJsonArray("data");
                                for (int i = 0; i < data.size(); i++) {
                                    com.google.gson.JsonElement e = data.get(i);
                                    if (!e.isJsonObject()) continue;
                                    com.google.gson.JsonObject m = e.getAsJsonObject();
                                    String id = m.has("id") ? m.get("id").getAsString() : null;
                                    if (id == null || id.isEmpty()) continue;
                                    boolean supportsChat = m.has("supports_chat") && m.get("supports_chat").getAsBoolean();
                                    boolean supportsImages = m.has("supports_images") && m.get("supports_images").getAsBoolean();
                                    String display = toDisplayName(id);
                                    ModelCapabilities caps = new ModelCapabilities(
                                            false, // thinking
                                            false, // web
                                            supportsImages, // vision
                                            true,  // document/text
                                            false, // video
                                            false, // audio
                                            false, // citations
                                            131072,
                                            8192
                                    );
                                    // Only list chat-capable models for chat UI
                                    if (supportsChat) {
                                        out.add(new AIModel(id, display, AIProvider.AIRFORCE, caps));
                                    }
                                }
                            }
                        }
                    } catch (Exception parseErr) {
                        Log.w(TAG, "Failed to parse Api.Airforce models JSON", parseErr);
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Api.Airforce models fetch failed", ex);
        }

        if (out.isEmpty()) {
            // Fallback minimal set
            out.add(new AIModel("openai", "Api.Airforce OpenAI", AIProvider.AIRFORCE,
                    new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        }
        return out;
    }

    private String toDisplayName(String id) {
        if (id == null || id.isEmpty()) return "Api.Airforce";
        String s = id.replace('-', ' ').replace('_', ' ');
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }
}
