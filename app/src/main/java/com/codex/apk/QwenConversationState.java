package com.codex.apk;

import com.google.gson.Gson;

public class QwenConversationState {
    private String conversationId;
    private String lastParentId;

    public QwenConversationState() {
        this.conversationId = null;
        this.lastParentId = null;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getLastParentId() {
        return lastParentId;
    }

    public void setLastParentId(String lastParentId) {
        this.lastParentId = lastParentId;
    }

    public void clear() {
        this.conversationId = null;
        this.lastParentId = null;
    }

    // Serialize to JSON string
    public String toJson() {
        return new Gson().toJson(this);
    }

    // Deserialize from JSON string
    public static QwenConversationState fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new QwenConversationState();
        }
        return new Gson().fromJson(json, QwenConversationState.class);
    }
}
