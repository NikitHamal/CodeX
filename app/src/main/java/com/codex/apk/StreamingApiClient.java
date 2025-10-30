package com.codex.apk;

import com.codex.apk.ai.AIModel;
import java.io.File;
import java.util.List;
import okhttp3.ConnectionPool;

public interface StreamingApiClient extends ApiClient {

    void sendMessageStreaming(MessageRequest request, StreamListener listener);
    void cancelStreaming(String requestId);
    void setConnectionPool(ConnectionPool pool);

    class MessageRequest {
        private final String requestId;
        private final String message;
        private final List<ChatMessage> history;
        private final AIModel model;
        private final Object conversationState;
        private final boolean thinkingModeEnabled;
        private final boolean webSearchEnabled;
        private final List<ToolSpec> enabledTools;
        private final List<File> attachments;

        public MessageRequest(Builder builder) {
            this.requestId = java.util.UUID.randomUUID().toString();
            this.message = builder.message;
            this.history = builder.history;
            this.model = builder.model;
            this.conversationState = builder.conversationState;
            this.thinkingModeEnabled = builder.thinkingModeEnabled;
            this.webSearchEnabled = builder.webSearchEnabled;
            this.enabledTools = builder.enabledTools;
            this.attachments = builder.attachments;
        }

        public String getRequestId() { return requestId; }
        public String getMessage() { return message; }
        public List<ChatMessage> getHistory() { return history; }
        public AIModel getModel() { return model; }
        public Object getConversationState() { return conversationState; }
        public boolean isThinkingModeEnabled() { return thinkingModeEnabled; }
        public boolean isWebSearchEnabled() { return webSearchEnabled; }
        public List<ToolSpec> getEnabledTools() { return enabledTools; }
        public List<File> getAttachments() { return attachments; }

        public static class Builder {
            private String message;
            private List<ChatMessage> history;
            private AIModel model;
            private Object conversationState;
            private boolean thinkingModeEnabled;
            private boolean webSearchEnabled;
            private List<ToolSpec> enabledTools;
            private List<File> attachments;

            public Builder message(String message) { this.message = message; return this; }
            public Builder history(List<ChatMessage> history) { this.history = history; return this; }
            public Builder model(AIModel model) { this.model = model; return this; }
            public Builder conversationState(Object state) { this.conversationState = state; return this; }
            public Builder thinkingModeEnabled(boolean enabled) { this.thinkingModeEnabled = enabled; return this; }
            public Builder webSearchEnabled(boolean enabled) { this.webSearchEnabled = enabled; return this; }
            public Builder enabledTools(List<ToolSpec> tools) { this.enabledTools = tools; return this; }
            public Builder attachments(List<File> attachments) { this.attachments = attachments; return this; }
            public MessageRequest build() { return new MessageRequest(this); }
        }
    }

    interface StreamListener {
        void onStreamStarted(String requestId);
        void onStreamPartialUpdate(String requestId, String partialResponse, boolean isThinking);
        void onStreamCompleted(String requestId, QwenResponseParser.ParsedResponse response);
        void onStreamError(String requestId, String errorMessage, Throwable throwable);
    }

    enum RetrySuggestion {
        NONE,
        RETRY,
        SWITCH_PROVIDER
    }
}
