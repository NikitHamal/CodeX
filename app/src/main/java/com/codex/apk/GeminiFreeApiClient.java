package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * Reverse-engineered Gemini client using cookies (__Secure-1PSID, __Secure-1PSIDTS).
 * Minimal implementation: text prompt -> text response.
 */
public class GeminiFreeApiClient implements ApiClient {
    private static final String TAG = "GeminiFreeApiClient";

    private static final String INIT_URL = "https://gemini.google.com/app";
    private static final String GOOGLE_URL = "https://www.google.com";
    private static final String GENERATE_URL = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String ROTATE_COOKIES_URL = "https://accounts.google.com/RotateCookies";
    private static final String UPLOAD_URL = "https://content-push.googleapis.com/upload";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean refreshRunning = false;

    public GeminiFreeApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState unused, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                String psid = SettingsActivity.getSecure1PSID(context);
                String psidts = SettingsActivity.getSecure1PSIDTS(context);
                // Load cached 1psidts if missing
                if ((psidts == null || psidts.isEmpty()) && psid != null && !psid.isEmpty()) {
                    String cached = SettingsActivity.getCached1psidts(context, psid);
                    if (cached != null && !cached.isEmpty()) {
                        psidts = cached;
                    }
                }
                if (psid == null || psid.isEmpty()) {
                    if (actionListener != null) actionListener.onAiError("__Secure-1PSID cookie not set in Settings");
                    return;
                }

                Map<String, String> baseCookies = new HashMap<>();
                baseCookies.put("__Secure-1PSID", psid);
                if (psidts != null && !psidts.isEmpty()) baseCookies.put("__Secure-1PSIDTS", psidts);

                Map<String, String> cookies = warmupAndMergeCookies(baseCookies);

                String accessToken = fetchAccessToken(cookies);
                if (accessToken == null) {
                    if (actionListener != null) actionListener.onAiError("Failed to retrieve access token from Gemini INIT page");
                    return;
                }

                // Start periodic refresh if not running
                startAutoRefresh(psid, cookies);

                // Optionally rotate 1PSIDTS immediately once
                rotate1psidtsIfPossible(cookies);
                // Persist refreshed __Secure-1PSIDTS if present
                if (cookies.containsKey("__Secure-1PSIDTS")) {
                    SettingsActivity.setCached1psidts(context, psid, cookies.get("__Secure-1PSIDTS"));
                }

                String modelId = model != null ? model.getModelId() : "gemini-2.5-flash";
                Headers requestHeaders = buildGeminiHeaders(modelId);

                // Load prior conversation metadata if any, else derive from history if present
                String priorMeta = SettingsActivity.getFreeConversationMetadata(context, modelId);
                String chatMeta = null;
                if (priorMeta != null && !priorMeta.isEmpty()) {
                    chatMeta = priorMeta; // Stored as JSON array string like [cid, rid, rcid]
                } else {
                    // Try to derive minimal metadata from last assistant message raw response if available
                    try {
                        for (int i = history.size() - 1; i >= 0; i--) {
                            ChatMessage m = history.get(i);
                            if (m.getSender() == ChatMessage.SENDER_AI && m.getRawApiResponse() != null) {
                                String meta = extractMetadataArrayFromRaw(m.getRawApiResponse());
                                if (meta != null) { chatMeta = meta; break; }
                            }
                        }
                    } catch (Exception ignore) {}
                }

                // Upload attachments minimally and build files array entries
                List<File> imageFiles = attachments != null ? attachments : new ArrayList<>();
                List<UploadedRef> uploaded = new ArrayList<>();
                for (File f : imageFiles) {
                    try {
                        String identifier = uploadFileReturnId(cookies, f);
                        if (identifier != null) uploaded.add(new UploadedRef(identifier, f.getName()));
                    } catch (Exception e) {
                        Log.w(TAG, "Upload failed for " + f.getName() + ": " + e.getMessage());
                    }
                }

                RequestBody formBody = buildGenerateForm(accessToken, message, chatMeta, uploaded);
                Request req = new Request.Builder()
                        .url(GENERATE_URL)
                        .headers(requestHeaders)
                        .header("Cookie", buildCookieHeader(cookies))
                        .post(formBody)
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        String errBody = null;
                        try { errBody = resp.body() != null ? resp.body().string() : null; } catch (Exception ignore) {}
                        Log.w(TAG, "Gemini request failed (first attempt): " + resp.code() + ", body=" + errBody);

                        accessToken = fetchAccessToken(cookies);
                        if (accessToken != null) {
                            Request retry = new Request.Builder()
                                    .url(GENERATE_URL)
                                    .headers(requestHeaders)
                                    .header("Cookie", buildCookieHeader(cookies))
                                    .post(buildGenerateForm(accessToken, message, chatMeta, uploaded))
                                    .build();
                            try (Response resp2 = httpClient.newCall(retry).execute()) {
                                if (!resp2.isSuccessful() || resp2.body() == null) {
                                    String errBody2 = null;
                                    try { errBody2 = resp2.body() != null ? resp2.body().string() : null; } catch (Exception ignore) {}
                                    if (actionListener != null) actionListener.onAiError("Gemini request failed: " + resp2.code() + (errBody2 != null ? ": " + errBody2 : ""));
                                    return;
                                }
                                                    String body2 = resp2.body().string();
                                 ParsedOutput parsed2 = parseOutputFromStream(body2);
                                 persistConversationMetaIfAvailable(modelId, body2);
                                 String explanation2 = buildExplanationWithThinking(parsed2.text, parsed2.thoughts);
                                 if (actionListener != null) {
                                     actionListener.onAiActionsProcessed(body2, explanation2, new ArrayList<>(), new ArrayList<>(), model != null ? model.getDisplayName() : "Gemini (Free)");
                                 }
                                 // Cache metadata onto the last chat message raw response to help derive context later
                                 // (UI manager will receive this via onAiActionsProcessed).
                                 
                                return;
                            }
                        }

                        if (actionListener != null) actionListener.onAiError("Gemini request failed: " + resp.code() + (errBody != null ? ": " + errBody : ""));
                        return;
                    }

                    // Stream parse lines for partial updates
                    BufferedSource source = resp.body().source();
                    StringBuilder full = new StringBuilder();
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) break;
                        full.append(line).append("\n");
                        // emit incremental thinking/text when possible
                        try {
                            String[] parts = full.toString().split("\n");
                            if (parts.length >= 3) {
                                com.google.gson.JsonArray responseJson = JsonParser.parseString(parts[2]).getAsJsonArray();
                                // naive partial: look at last part for candidate delta
                                for (int i = 0; i < responseJson.size(); i++) {
                                    try {
                                        com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
                                        if (part.size() > 4 && !part.get(4).isJsonNull()) {
                                            com.google.gson.JsonArray candidates = part.get(4).getAsJsonArray();
                                            if (candidates.size() > 0) {
                                                String partial = candidates.get(0).getAsJsonArray().get(1).getAsJsonArray().get(0).getAsString();
                                                if (actionListener != null) actionListener.onAiStreamUpdate(partial, false);
                                            }
                                        }
                                    } catch (Exception ignore) {}
                                }
                            }
                        } catch (Exception ignore) {}
                    }

                    String body = full.toString();
                    ParsedOutput parsed = parseOutputFromStream(body);
                    persistConversationMetaIfAvailable(modelId, body);
                    String explanation = buildExplanationWithThinking(parsed.text, parsed.thoughts);
                    if (actionListener != null) {
                        actionListener.onAiActionsProcessed(body, explanation, new ArrayList<>(), new ArrayList<>(), model != null ? model.getDisplayName() : "Gemini (Free)");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini Free API", e);
                if (actionListener != null) actionListener.onAiError("Error: " + e.getMessage());
            } finally {
                if (actionListener != null) actionListener.onAiRequestCompleted();
            }
        }).start();
    }

    @Override
    public List<AIModel> fetchModels() {
        // Static supported list for FREE provider
        List<AIModel> list = new ArrayList<>();
        list.add(new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash (Free)", AIProvider.FREE, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)));
        list.add(new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro (Free)", AIProvider.FREE, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)));
        list.add(new AIModel("gemini-2.0-flash", "Gemini 2.0 Flash (Free)", AIProvider.FREE, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)));
        return list;
    }

    private String fetchAccessToken(Map<String, String> cookies) throws IOException {
        Request init = new Request.Builder()
                .url(INIT_URL)
                .get()
                .headers(Headers.of(defaultGeminiHeaders()))
                .header("Cookie", buildCookieHeader(cookies))
                .build();
        try (Response resp = httpClient.newCall(init).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return null;
            // Merge Set-Cookie from INIT into cookies map
            if (resp.headers("Set-Cookie") != null) {
                for (String c : resp.headers("Set-Cookie")) {
                    String[] parts = c.split(";", 2);
                    String[] kv = parts[0].split("=", 2);
                    if (kv.length == 2) cookies.put(kv[0], kv[1]);
                }
            }
            String html = resp.body().string();
            // Extract "SNlM0e":"<token>"
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"SNlM0e\":\"(.*?)\"").matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private void rotate1psidtsIfPossible(Map<String, String> cookies) {
        try {
            if (!cookies.containsKey("__Secure-1PSID")) return;
            Request req = new Request.Builder()
                    .url(ROTATE_COOKIES_URL)
                    .post(RequestBody.create("[000,\"-0000000000000000000\"]", MediaType.parse("application/json")))
                    .headers(Headers.of(new HashMap<String, String>() {{
                        put("Content-Type", "application/json");
                    }}))
                    .header("Cookie", buildCookieHeader(cookies))
                    .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (resp.code() == 401) return; // unauthorized, keep old
                if (!resp.isSuccessful()) return;
                if (resp.headers("Set-Cookie") != null) {
                    for (String c : resp.headers("Set-Cookie")) {
                        String[] parts = c.split(";", 2);
                        String[] kv = parts[0].split("=", 2);
                        if (kv.length == 2) cookies.put(kv[0], kv[1]);
                    }
                }
            }
        } catch (Exception ignore) {}
    }

    private Map<String, String> warmupAndMergeCookies(Map<String, String> baseCookies) throws IOException {
        Map<String, String> cookies = new HashMap<>(baseCookies);
        try (Response r = httpClient.newCall(new Request.Builder().url(GOOGLE_URL).get().build()).execute()) {
            if (r.headers("Set-Cookie") != null) {
                for (String c : r.headers("Set-Cookie")) {
                    String[] parts = c.split(";", 2);
                    String[] kv = parts[0].split("=", 2);
                    if (kv.length == 2) cookies.put(kv[0], kv[1]);
                }
            }
        }
        return cookies;
    }

    private void startAutoRefresh(String psid, Map<String, String> cookies) {
        if (refreshRunning) return;
        refreshRunning = true;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                rotate1psidtsIfPossible(cookies);
                if (cookies.containsKey("__Secure-1PSIDTS")) {
                    SettingsActivity.setCached1psidts(context, psid, cookies.get("__Secure-1PSIDTS"));
                }
            } catch (Exception e) {
                Log.w(TAG, "Auto-refresh failed: " + e.getMessage());
            }
        }, 9, 9, TimeUnit.MINUTES); // default 540s in python
    }

    private String uploadFileReturnId(Map<String, String> cookies, File file) throws IOException {
        RequestBody fileBody = RequestBody.create(okio.Okio.buffer(okio.Okio.source(file)).readByteArray(), MediaType.parse("application/octet-stream"));
        RequestBody multipart = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();
        Request upload = new Request.Builder()
                .url(UPLOAD_URL)
                .header("Push-ID", "feeds/mcudyrk2a4khkz")
                .header("Cookie", buildCookieHeader(cookies))
                .post(multipart)
                .build();
        try (Response r = httpClient.newCall(upload).execute()) {
            if (!r.isSuccessful() || r.body() == null) throw new IOException("Upload failed: " + r.code());
            return r.body().string();
        }
    }

    private static class UploadedRef {
        final String id;
        final String name;
        UploadedRef(String id, String name) { this.id = id; this.name = name; }
    }

    private Headers buildGeminiHeaders(String modelId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.put("Host", "gemini.google.com");
        headers.put("Origin", "https://gemini.google.com");
        headers.put("Referer", "https://gemini.google.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("X-Same-Domain", "1");
        headers.put("Accept", "*/*");
        // Per-model header similar to x-goog-ext-525001261-jspb in reference; minimal without it may still work for flash.
        if ("gemini-2.5-flash".equals(modelId)) {
            headers.put("x-goog-ext-525001261-jspb", "[1,null,null,null,\"71c2d248d3b102ff\"]");
        } else if ("gemini-2.5-pro".equals(modelId)) {
            headers.put("x-goog-ext-525001261-jspb", "[1,null,null,null,\"2525e3954d185b3c\"]");
        } else if ("gemini-2.0-flash".equals(modelId)) {
            headers.put("x-goog-ext-525001261-jspb", "[1,null,null,null,\"f299729663a2343f\"]");
        }
        return Headers.of(headers);
    }

    private RequestBody buildGenerateForm(String accessToken, String prompt, String chatMetadataJsonArray, List<UploadedRef> uploaded) {
        JsonArray inner = new JsonArray();
        if (uploaded != null && !uploaded.isEmpty()) {
            // files payload: [ prompt, 0, null, [ [ [id], name ], ... ] ]
            JsonArray filesEntry = new JsonArray();
            filesEntry.add(prompt);
            filesEntry.add(0);
            filesEntry.add(com.google.gson.JsonNull.INSTANCE);
            JsonArray filesArray = new JsonArray();
            for (UploadedRef ur : uploaded) {
                JsonArray item = new JsonArray();
                JsonArray idArr = new JsonArray();
                idArr.add(ur.id);
                JsonArray pair = new JsonArray();
                pair.add(idArr);
                item.add(pair);
                item.add(ur.name);
                filesArray.add(item);
            }
            filesEntry.add(filesArray);
            inner.add(filesEntry);
        } else {
            JsonArray promptArray = new JsonArray();
            promptArray.add(prompt);
            inner.add(promptArray);
        }
        inner.add(com.google.gson.JsonNull.INSTANCE);
        if (chatMetadataJsonArray != null && !chatMetadataJsonArray.isEmpty()) {
            try {
                inner.add(JsonParser.parseString(chatMetadataJsonArray).getAsJsonArray());
            } catch (Exception e) {
                inner.add(com.google.gson.JsonNull.INSTANCE);
            }
        } else {
            inner.add(com.google.gson.JsonNull.INSTANCE);
        }
        String jsonInner = inner.toString();
        JsonArray outer = new JsonArray();
        outer.add(com.google.gson.JsonNull.INSTANCE);
        outer.add(jsonInner);
        String fReq = outer.toString();

        return new FormBody.Builder()
                .addEncoded("at", accessToken)
                .addEncoded("f.req", fReq)
                .build();
    }

    // Python-style parsing adapted: returns text and thoughts when available
    private ParsedOutput parseOutputFromStream(String responseText) throws IOException {
        try {
            String[] lines = responseText.split("\n");
            if (lines.length < 3) throw new IOException("Unexpected response");
            com.google.gson.JsonArray responseJson = JsonParser.parseString(lines[2]).getAsJsonArray();

            com.google.gson.JsonArray body = null;
            int bodyIndex = 0;
            for (int partIndex = 0; partIndex < responseJson.size(); partIndex++) {
                try {
                    com.google.gson.JsonArray mainPart = JsonParser.parseString(responseJson.get(partIndex).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
                    if (mainPart.size() > 4 && !mainPart.get(4).isJsonNull()) {
                        body = mainPart;
                        bodyIndex = partIndex;
                        break;
                    }
                } catch (Exception ignore) {}
            }
            if (body == null) throw new IOException("Invalid response body");

            com.google.gson.JsonArray candidates = body.get(4).getAsJsonArray();
            if (candidates.size() == 0) throw new IOException("No candidates");

            // First candidate
            com.google.gson.JsonArray cand = candidates.get(0).getAsJsonArray();
            String text = cand.get(1).getAsJsonArray().get(0).getAsString();
            if (text.matches("^http://googleusercontent\\.com/card_content/\\d+")) {
                try {
                    String fallback = cand.get(22).getAsJsonArray().get(0).getAsString();
                    if (fallback != null && !fallback.isEmpty()) text = fallback;
                } catch (Exception ignore) {}
            }
            String thoughts = null;
            try {
                thoughts = cand.get(37).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
            } catch (Exception ignore) {}

            return new ParsedOutput(text, thoughts);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse StreamGenerate response", e);
            throw new IOException("Failed to parse response");
        }
    }

    private String buildExplanationWithThinking(String text, String thoughts) {
        if (thoughts == null || thoughts.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        sb.append(text);
        sb.append("\n\n");
        sb.append("Thinking:\n");
        sb.append(thoughts);
        return sb.toString();
    }

    private Map<String, String> defaultGeminiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Referer", "https://gemini.google.com/");
        headers.put("Origin", "https://gemini.google.com");
        return headers;
    }

    private String buildCookieHeader(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (!first) sb.append("; ");
            first = false;
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private void persistConversationMetaIfAvailable(String modelId, String rawResponse) {
        try {
            String[] lines = rawResponse.split("\n");
            if (lines.length < 3) return;
            com.google.gson.JsonArray responseJson = JsonParser.parseString(lines[2]).getAsJsonArray();
            for (int i = 0; i < responseJson.size(); i++) {
                try {
                    com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
                    // body structure has metadata at [1] -> [cid, rid, rcid] possibly
                    if (part.size() > 1 && part.get(1).isJsonArray()) {
                        String meta = part.get(1).toString();
                        if (meta != null && meta.length() > 2) { // simple validity check
                            SettingsActivity.setFreeConversationMetadata(context, modelId, meta);
                            return;
                        }
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
    }

    private String extractMetadataArrayFromRaw(String rawResponse) {
        try {
            String[] lines = rawResponse.split("\n");
            if (lines.length < 3) return null;
            com.google.gson.JsonArray responseJson = JsonParser.parseString(lines[2]).getAsJsonArray();
            for (int i = 0; i < responseJson.size(); i++) {
                try {
                    com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
                    if (part.size() > 1 && part.get(1).isJsonArray()) {
                        com.google.gson.JsonArray metaArr = part.get(1).getAsJsonArray();
                        // Return JSON string of [cid, rid, rcid] (can be shorter)
                        return metaArr.toString();
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static class ParsedOutput {
        final String text;
        final String thoughts;
        ParsedOutput(String text, String thoughts) {
            this.text = text;
            this.thoughts = thoughts;
        }
    }
}