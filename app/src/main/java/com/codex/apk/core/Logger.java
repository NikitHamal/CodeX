package com.codex.apk.core;

import android.util.Log;

/**
 * Centralized logger wrapper to standardize logging across the app.
 * - Adds unified tags
 * - Provides debug flags and structured helpers
 */
public final class Logger {
    private static volatile boolean ENABLE_DEBUG = true;
    private static final String GLOBAL_TAG = "CodeX";

    private Logger() {}

    public static void setDebug(boolean enabled) { ENABLE_DEBUG = enabled; }

    public static void d(String tag, String msg) {
        if (ENABLE_DEBUG) Log.d(compose(tag), safe(msg));
    }

    public static void i(String tag, String msg) {
        Log.i(compose(tag), safe(msg));
    }

    public static void w(String tag, String msg) {
        Log.w(compose(tag), safe(msg));
    }

    public static void e(String tag, String msg) {
        Log.e(compose(tag), safe(msg));
    }

    public static void e(String tag, String msg, Throwable t) {
        Log.e(compose(tag), safe(msg), t);
    }

    private static String compose(String tag) {
        if (tag == null || tag.isEmpty()) return GLOBAL_TAG;
        return GLOBAL_TAG + "/" + tag;
    }

    private static String safe(String msg) {
        return msg == null ? "" : msg;
    }
}
