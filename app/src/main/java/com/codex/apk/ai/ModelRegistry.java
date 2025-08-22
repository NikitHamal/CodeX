package com.codex.apk.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.codex.apk.CodeXApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade for model discovery, selection and persistence. Initially delegates
 * to the existing static methods on AIModel to avoid breaking callers.
 */
public final class ModelRegistry {
    private ModelRegistry() {}

    private static final Map<AIProvider, List<AIModel>> modelsByProvider = new ConcurrentHashMap<>();
    private static final List<AIModel> customModels = new ArrayList<>();
    private static volatile boolean bootstrapped = false;

    private static void bootstrapOnce() {
        if (bootstrapped) return;
        synchronized (ModelRegistry.class) {
            if (bootstrapped) return;
            com.google.gson.Gson gson = new com.google.gson.Gson();
            // 1) Load fetched models per provider
            for (AIProvider p : AIProvider.values()) {
                String fetchedJson = ModelStorage.getFetchedModelsJson(p.name());
                if (fetchedJson != null && !fetchedJson.isEmpty()) {
                    try {
                        SimpleModel[] arr = gson.fromJson(fetchedJson, SimpleModel[].class);
                        if (arr != null && arr.length > 0) {
                            List<AIModel> restored = new ArrayList<>();
                            for (SimpleModel sm : arr) {
                                ModelCapabilities caps = defaultChatCaps();
                                restored.add(new AIModel(sm.modelId, sm.displayName, AIProvider.valueOf(sm.provider), caps));
                            }
                            modelsByProvider.put(p, restored);
                        }
                    } catch (Exception ignored) {}
                }
            }
            // 2) Apply deletions
            try {
                String deletedJson = ModelStorage.getDeletedModelsJson();
                if (deletedJson != null) {
                    String[] arr = gson.fromJson(deletedJson, String[].class);
                    if (arr != null && arr.length > 0) {
                        java.util.Set<String> deleted = new java.util.HashSet<>(java.util.Arrays.asList(arr));
                        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
                            e.getValue().removeIf(m -> deleted.contains(m.getDisplayName()));
                        }
                    }
                }
            } catch (Exception ignored) {}
            // 3) Apply overrides (replace or insert)
            try {
                String overridesJson = ModelStorage.getOverridesJson();
                if (overridesJson != null) {
                    SimpleModel[] overrides = gson.fromJson(overridesJson, SimpleModel[].class);
                    if (overrides != null) {
                        for (SimpleModel sm : overrides) {
                            ModelCapabilities caps = defaultChatCaps();
                            AIModel updated = new AIModel(sm.modelId, sm.displayName, AIProvider.valueOf(sm.provider), caps);
                            upsertModel(updated);
                        }
                    }
                }
            } catch (Exception ignored) {}
            // 4) Merge custom models
            try {
                String customJson = ModelStorage.getCustomModelsJson();
                if (customJson != null) {
                    AIModel[] custom = gson.fromJson(customJson, AIModel[].class);
                    if (custom != null) {
                        for (AIModel cm : custom) upsertModel(cm);
                    }
                }
            } catch (Exception ignored) {}
            // 5) If empty, add a minimal seed to prevent empty UI on first run
            boolean empty = true;
            for (List<AIModel> list : modelsByProvider.values()) { if (list != null && !list.isEmpty()) { empty = false; break; } }
            if (empty) addMinimalSeed();
            bootstrapped = true;
        }
    }

    private static ModelCapabilities defaultChatCaps() {
        return new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
    }

    private static void addMinimalSeed() {
        // Small, safe default set
        upsertModel(new AIModel("qwen3-coder-plus", "Qwen3-Coder", AIProvider.ALIBABA, defaultChatCaps()));
        upsertModel(new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.GOOGLE, defaultChatCaps()));
        upsertModel(new AIModel("glm-4-plus", "GLM-4-Plus", AIProvider.Z, defaultChatCaps()));
        upsertModel(new AIModel("openai", "Api.Airforce OpenAI", AIProvider.AIRFORCE, defaultChatCaps()));
        upsertModel(new AIModel("llama-3.3-70b", "Cloudflare Llama 3.3 70B", AIProvider.CLOUDFLARE, defaultChatCaps()));
        upsertModel(new AIModel("gpt-oss-120b", "GPT OSS 120B", AIProvider.GPT_OSS, defaultChatCaps()));
    }

    public static List<AIModel> all() {
        bootstrapOnce();
        List<AIModel> allModels = new ArrayList<>();
        for (List<AIModel> list : modelsByProvider.values()) allModels.addAll(list);
        return allModels;
    }

    public static List<String> displayNames() {
        List<String> names = new ArrayList<>();
        for (AIModel m : all()) names.add(m.getDisplayName());
        return names;
    }

    public static AIModel byId(String id) {
        if (id == null) return null;
        for (AIModel m : all()) if (id.equals(m.getModelId())) return m;
        return null;
    }
    public static AIModel byName(String name) {
        if (name == null) return null;
        for (AIModel m : all()) if (name.equals(m.getDisplayName())) return m;
        return null;
    }

    public static Map<AIProvider, List<AIModel>> byProvider() {
        bootstrapOnce();
        return new HashMap<>(modelsByProvider);
    }

    public static void updateForProvider(AIProvider provider, List<AIModel> models) {
        bootstrapOnce();
        modelsByProvider.put(provider, new ArrayList<>(models));
        try {
            List<SimpleModel> simple = new ArrayList<>();
            for (AIModel m : models) simple.add(new SimpleModel(m.getModelId(), m.getDisplayName(), m.getProvider().name()));
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(simple.toArray(new SimpleModel[0]));
            ModelStorage.putFetchedModelsJson(provider.name(), json);
        } catch (Exception ignored) {}
    }

    public static void addCustom(AIModel model) {
        bootstrapOnce();
        upsertModel(model);
        // Persist entire custom list as JSON array of AIModel
        com.google.gson.Gson gson = new com.google.gson.Gson();
        // Rebuild custom list by selecting models not in any fetched set (best-effort)
        // For now, store just the single model to keep behavior compatible with previous storage usage
        // but since previous code persisted full list, we reconstruct from storage if needed elsewhere.
        // Here we read existing stored list and append.
        String existing = ModelStorage.getCustomModelsJson();
        List<AIModel> all = new ArrayList<>();
        if (existing != null) {
            try {
                AIModel[] arr = gson.fromJson(existing, AIModel[].class);
                if (arr != null) Collections.addAll(all, arr);
            } catch (Exception ignored) {}
        }
        all.add(model);
        ModelStorage.putCustomModelsJson(gson.toJson(all));
    }

    public static void removeByDisplayName(String displayName) {
        bootstrapOnce();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String deletedJson = ModelStorage.getDeletedModelsJson();
        java.util.Set<String> deleted = new java.util.HashSet<>();
        if (deletedJson != null) {
            try { String[] arr = gson.fromJson(deletedJson, String[].class); if (arr != null) deleted.addAll(java.util.Arrays.asList(arr)); } catch (Exception ignored) {}
        }
        deleted.add(displayName);
        ModelStorage.putDeletedModelsJson(gson.toJson(deleted.toArray(new String[0])));
        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
            e.getValue().removeIf(m -> m.getDisplayName().equals(displayName));
        }
    }

    public static void overrideModel(String oldDisplayName, String newDisplayName, String newModelId, AIProvider provider) {
        bootstrapOnce();
        AIModel existing = byName(oldDisplayName);
        ModelCapabilities caps = existing != null ? existing.getCapabilities() : new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
        AIModel updated = new AIModel(newModelId, newDisplayName, provider, caps);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String overridesJson = ModelStorage.getOverridesJson();
        java.util.List<SimpleModel> overrides = new java.util.ArrayList<>();
        if (overridesJson != null) {
            try { SimpleModel[] arr = gson.fromJson(overridesJson, SimpleModel[].class); if (arr != null) overrides.addAll(java.util.Arrays.asList(arr)); } catch (Exception ignored) {}
        }
        overrides.removeIf(sm -> sm.displayName.equals(oldDisplayName));
        overrides.add(new SimpleModel(updated.getModelId(), updated.getDisplayName(), updated.getProvider().name()));
        ModelStorage.putOverridesJson(gson.toJson(overrides));
        upsertModel(updated);
    }

    private static void upsertModel(AIModel model) {
        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
            e.getValue().removeIf(m -> m.getDisplayName().equals(model.getDisplayName()));
        }
        modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
    }

    private static class SimpleModel {
        String modelId; String displayName; String provider;
        SimpleModel() {}
        SimpleModel(String id, String name, String provider) { this.modelId = id; this.displayName = name; this.provider = provider; }
    }
}
