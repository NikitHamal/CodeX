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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import com.codex.apk.util.JsonUtils;
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
    protected final AIAssistant.AIActionListener actionListener;
    protected final OkHttpClient httpClient;
    protected final Gson gson = new Gson();
    protected final Random random = new Random();

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
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                String providerModel = mapToProviderModel(model != null ? model.getModelId() : null);
                JsonObject body = buildOpenAIStyleBody(providerModel, message, history, thinkingModeEnabled);
                Request req = new Request.Builder()
                        .url(OPENAI_ENDPOINT)
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .addHeader("accept", "text/event-stream")
                        .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                        .addHeader("origin", "https://pollinations.ai")
                        .addHeader("referer", "https://pollinations.ai/")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("accept-encoding", "identity")
                        .build();

                SseClient sse = new SseClient(httpClient);
                StringBuilder finalText = new StringBuilder();
                StringBuilder rawSse = new StringBuilder();
                sse.postStream(OPENAI_ENDPOINT, req.headers(), body, new SseClient.Listener() {
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
                                            actionListener.onAiStreamUpdate(finalText.toString(), false);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                    @Override public void onUsage(JsonObject usage) {}
                    @Override public void onError(String message, int code) {
                        if (actionListener != null) actionListener.onAiError("Free endpoint request failed: " + code + (message != null ? (" | body: " + message) : ""));
                    }
                    @Override public void onComplete() {
                        if (actionListener == null) return;
                        String jsonToParse = JsonUtils.extractJsonFromCodeBlock(finalText.toString());
                        if (jsonToParse == null && JsonUtils.looksLikeJson(finalText.toString())) {
                            jsonToParse = finalText.toString();
                        }
                        if (jsonToParse != null) {
                            try {
                                QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(jsonToParse);
                                if (parsed != null && parsed.isValid) {
                                    if ("plan".equals(parsed.action) && parsed.planSteps != null && !parsed.planSteps.isEmpty()) {
                                        List<ChatMessage.PlanStep> planSteps = QwenResponseParser.toPlanSteps(parsed);
                                        actionListener.onAiActionsProcessed(jsonToParse, parsed.explanation, new ArrayList<>(), new ArrayList<>(), planSteps, model.getDisplayName());
                                    } else {
                                        List<ChatMessage.FileActionDetail> fileActions = QwenResponseParser.toFileActionDetails(parsed);
                                        actionListener.onAiActionsProcessed(jsonToParse, parsed.explanation, new ArrayList<>(), fileActions, new ArrayList<>(), model.getDisplayName());
                                    }
                                } else {
                                    actionListener.onAiActionsProcessed(rawSse.toString(), finalText.toString(), new ArrayList<>(), new ArrayList<>(), model.getDisplayName());
                                }
                            } catch (Exception e) {
                                actionListener.onAiActionsProcessed(rawSse.toString(), finalText.toString(), new ArrayList<>(), new ArrayList<>(), model.getDisplayName());
                            }
                        } else {
                            actionListener.onAiActionsProcessed(rawSse.toString(), finalText.toString(), new ArrayList<>(), new ArrayList<>(), model.getDisplayName());
                        }
                        actionListener.onAiRequestCompleted();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error calling free provider", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    protected JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history, boolean thinkingModeEnabled) {
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

    protected void streamOpenAiSse(Response response, StringBuilder finalText, StringBuilder rawAnswer) throws IOException {
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

    protected void handleOpenAiEvent(String rawEvent, StringBuilder finalText, StringBuilder rawAnswer, long[] lastEmitNs, int[] lastSentLen) {
        String prefix = "data:";
        int idx = rawEvent.indexOf(prefix);
        if (idx < 0) return;
        String jsonPart = rawEvent.substring(idx + prefix.length()).trim();
        if (jsonPart.isEmpty() || jsonPart.equals("[DONE]") || jsonPart.equalsIgnoreCase("data: [DONE]")) return;
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

    protected void maybeEmit(StringBuilder buf, long[] lastEmitNs, int[] lastSentLen) {
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

    protected String mapToProviderModel(String modelId) {
        if (modelId == null || modelId.isEmpty()) return "openai"; // sensible default
        String lower = modelId.toLowerCase(Locale.ROOT);
        // Pollinations exposes many backends by name; pass through most names.
        return lower;
    }

    @Override
    public List<AIModel> fetchModels() {
        return new ArrayList<>();
    }

    protected static String toDisplay(String id) {
        if (id == null || id.isEmpty()) return "Unnamed Model";
        String s = id.replace('-', ' ').trim();
        if (s.isEmpty()) return id;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
