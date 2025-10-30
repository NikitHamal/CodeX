package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.util.JsonUtils;
import com.codex.apk.util.ResponseUtils;
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
public class GeminiFreeApiClient implements StreamingApiClient {
    private static final String TAG = "GeminiFreeApiClient";

    private static final String INIT_URL = "https://gemini.google.com/app";
    private static final String GOOGLE_URL = "https://www.google.com";
    private static final String GENERATE_URL = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String ROTATE_COOKIES_URL = "https://accounts.google.com/RotateCookies";
    private static final String UPLOAD_URL = "https://content-push.googleapis.com/upload";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private OkHttpClient httpClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean refreshRunning = false;
    private final Map<String, okhttp3.Call> activeStreams = new HashMap<>();

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
    public List<AIModel> fetchModels() {
        // Static supported list for COOKIES provider (cookie-based Gemini)
        List<AIModel> list = new ArrayList<>();
        list.add(new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.COOKIES, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)));
        list.add(new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.COOKIES, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)));
        list.add(new AIModel("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.COOKIES, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)));
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
                .headers(Headers.of(new HashMap<String, String>() {{ put("Push-ID", "feeds/mcudyrk2a4khkz"); }}))
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
                // Expected shape per python client: [ [id], name ]
                item.add(idArr);
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
        // second element must be null placeholder
        inner.add(com.google.gson.JsonNull.INSTANCE);
        // third element: chat metadata array (e.g., [cid, rid, rcid])
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

        // Gemini expects normal form encoding (not URL double-encoded JSON). Use add instead of addEncoded.
        return new FormBody.Builder()
                .add("at", accessToken)
                .add("f.req", fReq)
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

    private void notifyAiActionsProcessed(String rawAiResponseJson,
                                          String explanation,
                                          List<String> suggestions,
                                          List<ChatMessage.FileActionDetail> fileActions,
                                          String modelDisplayName,
                                          String thinking,
                                          List<com.codex.apk.ai.WebSource> sources) {

        String jsonToParse = JsonUtils.extractJsonFromCodeBlock(explanation);
        if (jsonToParse == null && JsonUtils.looksLikeJson(explanation)) {
            jsonToParse = explanation;
        }

        if (jsonToParse != null) {
            try {
                QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(jsonToParse);
                if (parsed != null && parsed.isValid) {
                    if ("plan".equals(parsed.action) && parsed.planSteps != null && !parsed.planSteps.isEmpty()) {
                        List<ChatMessage.PlanStep> planSteps = QwenResponseParser.toPlanSteps(parsed);
                        actionListener.onAiActionsProcessed(rawAiResponseJson, parsed.explanation, suggestions, new ArrayList<>(), planSteps, modelDisplayName);
                        return;
                    } else {
                        fileActions = QwenResponseParser.toFileActionDetails(parsed);
                        explanation = parsed.explanation;
                    }
                }
            } catch (Exception e) {
                // Fallback to default behavior
            }
        }

        if (actionListener instanceof com.codex.apk.editor.AiAssistantManager) {
            ((com.codex.apk.editor.AiAssistantManager) actionListener).onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, fileActions, modelDisplayName, thinking, sources);
        } else {
            String fallback = ResponseUtils.buildExplanationWithThinking(explanation, thinking);
            actionListener.onAiActionsProcessed(rawAiResponseJson, fallback, suggestions, fileActions, modelDisplayName);
        }
    }

    private static class ParsedOutput {
        final String text;
        final String thoughts;
        ParsedOutput(String text, String thoughts) {
            this.text = text;
            this.thoughts = thoughts;
        }
    }

    @Override
    public void sendMessageStreaming(MessageRequest request, StreamListener listener) {
        new Thread(() -> {
            try {
                listener.onStreamStarted(request.getRequestId());

                String psid = SettingsActivity.getSecure1PSID(context);
                String psidts = SettingsActivity.getSecure1PSIDTS(context);
                if (psid == null || psid.isEmpty()) {
                    listener.onStreamError(request.getRequestId(), "__Secure-1PSID cookie not set", null);
                    return;
                }

                Map<String, String> cookies = warmupAndMergeCookies(new HashMap<String, String>() {{ put("__Secure-1PSID", psid); if (psidts != null) put("__Secure-1PSIDTS", psidts); }});
                String accessToken = fetchAccessToken(cookies);
                if (accessToken == null) {
                    listener.onStreamError(request.getRequestId(), "Failed to retrieve access token", null);
                    return;
                }

                String modelId = request.getModel() != null ? request.getModel().getModelId() : "gemini-2.5-flash";
                RequestBody formBody = buildGenerateForm(accessToken, request.getMessage(), null, null);
                Request req = new Request.Builder()
                        .url(GENERATE_URL)
                        .headers(buildGeminiHeaders(modelId))
                        .header("Cookie", buildCookieHeader(cookies))
                        .post(formBody)
                        .build();

                okhttp3.Call call = httpClient.newCall(req);
                activeStreams.put(request.getRequestId(), call);

                try (Response resp = call.execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        listener.onStreamError(request.getRequestId(), "HTTP " + resp.code(), null);
                        return;
                    }

                    BufferedSource source = resp.body().source();
                    StringBuilder fullResponse = new StringBuilder();
                    StringBuilder fullText = new StringBuilder();
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) break;
                        fullResponse.append(line).append("\n");
                        try {
                            // In this custom protocol, each line is not a full JSON object,
                            // so we must parse the whole thing to get the latest text.
                            // This is inefficient but required by the API's design.
                            ParsedOutput partial = parseOutputFromStream(fullResponse.toString());
                            if (partial.text.length() > fullText.length()) {
                                listener.onStreamPartialUpdate(request.getRequestId(), partial.text, false);
                                fullText.setLength(0);
                                fullText.append(partial.text);
                            }
                        } catch (Exception ignore) {}
                    }

                    ParsedOutput finalParsed = parseOutputFromStream(fullResponse.toString());
                    QwenResponseParser.ParsedResponse finalResponse = new QwenResponseParser.ParsedResponse();
                    finalResponse.action = "message";
                    finalResponse.explanation = finalParsed.text;
                    finalResponse.rawResponse = fullResponse.toString();
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
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .connectionPool(pool)
                .build();
    }
}