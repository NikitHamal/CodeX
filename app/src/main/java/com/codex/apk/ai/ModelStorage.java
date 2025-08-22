package com.codex.apk.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.codex.apk.CodeXApplication;
import com.google.gson.Gson;

/**
 * Central storage for model-related persistence in SharedPreferences.
 * Keeps keys and JSON plumbing in one place.
 */
public final class ModelStorage {
    private static final String PREF_MODELS = "model_settings";
    private static final String PREF_CUSTOM = "custom_models";

    private static final Gson gson = new Gson();

    private ModelStorage() {}

    private static SharedPreferences prefs() {
        Context c = CodeXApplication.getAppContext();
        if (c == null) throw new IllegalStateException("App context not ready");
        return c.getSharedPreferences(PREF_MODELS, Context.MODE_PRIVATE);
    }

    public static void putFetchedModelsJson(String providerName, String json) {
        prefs().edit().putString("fetched_models_" + providerName, json).apply();
    }

    public static String getFetchedModelsJson(String providerName) {
        return prefs().getString("fetched_models_" + providerName, null);
    }

    public static void putDeletedModelsJson(String json) {
        prefs().edit().putString("deleted_models", json).apply();
    }

    public static String getDeletedModelsJson() {
        return prefs().getString("deleted_models", null);
    }

    public static void putOverridesJson(String json) {
        prefs().edit().putString("model_overrides", json).apply();
    }

    public static String getOverridesJson() {
        return prefs().getString("model_overrides", null);
    }

    public static void putCustomModelsJson(String json) {
        Context c = CodeXApplication.getAppContext();
        if (c == null) return;
        c.getSharedPreferences(PREF_CUSTOM, Context.MODE_PRIVATE).edit().putString("models", json).apply();
    }

    public static String getCustomModelsJson() {
        Context c = CodeXApplication.getAppContext();
        if (c == null) return null;
        return c.getSharedPreferences(PREF_CUSTOM, Context.MODE_PRIVATE).getString("models", null);
    }
}
