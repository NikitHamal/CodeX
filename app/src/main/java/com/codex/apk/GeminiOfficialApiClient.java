package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.util.JsonUtils;
import com.codex.apk.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Official Gemini client using the Generative Language API.
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=API_KEY
 */
public class GeminiOfficialApiClient implements StreamingApiClient {
    private static final String TAG = "GeminiOfficialApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private OkHttpClient http;

    // API key is optional at construction and can be set later.
    private volatile String apiKey;
    private final java.util.Map<String, okhttp3.Call> activeStreams = new java.util.HashMap<>();

    public GeminiOfficialApiClient(Context context, AIAssistant.AIActionListener actionListener, String apiKey) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.apiKey = apiKey;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiKey() { return apiKey; }

    @Override
    public List<AIModel> fetchModels() {
        // Return static models for GOOGLE provider from AIModel registry.
        List<AIModel> all = com.codex.apk.ai.AIModel.values();
        List<AIModel> google = new ArrayList<>();
        for (AIModel m : all) {
            if (m.getProvider() == AIProvider.GOOGLE) google.add(m);
        }
        return google;
    }

    private JsonObject textPart(String text) {
        JsonObject p = new JsonObject();
        p.addProperty("text", text);
        return p;
    }

    private Parsed parseGenerateContent(String body) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            // Candidates -> [ { content: { parts: [ {text: ...}, ... ] } } ]
            StringBuilder sb = new StringBuilder();
            if (root.has("candidates") && root.get("candidates").isJsonArray()) {
                JsonArray cands = root.getAsJsonArray("candidates");
                if (cands.size() > 0) {
                    JsonObject cand = cands.get(0).getAsJsonObject();
                    if (cand.has("content") && cand.get("content").isJsonObject()) {
                        JsonObject content = cand.getAsJsonObject("content");
                        if (content.has("parts") && content.get("parts").isJsonArray()) {
                            for (JsonElement el : content.getAsJsonArray("parts")) {
                                try {
                                    JsonObject part = el.getAsJsonObject();
                                    if (part.has("text") && !part.get("text").isJsonNull()) {
                                        sb.append(part.get("text").getAsString());
                                    }
                                } catch (Exception ignore) {}
                            }
                        }
                    }
                }
            }
            return new Parsed(sb.toString());
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }


    private static class Parsed { final String text; Parsed(String t) { this.text = t; } }

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());
                String key = apiKey != null && !apiKey.isEmpty() ? apiKey : SettingsActivity.getGeminiApiKey(context);
                if (key == null || key.isEmpty()) {
                    listener.onStreamError(request.getRequestId(), "Gemini API key is missing", null);
                    return;
                }
                String modelId = request.getModel() != null ? request.getModel().getModelId() : "gemini-1.5-flash";
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId + ":streamGenerateContent?key=" + key;

                JsonObject reqBody = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                JsonArray parts = new JsonArray();
                parts.add(textPart(request.getMessage()));
                userMsg.add("parts", parts);
                contents.add(userMsg);
                reqBody.add("contents", contents);

                Request httpReq = new Request.Builder().url(url).post(RequestBody.create(reqBody.toString(), JSON)).build();
                okhttp3.Call call = http.newCall(httpReq);
                activeStreams.put(request.getRequestId(), call);

                try (Response resp = call.execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        listener.onStreamError(request.getRequestId(), "HTTP " + resp.code(), null);
                        return;
                    }

                    okio.BufferedSource source = resp.body().source();
                    StringBuilder fullText = new StringBuilder();
                    StringBuilder rawResponse = new StringBuilder();

                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null || line.trim().isEmpty()) continue;
                        rawResponse.append(line).append('\n');
                        try {
                           Parsed partial = parseGenerateContent(line);
                           fullText.append(partial.text);
                           listener.onStreamPartialUpdate(request.getRequestId(), fullText.toString(), false);
                        } catch(Exception ignore) {}
                    }

                    QwenResponseParser.ParsedResponse finalResponse = new QwenResponseParser.ParsedResponse();
                    finalResponse.action = "message";
                    finalResponse.explanation = fullText.toString();
                    finalResponse.rawResponse = rawResponse.toString();
                    finalResponse.isValid = true;
                    listener.onStreamCompleted(request.getRequestId(), finalResponse);

                } finally {
                    activeStreams.remove(request.getRequestId());
                }
            } catch (IOException e) {
                if (!"Canceled".equalsIgnoreCase(e.getMessage())) {
                    listener.onStreamError(request.getRequestId(), "Error: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    @Override
    public void cancelStreaming(String requestId) {
        if (activeStreams.containsKey(requestId)) {
            okhttp3.Call call = activeStreams.get(requestId);
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
            activeStreams.remove(requestId);
        }
    }

    @Override
    public void setConnectionPool(okhttp3.ConnectionPool pool) {
        this.http = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .connectionPool(pool)
                .build();
    }
}
