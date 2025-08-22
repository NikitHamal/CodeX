package com.codex.apk.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lightweight preferences facade. Centralizes keys and access patterns
 * while remaining compatible with existing keys used across the app.
 */
public final class Prefs {
    private static final String SETTINGS = "settings";

    // Existing keys (do not rename to preserve compatibility)
    public static final String KEY_GEMINI_API = "gemini_api_key";
    public static final String KEY_SECURE_1PSID = "secure_1psid";
    public static final String KEY_SECURE_1PSIDTS = "secure_1psidts";

    private Prefs() {}

    public static SharedPreferences of(Context ctx) {
        return ctx.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
    }

    public static String getString(Context ctx, String key, String def) {
        return of(ctx).getString(key, def);
    }

    public static void putString(Context ctx, String key, String value) {
        of(ctx).edit().putString(key, value).apply();
    }
}
