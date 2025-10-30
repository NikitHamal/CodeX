package com.codex.apk;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Shared SSE client with unified parsing and lifecycle callbacks.
 */
public class SseClient {
    public interface Listener {
        void onOpen();
        void onDelta(JsonObject chunk); // raw provider chunk
        void onUsage(JsonObject usage); // optional usage block
        void onError(String message, int code);
        void onComplete();
    }

    private final OkHttpClient http;
    private Call call;

    public SseClient(OkHttpClient base) {
        // Derive a client with longer read timeout for streaming.
        this.http = base.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
    }

    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }

    public void postStream(String url, okhttp3.Headers headers, JsonObject body, Listener listener) {
        Request req = new Request.Builder()
                .url(url)
                .headers(headers)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .addHeader("accept", "text/event-stream")
                .build();
        call = http.newCall(req);
        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (listener != null) listener.onError(e.getMessage(), -1);
            }
            @Override public void onResponse(Call call, Response response) {
                if (!response.isSuccessful() || response.body() == null) {
                    int code = response.code();
                    String msg;
                    try { msg = response.body() != null ? response.body().string() : null; } catch (Exception ignore) { msg = null; }
                    if (listener != null) listener.onError(msg != null ? msg : ("HTTP " + code), code);
                    try { response.close(); } catch (Exception ignore) {}
                    return;
                }
                if (listener != null) listener.onOpen();
                try (BufferedSource source = response.body().source()) {
                    while (true) {
                        String line;
                        try {
                            line = source.readUtf8LineStrict();
                        } catch (EOFException eof) { break; }
                        catch (java.io.InterruptedIOException timeout) { break; }
                        if (line == null) break;
                        if (line.isEmpty()) {
                            continue; // no buffering; handle each line independently
                        }
                        handleEvent(line, listener);
                    }
                } catch (Exception e) {
                    if (listener != null) listener.onError(e.getMessage(), -1);
                } finally {
                    if (listener != null) listener.onComplete();
                }
            }
        });
    }

    private void handleEvent(String rawEvent, Listener listener) {
        String trimmedBlock = rawEvent == null ? "" : rawEvent.trim();
        if (trimmedBlock.isEmpty()) return;
        // Split into individual lines and process each; many providers emit multiple
        // data: lines without blank separators. We must parse each line separately.
        String[] lines = trimmedBlock.split("\n");
        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("data:")) {
                String jsonPart = trimmed.substring("data:".length()).trim();
                if (jsonPart.isEmpty()) continue;
                if ("[DONE]".equals(jsonPart) || "data: [DONE]".equalsIgnoreCase(jsonPart)) continue;
                try {
                    JsonObject obj = JsonParser.parseString(jsonPart).getAsJsonObject();
                    if (obj.has("usage") && obj.get("usage").isJsonObject()) {
                        if (listener != null) listener.onUsage(obj.getAsJsonObject("usage"));
                    }
                    if (listener != null) listener.onDelta(obj);
                } catch (Exception ignore) { /* malformed partials: still surface raw for debugging/recovery */
                    try {
                        com.google.gson.JsonObject rawObj = new com.google.gson.JsonObject();
                        rawObj.addProperty("_raw", jsonPart);
                        if (listener != null) listener.onDelta(rawObj);
                    } catch (Exception ignored2) {}
                }
                continue;
            }
            // Fallback: initial JSON line with no data: prefix
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    JsonObject obj = JsonParser.parseString(trimmed).getAsJsonObject();
                    if (obj.has("usage") && obj.get("usage").isJsonObject()) {
                        if (listener != null) listener.onUsage(obj.getAsJsonObject("usage"));
                    }
                    if (listener != null) listener.onDelta(obj);
                } catch (Exception ignore) { /* surface raw */
                    try {
                        com.google.gson.JsonObject rawObj = new com.google.gson.JsonObject();
                        rawObj.addProperty("_raw", trimmed);
                        if (listener != null) listener.onDelta(rawObj);
                    } catch (Exception ignored2) {}
                }
            }
        }
    }

    // Synchronous streaming with simple retry/backoff for 429/5xx
    public void postStreamWithRetry(String url, okhttp3.Headers headers, JsonObject body, int maxAttempts, long baseBackoffMs, Listener listener) {
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;
            Request req = new Request.Builder()
                    .url(url)
                    .headers(headers)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .addHeader("accept", "text/event-stream")
                    .build();
            call = http.newCall(req);
            try (Response response = call.execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    int code = response.code();
                    boolean retry = (code == 429 || (code >= 500 && code < 600));
                    if (retry && attempts < maxAttempts) {
                        try { Thread.sleep(baseBackoffMs * attempts); } catch (InterruptedException ignore) {}
                        continue;
                    }
                    String msg;
                    try { msg = response.body() != null ? response.body().string() : null; } catch (Exception ignore) { msg = null; }
                    if (listener != null) listener.onError(msg != null ? msg : ("HTTP " + code), code);
                    return;
                }
                if (listener != null) listener.onOpen();
                try (BufferedSource source = response.body().source()) {
                    while (true) {
                        String line;
                        try {
                            line = source.readUtf8LineStrict();
                        } catch (EOFException eof) { break; }
                        catch (java.io.InterruptedIOException timeout) { break; }
                        if (line == null) break;
                        if (line.isEmpty()) {
                            continue; // no buffering; handle each line independently
                        }
                        handleEvent(line, listener);
                    }
                } finally {
                    if (listener != null) listener.onComplete();
                }
                return;
            } catch (IOException e) {
                if (attempts >= maxAttempts) {
                    if (listener != null) listener.onError(e.getMessage(), -1);
                    return;
                }
                try { Thread.sleep(baseBackoffMs * attempts); } catch (InterruptedException ignore) {}
            }
        }
    }
}
