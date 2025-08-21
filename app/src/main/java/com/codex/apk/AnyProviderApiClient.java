package com.codex.apk;

import android.content.Context;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * AnyProviderApiClient
 * Generic client for free endpoints (starting with Pollinations text API).
 * - Streams OpenAI-style SSE deltas from https://text.pollinations.ai/openai
 * - No API key required
 * - Gracefully maps unknown FREE models to a default provider model
 */
public class AnyProviderApiClient implements ApiClient {

    private static final String TAG = "AnyProviderApiClient";
    private static final String OPENAI_ENDPOINT = "https://text.pollinations.ai/openai";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private final Random random = new Random();

    public AnyProviderApiClient(Context context, AIAssistant.AIActionListener actionListener) {
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

                // Build request JSON (OpenAI-style)
                String providerModel = mapToProviderModel(model != null ? model.getModelId() : null);
                JsonObject body = buildOpenAIStyleBody(providerModel, message, history, thinkingModeEnabled);

                Request request = new Request.Builder()
                        .url(OPENAI_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "text/event-stream")
                        .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                        .addHeader("origin", "https://pollinations.ai")
                        .addHeader("referer", "https://pollinations.ai/")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("accept-encoding", "identity")
                        .build();

                response = httpClient.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errBody = null;
                    try { if (response != null && response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
                    String snippet = errBody != null ? (errBody.length() > 400 ? errBody.substring(0, 400) + "..." : errBody) : null;
                    if (actionListener != null) actionListener.onAiError("Free endpoint request failed: " + (response != null ? response.code() : -1) + (snippet != null ? (" | body: " + snippet) : ""));
                    return;
                }

                // Stream OpenAI-like SSE
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
                                model != null ? model.getDisplayName() : "Free Model"
                        );
                    }
                } else {
                    if (actionListener != null) actionListener.onAiError("No response from free provider");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling free provider", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                try { if (response != null) response.close(); } catch (Exception ignore) {}
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    private JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history, boolean thinkingModeEnabled) {
        JsonArray messages = new JsonArray();
        // Convert existing history (keep it light)
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
        // Append the new user message (AIAssistant has already prepended system prompt when needed)
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        JsonObject root = new JsonObject();
        root.addProperty("model", modelId);
        root.add("messages", messages);
        root.addProperty("stream", true);
        // Pollinations supports seed and referrer; seed helps cache-busting and diversity
        root.addProperty("seed", random.nextInt(Integer.MAX_VALUE));
        root.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
        // If provider supports reasoning visibility, prefer default (no explicit toggle); keep payload minimal
        if (thinkingModeEnabled) {
            // Some providers accept x-show-reasoning header; we rely on minimal body here
        }
        return root;
    }

    private void streamOpenAiSse(Response response, StringBuilder finalText, StringBuilder rawAnswer) throws IOException {
        BufferedSource source = response.body().source();
        try { source.timeout().timeout(60, TimeUnit.SECONDS); } catch (Exception ignore) {}
        StringBuilder eventBuf = new StringBuilder();
        // Throttle
        long[] lastEmitNs = new long[]{0L};
        int[] lastSentLen = new int[]{0};
        while (true) {
            String line;
            try {
                line = source.readUtf8LineStrict();
            } catch (EOFException eof) { break; }
            catch (java.io.InterruptedIOException timeout) { Log.w(TAG, "Free SSE read timed out"); break; }
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
        // Force final emit
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
                        // Non-streaming fallback chunk
                        JsonObject msg = choice.getAsJsonObject("message");
                        if (msg.has("content") && !msg.get("content").isJsonNull()) {
                            finalText.append(msg.get("content").getAsString());
                            maybeEmit(finalText, lastEmitNs, lastSentLen);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to parse OpenAI-like SSE: " + ex.getMessage());
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

    private String mapToProviderModel(String modelId) {
        if (modelId == null || modelId.isEmpty()) return "openai"; // sensible default
        String lower = modelId.toLowerCase(Locale.ROOT);
        if (lower.startsWith("gemini")) return "openai"; // map Gemini-free to openai backend
        // pass through known pollinations models, otherwise fallback
        switch (lower) {
            case "openai":
            case "deepseek":
            case "deepseek-reasoning":
            case "llamascout":
            case "grok":
                return lower;
            default:
                return "openai";
        }
    }

    @Override
    public List<AIModel> fetchModels() {
        List<AIModel> models = new ArrayList<>();
        // Try to fetch text models from Pollinations; fall back to a minimal set
        try {
            OkHttpClient client = httpClient.newBuilder().readTimeout(30, TimeUnit.SECONDS).build();
            Request req = new Request.Builder().url("https://text.pollinations.ai/models")
                    .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                    .build();
            try (Response r = client.newCall(req).execute()) {
                if (r.isSuccessful() && r.body() != null) {
                    String body = new String(r.body().bytes(), StandardCharsets.UTF_8);
                    JsonElement el = JsonParser.parseString(body);
                    if (el.isJsonArray()) {
                        for (JsonElement e : el.getAsJsonArray()) {
                            if (!e.isJsonObject()) continue;
                            JsonObject obj = e.getAsJsonObject();
                            String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : null;
                            if (name == null || name.isEmpty()) continue;
                            String display = toDisplay(name);
                            models.add(new AIModel(name, display, AIProvider.FREE,
                                    new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to fetch pollinations text models: " + ex.getMessage());
        }
        if (models.isEmpty()) {
            models.add(new AIModel("openai", "Pollinations OpenAI", AIProvider.FREE,
                    new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
            models.add(new AIModel("deepseek", "Pollinations DeepSeek", AIProvider.FREE,
                    new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        }
        return models;
    }

    private static String toDisplay(String id) {
        if (id == null || id.isEmpty()) return "Unnamed Model";
        String s = id.replace('-', ' ').trim();
        if (s.isEmpty()) return id;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
