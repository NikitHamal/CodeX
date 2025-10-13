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

    public SseClient(OkHttpClient base) {
        // Derive a client with longer read timeout for streaming.
        this.http = base.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
    }

    public void postStream(String url, okhttp3.Headers headers, JsonObject body, Listener listener) {
        Request req = new Request.Builder()
                .url(url)
                .headers(headers)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .addHeader("accept", "text/event-stream")
                .build();
        http.newCall(req).enqueue(new Callback() {
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
                    StringBuilder eventBuf = new StringBuilder();
                    while (true) {
                        String line;
                        try {
                            line = source.readUtf8LineStrict();
                        } catch (EOFException eof) { break; }
                        catch (java.io.InterruptedIOException timeout) { break; }
                        if (line == null) break;
                        if (line.isEmpty()) {
                            handleEvent(eventBuf.toString(), listener);
                            eventBuf.setLength(0);
                            continue;
                        }
                        eventBuf.append(line).append('\n');
                    }
                    if (eventBuf.length() > 0) handleEvent(eventBuf.toString(), listener);
                } catch (Exception e) {
                    if (listener != null) listener.onError(e.getMessage(), -1);
                } finally {
                    if (listener != null) listener.onComplete();
                }
            }
        });
    }

    private void handleEvent(String rawEvent, Listener listener) {
        String prefix = "data:";
        int idx = rawEvent.indexOf(prefix);
        if (idx < 0) return;
        String jsonPart = rawEvent.substring(idx + prefix.length()).trim();
        if (jsonPart.isEmpty() || jsonPart.equals("[DONE]") || jsonPart.equalsIgnoreCase("data: [DONE]")) return;
        try {
            JsonObject obj = JsonParser.parseString(jsonPart).getAsJsonObject();
            if (obj.has("usage") && obj.get("usage").isJsonObject()) {
                if (listener != null) listener.onUsage(obj.getAsJsonObject("usage"));
            }
            if (listener != null) listener.onDelta(obj);
        } catch (Exception ignore) {}
    }
}
