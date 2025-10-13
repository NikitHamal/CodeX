package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class QwenMidTokenManager {
    private static final String TAG = "QwenMidTokenManager";
    private static final String PREFS_NAME = "ai_chat_prefs";
    private static final String QWEN_MIDTOKEN_KEY = "qwen_midtoken";
    private static final Pattern MIDTOKEN_PATTERN = Pattern.compile("(?:umx\\.wu|__fycb)\\('([^']+)'\\)");

    private final OkHttpClient httpClient;
    private final SharedPreferences sharedPreferences;
    private volatile String midToken = null;
    private int midTokenUses = 0;
    private long midTokenCreatedAtMs = 0L;

    private static final int MAX_USES = 20; // refresh after 20 uses
    private static final long MAX_AGE_MS = 5 * 60 * 1000L; // refresh after 5 minutes

    public QwenMidTokenManager(Context context, OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            this.midToken = sharedPreferences.getString(QWEN_MIDTOKEN_KEY, null);
            if (this.midToken != null) {
                this.midTokenUses = 0;
                this.midTokenCreatedAtMs = System.currentTimeMillis();
                Log.i(TAG, "Loaded persisted midtoken.");
            }
        } catch (Exception ignored) {}
    }

    public synchronized String ensureMidToken(boolean forceRefresh) throws IOException {
        if (forceRefresh) {
            Log.w(TAG, "Force refreshing midtoken");
            this.midToken = null;
            this.midTokenUses = 0;
            sharedPreferences.edit().remove(QWEN_MIDTOKEN_KEY).apply();
        }
        if (midToken != null) {
            long age = System.currentTimeMillis() - midTokenCreatedAtMs;
            if (midTokenUses < MAX_USES && age < MAX_AGE_MS) {
                midTokenUses++;
                Log.i(TAG, "Reusing midtoken. Use count: " + midTokenUses + ", ageMs=" + age);
                return midToken;
            } else {
                Log.i(TAG, "Midtoken expired (uses=" + midTokenUses + ", ageMs=" + age + ") refreshing...");
                this.midToken = null;
                this.midTokenUses = 0;
                sharedPreferences.edit().remove(QWEN_MIDTOKEN_KEY).apply();
            }
        }

        Log.i(TAG, "No active midtoken. Fetching a new one...");
        Request req = new Request.Builder()
                .url("https://sg-wum.alibaba.com/w/wu.json")
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .addHeader("Accept", "*/*")
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new IOException("Failed to fetch midtoken: HTTP " + resp.code());
            }
            String text = resp.body().string();
            Matcher m = MIDTOKEN_PATTERN.matcher(text);
            if (!m.find()) {
                throw new IOException("Failed to extract bx-umidtoken");
            }
            midToken = m.group(1);
            midTokenUses = 1;
            midTokenCreatedAtMs = System.currentTimeMillis();
            try { sharedPreferences.edit().putString(QWEN_MIDTOKEN_KEY, midToken).apply(); } catch (Exception ignore) {}
            Log.i(TAG, "Obtained and saved new midtoken. Use count: 1");
            return midToken;
        }
    }
}