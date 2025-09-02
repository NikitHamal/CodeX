package com.codex.apk.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AIModel {

    private final String modelId;
    private final String displayName;
    private final AIProvider provider;
    private final ModelCapabilities capabilities;

    // Store models in a thread-safe map, keyed by provider for efficient lookup.
    private static final Map<AIProvider, List<AIModel>> modelsByProvider = new ConcurrentHashMap<>();
    private static final List<AIModel> customModels = new ArrayList<>();

    // Static initializer to populate the initial set of models
    static {
        loadCustomModels();
        List<AIModel> initialModels = new ArrayList<>(Arrays.asList(
            // Google Models
            new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
            new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
            new AIModel("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.0-flash-exp", "Gemini 2.0 Flash Experimental", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
            new AIModel("gemini-2.0-flash-thinking", "Gemini 2.0 Flash Thinking", AIProvider.GOOGLE, new ModelCapabilities(true, false, true, true, true, true, true, 1048576, 8192)),
            // Updated Alibaba/Qwen Models (from the new JSON data)
            new AIModel("qwen3-235b-a22b", "Qwen3-235B-A22B-2507", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 81920)),
            new AIModel("qwen3-coder-plus", "Qwen3-Coder", AIProvider.ALIBABA, new ModelCapabilities(false, true, true, true, true, true, true, 1048576, 65536)),
            new AIModel("qwen3-30b-a3b", "Qwen3-30B-A3B-2507", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 32768)),
            new AIModel("qwen3-coder-30b-a3b-instruct", "Qwen3-Coder-Flash", AIProvider.ALIBABA, new ModelCapabilities(false, true, true, true, true, true, true, 262144, 65536)),
            new AIModel("qwen-max-latest", "Qwen2.5-Max", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
            new AIModel("qwen-plus-2025-01-25", "Qwen2.5-Plus", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
            new AIModel("qwq-32b", "QwQ-32B", AIProvider.ALIBABA, new ModelCapabilities(true, true, false, true, false, false, false, 131072, 8192)),
            new AIModel("qwen-turbo-2025-02-11", "Qwen2.5-Turbo", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 1000000, 8192)),
            new AIModel("qwen2.5-omni-7b", "Qwen2.5-Omni-7B", AIProvider.ALIBABA, new ModelCapabilities(false, false, true, true, true, true, true, 30720, 2048)),
            new AIModel("qvq-72b-preview-0310", "QVQ-Max", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 131072, 8192)),
            new AIModel("qwen2.5-vl-32b-instruct", "Qwen2.5-VL-32B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 131072, 8192)),
            new AIModel("qwen2.5-14b-instruct-1m", "Qwen2.5-14B-Instruct-1M", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 1000000, 8192)),
            new AIModel("qwen2.5-coder-32b-instruct", "Qwen2.5-Coder-32B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 131072, 8192)),
            new AIModel("qwen2.5-72b-instruct", "Qwen2.5-72B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 131072, 8192)),

            // Api.Airforce Provider (OpenAI-compatible free endpoint)
            new AIModel("openai", "Api.Airforce OpenAI", AIProvider.AIRFORCE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),

            // DeepInfra (OpenAI-compatible chat)
            new AIModel("deepseek-v3", "DeepInfra DeepSeek V3", AIProvider.DEEPINFRA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),

            // Cookies Provider (Gemini reverse-engineered via cookies)
            new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
            new AIModel("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),

            // Zhipu Provider (default model)
            new AIModel("GLM-4.5", "Zhipu GLM-4.5", AIProvider.ZHIPU, new ModelCapabilities(true, false, false, false, false, false, false, false, false, false, 0, 0, 0, 0, new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>())),

            // OIVSCodeSer0501 Provider
            new AIModel("gpt-4.1-mini", "OIVSCodeSer0501 gpt-4.1-mini", AIProvider.OIVSCodeSer0501, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),

            // OIVSCodeSer2 Provider
            new AIModel("gpt-4o-mini", "OIVSCodeSer2 gpt-4o-mini", AIProvider.OIVSCodeSer2, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),

            // WeWordle Provider
            new AIModel("gpt-4", "WeWordle GPT-4", AIProvider.WEWORDLE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192))
        ));
        for (AIModel model : initialModels) {
            modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
        }
        applyPersistentDeletionsAndOverrides();
    }

    public AIModel(String modelId, String displayName, AIProvider provider, ModelCapabilities capabilities) {
        this.modelId = modelId;
        this.displayName = displayName;
        this.provider = provider;
        this.capabilities = capabilities;
    }

    public String getModelId() { return modelId; }
    public String getDisplayName() { return displayName; }
    public AIProvider getProvider() { return provider; }
    public ModelCapabilities getCapabilities() { return capabilities; }
    public boolean supportsVision() { return capabilities.supportsVision; }
    public boolean supportsFunctionCalling() { return true; }

    public static void addCustomModel(AIModel model) {
        customModels.add(model);
        // Reflect immediately in in-memory map to show in UI without duplication
        upsertModel(model);
        saveCustomModels();
    }

    private static void saveCustomModels() {
        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
        if (context == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("custom_models", android.content.Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(customModels);
        prefs.edit().putString("models", json).apply();
    }

    private static void loadCustomModels() {
        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
        if (context == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("custom_models", android.content.Context.MODE_PRIVATE);
        String json = prefs.getString("models", null);
        if (json != null) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            AIModel[] models = gson.fromJson(json, AIModel[].class);
            if (models != null) {
                customModels.clear();
                customModels.addAll(java.util.Arrays.asList(models));
            }
        }
    }

    private static void applyPersistentDeletionsAndOverrides() {
        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
        if (context == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        // Load previously fetched models per provider and replace in-memory lists where present
        try {
            for (AIProvider p : AIProvider.values()) {
                String key = "fetched_models_" + p.name();
                String fetchedJson = prefs.getString(key, null);
                if (fetchedJson != null && !fetchedJson.isEmpty()) {
                    SimpleModel[] arr = gson.fromJson(fetchedJson, SimpleModel[].class);
                    if (arr != null && arr.length > 0) {
                        java.util.List<AIModel> restored = new java.util.ArrayList<>();
                        for (SimpleModel sm : arr) {
                            // Preserve existing caps if known, else default to chat-only
                            AIModel existing = findByDisplayName(sm.displayName);
                            ModelCapabilities caps = existing != null ? existing.getCapabilities() : new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
                            restored.add(new AIModel(sm.modelId, sm.displayName, AIProvider.valueOf(sm.provider), caps));
                        }
                        modelsByProvider.put(p, restored);
                    }
                }
            }
        } catch (Exception ignored) {}
        // Deletions
        String deletedJson = prefs.getString("deleted_models", null);
        java.util.Set<String> deleted = new java.util.HashSet<>();
        if (deletedJson != null) {
            try {
                String[] arr = gson.fromJson(deletedJson, String[].class);
                if (arr != null) deleted.addAll(java.util.Arrays.asList(arr));
            } catch (Exception ignored) {}
        }
        // Remove deleted from map
        if (!deleted.isEmpty()) {
            for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
                e.getValue().removeIf(m -> deleted.contains(m.getDisplayName()));
            }
        }
        // Overrides
        String overridesJson = prefs.getString("model_overrides", null);
        if (overridesJson != null) {
            try {
                SimpleModel[] overrides = gson.fromJson(overridesJson, SimpleModel[].class);
                if (overrides != null) {
                    for (SimpleModel sm : overrides) {
                        AIModel existing = findByDisplayName(sm.displayName);
                        ModelCapabilities caps = existing != null ? existing.getCapabilities() : new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
                        AIModel updated = new AIModel(sm.modelId, sm.displayName, AIProvider.valueOf(sm.provider), caps);
                        upsertModel(updated);
                    }
                }
            } catch (Exception ignored) {}
        }
        // Add custom models into map (they are separate storage)
        for (AIModel cm : customModels) {
            upsertModel(cm);
        }
    }

    private static AIModel findByDisplayName(String displayName) {
        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
            for (AIModel m : e.getValue()) {
                if (m.getDisplayName().equals(displayName)) return m;
            }
        }
        return null;
    }

    private static void upsertModel(AIModel model) {
        // Remove any existing entry with same display name across providers
        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
            e.getValue().removeIf(m -> m.getDisplayName().equals(model.getDisplayName()));
        }
        modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
    }

    public static void removeModelByDisplayName(String displayName) {
        // Record deletion
        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
        if (context == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String deletedJson = prefs.getString("deleted_models", null);
        java.util.Set<String> deleted = new java.util.HashSet<>();
        if (deletedJson != null) {
            try { String[] arr = gson.fromJson(deletedJson, String[].class); if (arr != null) deleted.addAll(java.util.Arrays.asList(arr)); } catch (Exception ignored) {}
        }
        deleted.add(displayName);
        prefs.edit().putString("deleted_models", gson.toJson(deleted.toArray(new String[0]))).apply();
        // Apply in-memory removal
        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
            e.getValue().removeIf(m -> m.getDisplayName().equals(displayName));
        }
    }

    public static void updateModel(String oldDisplayName, String newDisplayName, String newModelId, AIProvider provider) {
        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
        if (context == null) return;
        AIModel existing = findByDisplayName(oldDisplayName);
        ModelCapabilities caps = existing != null ? existing.getCapabilities() : new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
        AIModel updated = new AIModel(newModelId, newDisplayName, provider, caps);
        // Persist override
        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String overridesJson = prefs.getString("model_overrides", null);
        java.util.List<SimpleModel> overrides = new java.util.ArrayList<>();
        if (overridesJson != null) {
            try { SimpleModel[] arr = gson.fromJson(overridesJson, SimpleModel[].class); if (arr != null) overrides.addAll(java.util.Arrays.asList(arr)); } catch (Exception ignored) {}
        }
        // Remove old if exists
        overrides.removeIf(sm -> sm.displayName.equals(oldDisplayName));
        overrides.add(new SimpleModel(updated.getModelId(), updated.getDisplayName(), updated.getProvider().name()));
        prefs.edit().putString("model_overrides", gson.toJson(overrides)).apply();
        // Apply in-memory
        upsertModel(updated);
    }

    private static class SimpleModel {
        String modelId; String displayName; String provider;
        SimpleModel() {}
        SimpleModel(String id, String name, String provider) { this.modelId = id; this.displayName = name; this.provider = provider; }
    }

    public static List<AIModel> getAllModels() {
        List<AIModel> allModels = new ArrayList<>();
        for (List<AIModel> modelList : modelsByProvider.values()) {
            allModels.addAll(modelList);
        }
        return allModels;
    }

    public static List<AIModel> values() {
        return getAllModels();
    }

    public static List<String> getAllDisplayNames() {
        List<String> displayNames = new ArrayList<>();
        for (AIModel model : values()) {
            displayNames.add(model.getDisplayName());
        }
        return displayNames;
    }

    public static Map<AIProvider, List<AIModel>> getModelsByProvider() {
        return new HashMap<>(modelsByProvider); // Return a copy
    }

    public static void updateModelsForProvider(AIProvider provider, List<AIModel> newModels) {
        modelsByProvider.put(provider, new ArrayList<>(newModels));
        // Persist a lightweight list so models survive app restarts
        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
        if (context != null) {
            try {
                java.util.List<SimpleModel> simple = new java.util.ArrayList<>();
                for (AIModel m : newModels) {
                    simple.add(new SimpleModel(m.getModelId(), m.getDisplayName(), m.getProvider().name()));
                }
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String json = gson.toJson(simple.toArray(new SimpleModel[0]));
                android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
                prefs.edit().putString("fetched_models_" + provider.name(), json).apply();
            } catch (Exception ignored) {}
        }
    }

    public static AIModel fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (AIModel model : values()) {
            if (displayName.equals(model.getDisplayName())) {
                return model;
            }
        }
        return null;
    }

    public static AIModel fromModelId(String modelId) {
        if (modelId == null) return null;
        for (AIModel model : values()) {
            if (modelId.equals(model.getModelId())) {
                return model;
            }
        }
        return null;
    }
}
