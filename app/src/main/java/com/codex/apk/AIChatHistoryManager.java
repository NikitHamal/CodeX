package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;

public class AIChatHistoryManager {

    private static final String TAG = "AIChatHistoryManager";
    private static final String PREFS_NAME = "ai_chat_prefs";
    private static final String CHAT_HISTORY_KEY_PREFIX = "chat_history_";
    private static final String QWEN_CONVERSATION_STATE_KEY_PREFIX = "qwen_conv_state_";
    private static final String OLD_GENERIC_CHAT_HISTORY_KEY = "chat_history";
    private static final String FREE_CONV_META_KEY_PREFIX = "free_conv_meta_";

    private final Context context;
    private final String projectPath;
    private final SharedPreferences prefs;
    private final Gson gson;

    public AIChatHistoryManager(Context context, String projectPath) {
        this.context = context;
        this.projectPath = projectPath;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void loadChatState(List<ChatMessage> chatHistory, QwenConversationState qwenState) {
        Type historyType = new TypeToken<List<ChatMessage>>() {}.getType();

        // Load Chat History
        String historyKey = getProjectSpecificKey(CHAT_HISTORY_KEY_PREFIX);
        String historyJson = prefs.getString(historyKey, null);
        if (historyJson != null) {
            List<ChatMessage> loadedHistory = gson.fromJson(historyJson, historyType);
            if (loadedHistory != null) {
                chatHistory.clear();
                chatHistory.addAll(loadedHistory);
            }
        } else {
            // Migration from old generic key
            String oldGenericJson = prefs.getString(OLD_GENERIC_CHAT_HISTORY_KEY, null);
            if (oldGenericJson != null) {
                List<ChatMessage> loadedHistory = gson.fromJson(oldGenericJson, historyType);
                if (loadedHistory != null && !loadedHistory.isEmpty()) {
                    chatHistory.clear();
                    chatHistory.addAll(loadedHistory);
                }
            }
        }

        // Load Qwen Conversation State
        String qwenStateKey = getProjectSpecificKey(QWEN_CONVERSATION_STATE_KEY_PREFIX);
        String qwenStateJson = prefs.getString(qwenStateKey, null);
        if (qwenStateJson != null) {
            QwenConversationState loadedState = QwenConversationState.fromJson(qwenStateJson);
            qwenState.setConversationId(loadedState.getConversationId());
            qwenState.setLastParentId(loadedState.getLastParentId());
        }

        // Restore Gemini FREE conversation metadata per project if present, by copying to SettingsActivity scoping
        try {
            String meta = prefs.getString(getProjectSpecificKey(FREE_CONV_META_KEY_PREFIX), null);
            if (meta != null && !meta.isEmpty()) {
                // Use default free model id if not known here; SettingsActivity stores per model.
                SettingsActivity.setFreeConversationMetadata(context, "gemini-2.5-flash", meta);
            }
        } catch (Exception ignore) {}
    }

    public void saveChatState(List<ChatMessage> chatHistory, QwenConversationState qwenState) {
        SharedPreferences.Editor editor = prefs.edit();

        // Save Chat History
        String historyKey = getProjectSpecificKey(CHAT_HISTORY_KEY_PREFIX);
        String historyJson = gson.toJson(chatHistory);
        editor.putString(historyKey, historyJson);

        // Save Qwen Conversation State
        String qwenStateKey = getProjectSpecificKey(QWEN_CONVERSATION_STATE_KEY_PREFIX);
        String qwenStateJson = qwenState.toJson();
        editor.putString(qwenStateKey, qwenStateJson);

        // Persist last known FREE conversation metadata for this project
        try {
            String modelId = "gemini-2.5-flash";
            String meta = SettingsActivity.getFreeConversationMetadata(context, modelId);
            if (meta != null && !meta.isEmpty()) {
                editor.putString(getProjectSpecificKey(FREE_CONV_META_KEY_PREFIX), meta);
            }
        } catch (Exception ignore) {}

        editor.apply();
    }

    private String getProjectSpecificKey(String prefix) {
        if (projectPath == null || projectPath.isEmpty()) {
            return prefix + "generic_fallback";
        }
        try {
            byte[] pathBytes = projectPath.getBytes("UTF-8");
            String encodedPath = Base64.encodeToString(pathBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            return prefix + encodedPath;
        } catch (UnsupportedEncodingException e) {
            return prefix + projectPath.replaceAll("[^a-zA-Z0-9_]", "_");
        }
    }

    public static void deleteChatStateForProject(Context context, String projectPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String historyKey;
        String qwenStateKey;
        try {
            byte[] pathBytes = projectPath.getBytes("UTF-8");
            String encodedPath = Base64.encodeToString(pathBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            historyKey = CHAT_HISTORY_KEY_PREFIX + encodedPath;
            qwenStateKey = QWEN_CONVERSATION_STATE_KEY_PREFIX + encodedPath;
        } catch (UnsupportedEncodingException e) {
            historyKey = CHAT_HISTORY_KEY_PREFIX + projectPath.replaceAll("[^a-zA-Z0-9_]", "_");
            qwenStateKey = QWEN_CONVERSATION_STATE_KEY_PREFIX + projectPath.replaceAll("[^a-zA-Z0-9_]", "_");
        }

        editor.remove(historyKey);
        editor.remove(qwenStateKey);
        editor.apply();
    }
}
