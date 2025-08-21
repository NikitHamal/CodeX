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
 * CloudflareApiClient
 * Client for Cloudflare AI Playground endpoints (free, no API key assumed).
 * - Inference: https://playground.ai.cloudflare.com/api/inference
 * - Models:    https://playground.ai.cloudflare.com/api/models
 */
public class CloudflareApiClient implements ApiClient {
    private static final String TAG = "CloudflareApiClient";
    private static final String CF_INFERENCE = "https://playground.ai.cloudflare.com/api/inference";
    private static final String CF_MODELS = "https://playground.ai.cloudflare.com/api/models";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public CloudflareApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
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

                String modelId = model != null ? model.getModelId() : "llama-3.3-70b";
                JsonObject body = buildCloudflareBody(modelId, message, history);

                Request request = new Request.Builder()
                        .url(CF_INFERENCE)
                        .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                        .addHeader("origin", "https://playground.ai.cloudflare.com")
                        .addHeader("referer", "https://playground.ai.cloudflare.com/")
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .build();

                response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String modelDisplay = model != null ? model.getDisplayName() : (modelId != null ? modelId : "Cloudflare");
                    streamCloudflare(response, modelDisplay);
                } else {
                    if (actionListener != null) actionListener.onAiError("Cloudflare error: " + (response != null ? response.code() : -1));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Cloudflare", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                try { if (response != null) response.close(); } catch (Exception ignore) {}
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    private JsonObject buildCloudflareBody(String model, String userMessage, List<ChatMessage> history) {
        JsonArray msgs = new JsonArray();
        if (history != null) {
            for (ChatMessage m : history) {
                String role = m.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant";
                String content = m.getContent() == null ? "" : m.getContent();
                if (content.isEmpty()) continue;
                JsonObject msg = new JsonObject();
                msg.addProperty("role", role);
                // Cloudflare expects parts array
                JsonArray parts = new JsonArray();
                JsonObject p = new JsonObject();
                p.addProperty("type", "text");
                p.addProperty("text", content);
                parts.add(p);
                msg.add("parts", parts);
                msgs.add(msg);
            }
        }
        JsonObject u = new JsonObject();
        u.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject p = new JsonObject();
        p.addProperty("type", "text");
        p.addProperty("text", userMessage);
        parts.add(p);
        u.add("parts", parts);
        msgs.add(u);

        JsonObject body = new JsonObject();
        body.add("messages", msgs);
        body.addProperty("lora", (String) null);
        body.addProperty("model", model);
        body.addProperty("max_tokens", 2048);
        body.addProperty("stream", true);
        body.addProperty("system_message", "You are a helpful assistant");
        body.add("tools", new JsonArray());
        return body;
    }

    private void streamCloudflare(Response response, String modelDisplayName) throws IOException {
        BufferedSource source = response.body().source();
        try { source.timeout().timeout(60, TimeUnit.SECONDS); } catch (Exception ignore) {}
        StringBuilder buf = new StringBuilder();
        long[] lastEmitNs = new long[]{0L};
        int[] lastLen = new int[]{0};
        while (true) {
            String line;
            try {
                line = source.readUtf8LineStrict();
            } catch (EOFException eof) { break; }
            catch (java.io.InterruptedIOException timeout) { Log.w(TAG, "CF SSE timeout"); break; }
            if (line == null) break;
            if (line.startsWith("0:")) {
                String json = line.substring(2);
                try {
                    JsonElement el = JsonParser.parseString(json);
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        // Heuristic: try common fields
                        if (obj.has("text") && !obj.get("text").isJsonNull()) {
                            buf.append(obj.get("text").getAsString());
                        } else if (obj.has("delta") && obj.get("delta").isJsonObject()) {
                            JsonObject d = obj.getAsJsonObject("delta");
                            if (d.has("content")) buf.append(d.get("content").getAsString());
                        } else if (obj.has("content")) {
                            buf.append(obj.get("content").getAsString());
                        }
                        maybeEmit(buf, lastEmitNs, lastLen);
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "CF parse err: " + ex.getMessage());
                }
            } else if (line.startsWith("e:")) {
                // usage/end marker; ignore
            }
        }
        if (actionListener != null) {
            // Final stream update
            actionListener.onAiStreamUpdate(buf.toString(), false);
            // Hand off full response to universal parser in AiAssistantManager
            try {
                actionListener.onAiActionsProcessed(buf.toString(), buf.toString(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), modelDisplayName);
            } catch (Exception e) {
                Log.w(TAG, "CF finalize parse dispatch failed: " + e.getMessage());
            }
        }
    }

    private void maybeEmit(StringBuilder buf, long[] lastEmitNs, int[] lastSentLen) {
        if (actionListener == null) return;
        int len = buf.length();
        if (len == lastSentLen[0]) return;
        long now = System.nanoTime();
        long last = lastEmitNs[0];
        boolean timeReady = (last == 0L) || (now - last) >= 40_000_000L;
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
            OkHttpClient client = httpClient.newBuilder().readTimeout(30, TimeUnit.SECONDS).build();
            Request req = new Request.Builder()
                    .url(CF_MODELS)
                    .addHeader("user-agent", "Mozilla/5.0 (Linux; Android) CodeX-Android/1.0")
                    .addHeader("accept", "*/*")
                    .build();
            try (Response r = client.newCall(req).execute()) {
                if (r.isSuccessful() && r.body() != null) {
                    String body = new String(r.body().bytes(), StandardCharsets.UTF_8);
                    try {
                        SharedPreferences sp = context.getSharedPreferences("ai_cloudflare_models", Context.MODE_PRIVATE);
                        sp.edit().putString("cf_models_json", body).putLong("cf_models_ts", System.currentTimeMillis()).apply();
                    } catch (Exception ignore) {}

                    try {
                        JsonElement root = JsonParser.parseString(body);
                        if (root.isJsonArray()) {
                            JsonArray arr = root.getAsJsonArray();
                            for (JsonElement e : arr) {
                                if (!e.isJsonObject()) continue;
                                JsonObject m = e.getAsJsonObject();
                                String name = m.has("name") ? m.get("name").getAsString() : null;
                                if (name == null || name.isEmpty()) continue;
                                String id = cleanModelId(name);
                                String display = toDisplayName(id);
                                ModelCapabilities caps = new ModelCapabilities(false, false, m.has("vision") && m.get("vision").getAsBoolean(), true, false, false, false, 131072, 8192);
                                out.add(new AIModel(id, display, AIProvider.CLOUDFLARE, caps));
                            }
                        }
                    } catch (Exception parseErr) {
                        Log.w(TAG, "CF models parse failed", parseErr);
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "CF models fetch failed", ex);
        }
        if (out.isEmpty()) {
            out.add(new AIModel("llama-3.3-70b", "Cloudflare Llama 3.3 70B", AIProvider.CLOUDFLARE,
                    new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        }
        return out;
    }

    private String cleanModelId(String name) {
        // Mirror provider clean_name heuristic partially
        String id = name.toLowerCase(Locale.ROOT);
        id = id.replace("meta-llama-", "llama-");
        id = id.replace("-instruct", "").replace("-fast", "").replace("-chat", "");
        id = id.replace("@cf/", "").replace("@hf/", "");
        return id;
    }

    private String toDisplayName(String id) {
        if (id == null || id.isEmpty()) return "Cloudflare";
        String s = id.replace('-', ' ').replace('_', ' ');
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
