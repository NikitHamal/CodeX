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
        // Based on the python script, this provider uses a hardcoded list of models.
        List<AIModel> models = new ArrayList<>();
        models.add(new AIModel("@hf/thebloke/deepseek-coder-6.7b-base-awq", "deepseek-coder-6.7b-base", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/deepseek-coder-6.7b-instruct-awq", "deepseek-coder-6.7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/deepseek-ai/deepseek-math-7b-instruct", "deepseek-math-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/deepseek-ai/deepseek-r1-distill-qwen-32b", "deepseek-distill-qwen-32b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/thebloke/discolm-german-7b-v1-awq", "discolm-german-7b-v1", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/tiiuae/falcon-7b-instruct", "falcon-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/google/gemma-3-12b-it", "gemma-3-12b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/google/gemma-7b-it", "gemma-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/nousresearch/hermes-2-pro-mistral-7b", "hermes-2-pro-mistral-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/llama-2-13b-chat-awq", "llama-2-13b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-2-7b-chat-fp16", "llama-2-7b-fp16", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-2-7b-chat-int8", "llama-2-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/meta-llama/meta-llama-3-8b-instruct", "llama-3-8b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-3.1-8b-instruct-fp8", "llama-3.1-8b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-3.2-11b-vision-instruct", "llama-3.2-11b-vision", AIProvider.CLOUDFLARE, new ModelCapabilities(true, true, true, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-3.2-1b-instruct", "llama-3.2-1b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-3.2-3b-instruct", "llama-3.2-3b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-3.3-70b-instruct-fp8-fast", "llama-3.3-70b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-4-scout-17b-16e-instruct", "llama-4-scout", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/meta/llama-guard-3-8b", "llama-guard-3-8b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/llamaguard-7b-awq", "llamaguard-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/mistral-7b-instruct-v0.1-awq", "mistral-7b-v0.1", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/mistral/mistral-7b-instruct-v0.2", "mistral-7b-v0.2", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/mistralai/mistral-small-3.1-24b-instruct", "mistral-small-3.1-24b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/neural-chat-7b-v3-1-awq", "neural-7b-v3-1", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/openchat/openchat-3.5-0106", "openchat-3.5-0106", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/openhermes-2.5-mistral-7b-awq", "openhermes-2.5-mistral-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/microsoft/phi-2", "phi-2", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/qwen/qwen1.5-0.5b-chat", "qwen1.5-0.5b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/qwen/qwen1.5-1.8b-chat", "qwen-1.5-1.8b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/qwen/qwen1.5-14b-chat-awq", "qwen-1.5-14b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/qwen/qwen1.5-7b-chat-awq", "qwen-1.5-7b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/qwen/qwen2.5-coder-32b-instruct", "qwen-2.5-coder-32b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/qwen/qwq-32b", "qwq-32b", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/defog/sqlcoder-7b-2", "sqlcoder-7b-2", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/nexusflow/starling-lm-7b-beta", "starling-lm-7b-beta", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/tinyllama/tinyllama-1.1b-chat-v1.0", "tinyllama-1.1b-v1.0", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@cf/fblgit/una-cybertron-7b-v2-bf16", "una-cybertron-7b-v2-bf16", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        models.add(new AIModel("@hf/thebloke/zephyr-7b-beta-awq", "zephyr-7b-beta", AIProvider.CLOUDFLARE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)));
        return models;
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
