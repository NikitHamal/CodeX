package com.codex.apk;

import android.content.Context;
import android.util.Log;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reverse-engineered Gemini client using cookies (__Secure-1PSID, __Secure-1PSIDTS).
 * Minimal implementation: text prompt -> text response.
 */
public class GeminiFreeApiClient implements ApiClient {
    private static final String TAG = "GeminiFreeApiClient";

    private static final String INIT_URL = "https://gemini.google.com/app";
    private static final String GOOGLE_URL = "https://www.google.com";
    private static final String GENERATE_URL = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";

    private final Context context;
    private final AIAssistant.AIActionListener actionListener;
    private final OkHttpClient httpClient;

    public GeminiFreeApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        this.context = context.getApplicationContext();
        this.actionListener = actionListener;
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void sendMessage(String message, AIModel model, List<ChatMessage> history, QwenConversationState unused, boolean thinkingModeEnabled, boolean webSearchEnabled, List<ToolSpec> enabledTools, List<java.io.File> attachments) {
        new Thread(() -> {
            try {
                if (actionListener != null) actionListener.onAiRequestStarted();
                String psid = SettingsActivity.getSecure1PSID(context);
                String psidts = SettingsActivity.getSecure1PSIDTS(context);
                if (psid == null || psid.isEmpty()) {
                    if (actionListener != null) actionListener.onAiError("__Secure-1PSID cookie not set in Settings");
                    return;
                }
                // Step 1: warm up google.com to obtain extra cookies (NID etc.)
                Map<String, String> baseCookies = new HashMap<>();
                baseCookies.put("__Secure-1PSID", psid);
                if (psidts != null && !psidts.isEmpty()) baseCookies.put("__Secure-1PSIDTS", psidts);

                Map<String, String> cookies = new HashMap<>(baseCookies);
                // Always include provided cookies in Cookie header for INIT/GENERATE
                try (Response r = httpClient.newCall(new Request.Builder().url(GOOGLE_URL).get().build()).execute()) {
                    if (r.headers("Set-Cookie") != null) {
                        for (String c : r.headers("Set-Cookie")) {
                            String[] parts = c.split(";", 2);
                            String[] kv = parts[0].split("=", 2);
                            if (kv.length == 2) cookies.put(kv[0], kv[1]);
                        }
                    }
                }

                // Step 2: fetch SNlM0e access token from INIT page with cookies
                String accessToken = fetchAccessToken(cookies);
                if (accessToken == null) {
                    if (actionListener != null) actionListener.onAiError("Failed to retrieve access token from Gemini INIT page");
                    return;
                }

                // Step 3: POST to StreamGenerate with model header and f.req
                String modelId = model != null ? model.getModelId() : "gemini-2.5-flash";
                Headers requestHeaders = buildGeminiHeaders(modelId);
                RequestBody formBody = buildGenerateForm(accessToken, message, null);
                Request req = new Request.Builder()
                        .url(GENERATE_URL)
                        .headers(requestHeaders)
                        .header("Cookie", buildCookieHeader(cookies))
                        .post(formBody)
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        if (actionListener != null) actionListener.onAiError("Gemini request failed: " + resp.code());
                        return;
                    }
                    String body = resp.body().string();
                    String text = parseStreamGenerateResponse(body);
                    if (actionListener != null) {
                        actionListener.onAiActionsProcessed(null, text, new ArrayList<>(), new ArrayList<>(), model != null ? model.getDisplayName() : "Gemini (Free)");
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
            String html = resp.body().string();
            // Extract "SNlM0e":"<token>"
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"SNlM0e\":\"(.*?)\"").matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private Headers buildGeminiHeaders(String modelId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.put("Host", "gemini.google.com");
        headers.put("Origin", "https://gemini.google.com");
        headers.put("Referer", "https://gemini.google.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("X-Same-Domain", "1");
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

    private RequestBody buildGenerateForm(String accessToken, String prompt, List<String> files) {
        // Build f.req according to reference: [null, json.dumps([ prompt_or_files, null, chat_metadata ])]
        // We send minimal: prompt only.
        JsonArray inner = new JsonArray();
        inner.add(prompt);
        inner.add(com.google.gson.JsonNull.INSTANCE);
        inner.add(com.google.gson.JsonNull.INSTANCE);
        String jsonInner = inner.toString();
        JsonArray outer = new JsonArray();
        outer.add(com.google.gson.JsonNull.INSTANCE);
        outer.add(jsonInner);
        String fReq = outer.toString();

        return new FormBody.Builder()
                .add("at", accessToken)
                .add("f.req", fReq)
                .build();
    }

    private String parseStreamGenerateResponse(String responseText) throws IOException {
        // Response is text with several JSON lines; the 3rd line (index 2) contains the array; then find body part having [4]
        try {
            String[] lines = responseText.split("\n");
            if (lines.length < 3) throw new IOException("Unexpected response");
            com.google.gson.JsonArray responseJson = JsonParser.parseString(lines[2]).getAsJsonArray();
            com.google.gson.JsonArray body = null;
            int bodyIndex = -1;
            for (int i = 0; i < responseJson.size(); i++) {
                try {
                    com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
                    if (part.size() > 4 && !part.get(4).isJsonNull()) {
                        body = part;
                        bodyIndex = i;
                        break;
                    }
                } catch (Exception ignore) {}
            }
            if (body == null) throw new IOException("Invalid body");
            com.google.gson.JsonArray candidates = body.get(4).getAsJsonArray();
            if (candidates.size() == 0) throw new IOException("No candidates");
            // First candidate text at [1][0]
            return candidates.get(0).getAsJsonArray().get(1).getAsJsonArray().get(0).getAsString();
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
}