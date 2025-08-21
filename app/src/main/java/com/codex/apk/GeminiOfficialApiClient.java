package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
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
public class GeminiOfficialApiClient implements ApiClient {
    private static final String TAG = "GeminiOfficialApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient http;

    // API key is optional at construction and can be set later.
    private volatile String apiKey;

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
    public void sendMessage(String message,
                            AIModel model,
                            List<ChatMessage> history,
                            QwenConversationState state,
                            boolean thinkingModeEnabled,
                            boolean webSearchEnabled,
                            List<ToolSpec> enabledTools,
                            List<File> attachments) {
        new Thread(() -> {
            if (actionListener != null) actionListener.onAiRequestStarted();
            try {
                String key = apiKey;
                if (key == null || key.isEmpty()) {
                    // Fallback to Settings if not set via setter
                    key = SettingsActivity.getGeminiApiKey(context);
                }
                if (key == null || key.isEmpty()) {
                    if (actionListener != null) actionListener.onAiError("Gemini API key is missing. Set it in Settings.");
                    return;
                }

                String modelId = model != null ? model.getModelId() : "gemini-1.5-flash";
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId + ":generateContent?key=" + key;

                // Build request JSON. We prepend the system prompt to the user content (simple and robust).
                JsonObject req = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                JsonArray parts = new JsonArray();
                parts.add(textPart(message));
                userMsg.add("parts", parts);
                contents.add(userMsg);
                req.add("contents", contents);

                Request httpReq = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(req.toString(), JSON))
                        .build();

                try (Response resp = http.newCall(httpReq).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        String err = resp.body() != null ? resp.body().string() : null;
                        if (actionListener != null) actionListener.onAiError("Gemini API error: " + resp.code() + (err != null ? ": " + err : ""));
                        return;
                    }
                    String body = resp.body().string();
                    Parsed parsed = parseGenerateContent(body);

                    String explanation = deriveHumanExplanation(parsed.text);
                    // No web sources for now; could parse citations later.
                    List<String> suggestions = new ArrayList<>();
                    List<ChatMessage.FileActionDetail> files = new ArrayList<>();

                    if (actionListener instanceof com.codex.apk.editor.AiAssistantManager) {
                        ((com.codex.apk.editor.AiAssistantManager) actionListener)
                                .onAiActionsProcessed(parsed.text, explanation, suggestions, files,
                                        model != null ? model.getDisplayName() : "Gemini", null, new ArrayList<>());
                    } else {
                        String fallback = ResponseUtils.buildExplanationWithThinking(explanation, null);
                        actionListener.onAiActionsProcessed(parsed.text, fallback, suggestions, files,
                                model != null ? model.getDisplayName() : "Gemini");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini official API", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

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

    private String deriveHumanExplanation(String text) {
        if (text == null) return "";
        String t = normalizeJsonIfPresent(text);
        boolean looksJson = com.codex.apk.QwenResponseParser.looksLikeJson(t != null ? t.trim() : "");
        if (looksJson) {
            try {
                JsonObject obj = JsonParser.parseString(t).getAsJsonObject();
                if (obj.has("action")) {
                    String action = obj.get("action").getAsString();
                    if ("plan".equalsIgnoreCase(action)) {
                        String goal = obj.has("goal") ? obj.get("goal").getAsString() : "Plan";
                        return "Plan: " + goal;
                    } else if ("file_operation".equalsIgnoreCase(action)) {
                        String expl = obj.has("explanation") ? obj.get("explanation").getAsString() : "Proposed file operations";
                        return expl;
                    }
                }
            } catch (Exception ignore) {}
            return "";
        }
        return text.trim();
    }

    private String normalizeJsonIfPresent(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("```") ) {
            int firstBrace = t.indexOf('{');
            int lastBrace = t.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                t = t.substring(firstBrace, lastBrace + 1);
            }
        } else if (t.startsWith("json\n")) {
            t = t.substring(5);
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String cand = t.substring(start, end + 1);
            try { JsonParser.parseString(cand).getAsJsonObject(); return cand; } catch (Exception ignore) {}
        }
        return text;
    }

    private static class Parsed { final String text; Parsed(String t) { this.text = t; } }
}
