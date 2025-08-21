package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * GPT OSS API Client
 * - Streams SSE from https://api.gpt-oss.com/chatkit
 * - Headers: accept: text/event-stream, x-reasoning-effort, x-selected-model, x-show-reasoning: true
 * - Conversation: state.conversationId => threadId, state.lastParentId => user_id cookie
 */
public class GptOssApiClient implements ApiClient {

    private static final String TAG = "GptOssApiClient";
    private static final String ENDPOINT = "https://api.gpt-oss.com/chatkit";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;

    public GptOssApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // no timeout for streaming
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

                // effort stored as low|medium|high
                String effort = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getString("thinking_effort", "high");

                int maxAttempts = 2; // initial + one retry
                for (int attempt = 0; attempt < maxAttempts; attempt++) {
                    JsonObject body = buildRequestBody(message, state);

                    Request.Builder builder = new Request.Builder()
                            .url(ENDPOINT)
                            .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                            .addHeader("accept", "text/event-stream")
                            .addHeader("x-reasoning-effort", effort)
                            .addHeader("x-selected-model", model.getModelId())
                            .addHeader("x-show-reasoning", thinkingModeEnabled && model.getCapabilities().supportsThinking ? "true" : "false")
                            .addHeader("User-Agent", "CodeX-Android/1.0")
                            .addHeader("Origin", "https://api.gpt-oss.com")
                            .addHeader("Cache-Control", "no-cache")
                            .addHeader("Accept-Encoding", "identity");

                    // Continue conversation requires user_id cookie
                    if (state != null && state.getConversationId() != null && state.getLastParentId() != null) {
                        builder.addHeader("Cookie", "user_id=" + state.getLastParentId());
                    }

                    Request request = builder.build();
                    try { if (response != null) { response.close(); } } catch (Exception ignore) {}
                    response = httpClient.newCall(request).execute();

                    if (!response.isSuccessful() || response.body() == null) {
                        String errBody = null;
                        try { if (response != null && response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
                        String snippet = errBody != null ? (errBody.length() > 400 ? errBody.substring(0, 400) + "..." : errBody) : null;
                        if (attempt == maxAttempts - 1) {
                            if (actionListener != null) actionListener.onAiError("GPT OSS request failed: " + (response != null ? response.code() : -1) + (snippet != null ? (" | body: " + snippet) : ""));
                        } else {
                            Log.w(TAG, "GPT-OSS non-200 response, will retry once: code=" + (response != null ? response.code() : -1));
                            continue;
                        }
                        return;
                    }

                    // Extract user_id cookie for new threads (robust: check all Set-Cookie headers)
                    if (state == null || state.getConversationId() == null) {
                        List<String> setCookies = response.headers("set-cookie");
                        String userId = extractCookieValueFromHeaders(setCookies, "user_id");
                        if (userId != null && state != null) {
                            state.setLastParentId(userId);
                            if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
                        }
                    }

                    StringBuilder finalText = new StringBuilder();
                    StringBuilder thinkingText = new StringBuilder();
                    StringBuilder rawSse = new StringBuilder();
                    streamSse(response, state, finalText, thinkingText, rawSse);

                    if (finalText.length() > 0 || thinkingText.length() > 0) {
                        // Success
                        if (actionListener != null) {
                            actionListener.onAiActionsProcessed(
                                    rawSse.toString(),
                                    finalText.toString(),
                                    new ArrayList<String>(),
                                    new ArrayList<ChatMessage.FileActionDetail>(),
                                    model.getDisplayName()
                            );
                        }
                        return;
                    } else {
                        // Empty stream (timeout or premature end)
                        if (attempt < maxAttempts - 1) {
                            Log.w(TAG, "GPT-OSS empty stream, retrying once...");
                            String raw = rawSse.toString();
                            if (!raw.isEmpty()) {
                                String snip = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
                                Log.w(TAG, "Raw SSE (truncated) before retry: " + snip);
                            }
                            continue;
                        } else {
                            String raw = rawSse.toString();
                            if (!raw.isEmpty()) {
                                String snip = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
                                Log.w(TAG, "Raw SSE (truncated) on final empty: " + snip);
                            }
                            if (actionListener != null) actionListener.onAiError("No response from GPT-OSS (empty stream after retry)");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error streaming from GPT OSS", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                if (response != null) {
                    try { response.close(); } catch (Exception ignore) {}
                }
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    private JsonObject buildRequestBody(String userMessage, QwenConversationState state) {
        JsonObject contentPart = new JsonObject();
        contentPart.addProperty("type", "input_text");
        contentPart.addProperty("text", userMessage);

        JsonObject input = new JsonObject();
        input.addProperty("text", userMessage);
        JsonArray contentArr = new JsonArray();
        contentArr.add(contentPart);
        input.add("content", contentArr);
        input.addProperty("quoted_text", "");
        input.add("attachments", new JsonArray());

        JsonObject params = new JsonObject();
        params.add("input", input);

        JsonObject root = new JsonObject();
        if (state == null || state.getConversationId() == null) {
            root.addProperty("op", "threads.create");
        } else {
            root.addProperty("op", "threads.addMessage");
            params.addProperty("threadId", state.getConversationId());
        }
        root.add("params", params);
        return root;
    }

    private void streamSse(Response response, QwenConversationState state, StringBuilder finalText, StringBuilder thinkingText, StringBuilder rawAnswer) throws IOException {
        BufferedSource source = response.body().source();
        // Per-read timeout to avoid endless silence without data
        try { source.timeout().timeout(60, TimeUnit.SECONDS); } catch (Exception ignore) {}
        StringBuilder eventBuf = new StringBuilder();
        // Throttle trackers
        long[] lastEmitNsAnswer = new long[]{0L};
        long[] lastEmitNsThinking = new long[]{0L};
        int[] lastSentLenAnswer = new int[]{0};
        int[] lastSentLenThinking = new int[]{0};
        int[] currentContentIndex = new int[]{-1};
        while (true) {
            String line;
            try {
                line = source.readUtf8LineStrict();
            } catch (EOFException eof) {
                break; // stream ended
            } catch (java.io.InterruptedIOException timeout) {
                // No data within timeout
                Log.w(TAG, "GPT-OSS SSE read timed out (no data)");
                break;
            }
            if (line == null) break;

            if (line.isEmpty()) {
                // end of event
                handleSseEvent(eventBuf.toString(), state, finalText, thinkingText,
                        lastEmitNsAnswer, lastEmitNsThinking, lastSentLenAnswer, lastSentLenThinking, currentContentIndex, rawAnswer);
                eventBuf.setLength(0);
                continue;
            }
            eventBuf.append(line).append('\n');
        }
        // flush remaining
        if (eventBuf.length() > 0) {
            handleSseEvent(eventBuf.toString(), state, finalText, thinkingText,
                    lastEmitNsAnswer, lastEmitNsThinking, lastSentLenAnswer, lastSentLenThinking, currentContentIndex, rawAnswer);
        }
    }

    private void handleSseEvent(String rawEvent, QwenConversationState state, StringBuilder finalText, StringBuilder thinkingText,
                                long[] lastEmitNsAnswer, long[] lastEmitNsThinking,
                                int[] lastSentLenAnswer, int[] lastSentLenThinking,
                                int[] currentContentIndex, StringBuilder rawAnswer) {
        // Expect lines like: data: {json}
        String prefix = "data:";
        int idx = rawEvent.indexOf(prefix);
        if (idx < 0) return;
        String jsonPart = rawEvent.substring(idx + prefix.length()).trim();
        if (jsonPart.isEmpty() || jsonPart.equals("[DONE]")) return;
        try {
            // Capture raw SSE JSON for diagnostics / downstream parsing
            if (rawAnswer != null) rawAnswer.append(jsonPart).append('\n');
            JsonElement elem = JsonParser.parseString(jsonPart);
            if (!elem.isJsonObject()) return;
            JsonObject obj = elem.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";
            switch (type) {
                case "thread.created":
                    if (obj.has("thread") && state != null) {
                        JsonObject thread = obj.getAsJsonObject("thread");
                        if (thread.has("id")) {
                            state.setConversationId(thread.get("id").getAsString());
                            if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
                        }
                    }
                    break;
                case "thread.item_updated":
                    JsonObject update = obj.has("update") ? obj.getAsJsonObject("update") : obj;
                    JsonObject entry = update.has("entry") ? update.getAsJsonObject("entry") : update;
                    String entryType = entry.has("type") ? entry.get("type").getAsString() : "";
                    if ("thought".equals(entryType)) {
                        String content = entry.has("content") ? entry.get("content").getAsString() : "";
                        if (!content.isEmpty()) {
                            thinkingText.append(content);
                            maybeEmitThinking(thinkingText, lastEmitNsThinking, lastSentLenThinking);
                        }
                    } else if ("recap".equals(entryType)) {
                        // Optional: could surface recap summary; keep minimal for now
                        // No-op or append small marker
                    } else if ("assistant_message.content_part.text_delta".equals(entryType)
                            || "assistant_message.delta".equals(entryType)
                            || "message.delta".equals(entryType)
                            || "response.output_text.delta".equals(entryType)
                            || "model_output_text.delta".equals(entryType)) {
                        String delta = entry.has("delta") ? entry.get("delta").getAsString() : "";
                        int contentIndex = update.has("contentIndex") ? update.get("contentIndex").getAsInt() : entry.has("contentIndex") ? entry.get("contentIndex").getAsInt() : 0;
                        // Separate parts when contentIndex increases
                        if (contentIndex != currentContentIndex[0]) {
                            if (finalText.length() > 0 && finalText.charAt(finalText.length()-1) != '\n') finalText.append('\n');
                            finalText.append('\n');
                            currentContentIndex[0] = contentIndex;
                        }
                        if (!delta.isEmpty()) {
                            finalText.append(delta);
                            maybeEmitAnswer(finalText, lastEmitNsAnswer, lastSentLenAnswer);
                        }
                    } else {
                        // Generic fallback: append any textual delta if present
                        if (entry.has("delta") && entry.get("delta").isJsonPrimitive()) {
                            String delta = entry.get("delta").getAsString();
                            if (!delta.isEmpty()) {
                                finalText.append(delta);
                                maybeEmitAnswer(finalText, lastEmitNsAnswer, lastSentLenAnswer);
                            }
                        }
                    }
                    break;
                case "thread.updated":
                    // Title generated; optionally could be used to set conversation title
                    // No-op for now
                    break;
                case "thread.item_done":
                    // Force final emission at item completion
                    forceEmit(finalText, lastSentLenAnswer);
                    forceEmit(thinkingText, lastSentLenThinking);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to parse SSE event: " + ex.getMessage());
        }
    }

    // Throttling helpers: emit at most every ~40ms or when enough new chars are buffered
    private void maybeEmitAnswer(StringBuilder finalText, long[] lastEmitNs, int[] lastSentLen) {
        maybeEmit(finalText, false, lastEmitNs, lastSentLen);
    }

    private void maybeEmitThinking(StringBuilder thinkingText, long[] lastEmitNs, int[] lastSentLen) {
        maybeEmit(thinkingText, true, lastEmitNs, lastSentLen);
    }

    private void maybeEmit(StringBuilder buf, boolean thinking, long[] lastEmitNs, int[] lastSentLen) {
        if (actionListener == null) return;
        int len = buf.length();
        if (len == lastSentLen[0]) return;
        long now = System.nanoTime();
        long last = lastEmitNs[0];
        boolean timeReady = (last == 0L) || (now - last) >= 40_000_000L; // ~40ms
        boolean sizeReady = (len - lastSentLen[0]) >= 24; // or burst threshold
        boolean boundaryReady = len > 0 && (buf.charAt(len-1) == '\n');
        if (timeReady || sizeReady || boundaryReady) {
            actionListener.onAiStreamUpdate(buf.toString(), thinking);
            lastEmitNs[0] = now;
            lastSentLen[0] = len;
        }
    }

    private void forceEmit(StringBuilder buf, int[] lastSentLen) {
        if (actionListener == null) return;
        int len = buf.length();
        if (len == lastSentLen[0]) return;
        actionListener.onAiStreamUpdate(buf.toString(), false);
        lastSentLen[0] = len;
    }

    private static String extractCookieValue(String setCookieHeader, String key) {
        if (setCookieHeader == null) return null;
        for (String cookieLine : setCookieHeader.split(",")) { // handle combined headers
            for (String part : cookieLine.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith(key + "=")) {
                    String val = trimmed.substring((key + "=").length());
                    return val;
                }
            }
        }
        return null;
    }

    private static String extractCookieValueFromHeaders(List<String> setCookieHeaders, String key) {
        if (setCookieHeaders == null) return null;
        for (String header : setCookieHeaders) {
            String val = extractCookieValue(header, key);
            if (val != null) return val;
        }
        return null;
    }

    @Override
    public List<AIModel> fetchModels() {
        // Provide known GPT-OSS models
        List<AIModel> models = new ArrayList<>();
        models.add(com.codex.apk.ai.AIModel.fromModelId("gpt-oss-120b"));
        models.add(com.codex.apk.ai.AIModel.fromModelId("gpt-oss-20b"));
        return models;
    }
}
