package com.codex.apk.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal AI response model that encapsulates responses from different AI providers
 * in a consistent format. This model normalizes provider-specific responses into
 * a unified structure for consumption by the application.
 */
public class AIResponse {
    
    private final String id;
    private final String requestId;
    private final String content;
    private final String model;
    private final TokenUsage usage;
    private final ResponseMetadata metadata;
    private final FinishReason finishReason;
    private final List<ToolCall> toolCalls;
    private final List<Citation> citations;
    private final ThinkingContent thinking;
    private final Instant timestamp;
    private final boolean isStreaming;
    private final boolean isComplete;
    private final AIError error;
    
    private AIResponse(Builder builder) {
        this.id = builder.id;
        this.requestId = builder.requestId;
        this.content = builder.content;
        this.model = builder.model;
        this.usage = builder.usage;
        this.metadata = builder.metadata;
        this.finishReason = builder.finishReason;
        this.toolCalls = new ArrayList<>(builder.toolCalls);
        this.citations = new ArrayList<>(builder.citations);
        this.thinking = builder.thinking;
        this.timestamp = Instant.now();
        this.isStreaming = builder.isStreaming;
        this.isComplete = builder.isComplete;
        this.error = builder.error;
    }
    
    // Getters
    public String getId() { return id; }
    public String getRequestId() { return requestId; }
    public String getContent() { return content != null ? content : ""; }
    public String getModel() { return model; }
    public TokenUsage getUsage() { return usage; }
    public ResponseMetadata getMetadata() { return metadata; }
    public FinishReason getFinishReason() { return finishReason; }
    public List<ToolCall> getToolCalls() { return new ArrayList<>(toolCalls); }
    public List<Citation> getCitations() { return new ArrayList<>(citations); }
    public ThinkingContent getThinking() { return thinking; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isStreaming() { return isStreaming; }
    public boolean isComplete() { return isComplete; }
    public AIError getError() { return error; }
    
    // Convenience methods
    public boolean hasError() {
        return error != null;
    }
    
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
    
    public boolean hasCitations() {
        return !citations.isEmpty();
    }
    
    public boolean hasThinking() {
        return thinking != null && thinking.hasContent();
    }
    
    public boolean isSuccessful() {
        return error == null;
    }
    
    public boolean needsFollowUp() {
        return hasToolCalls() || finishReason == FinishReason.TOOL_CALLS;
    }
    
    /**
     * Merges this response with another response, typically used for streaming
     * where multiple partial responses need to be combined.
     * 
     * @param other The response to merge with
     * @return New merged response
     */
    public AIResponse merge(AIResponse other) {
        if (other == null) return this;
        
        return builder()
            .withId(this.id)
            .withRequestId(this.requestId)
            .withContent(this.content + (other.content != null ? other.content : ""))
            .withModel(this.model)
            .withUsage(other.usage != null ? other.usage : this.usage)
            .withMetadata(other.metadata != null ? other.metadata : this.metadata)
            .withFinishReason(other.finishReason != null ? other.finishReason : this.finishReason)
            .withToolCalls(other.hasToolCalls() ? other.toolCalls : this.toolCalls)
            .withCitations(other.hasCitations() ? other.citations : this.citations)
            .withThinking(other.thinking != null ? other.thinking : this.thinking)
            .isStreaming(other.isStreaming)
            .isComplete(other.isComplete)
            .withError(other.error != null ? other.error : this.error)
            .build();
    }
    
    /**
     * Builder pattern for constructing AIResponse instances.
     */
    public static class Builder {
        private String id;
        private String requestId;
        private String content;
        private String model;
        private TokenUsage usage;
        private ResponseMetadata metadata;
        private FinishReason finishReason;
        private List<ToolCall> toolCalls = new ArrayList<>();
        private List<Citation> citations = new ArrayList<>();
        private ThinkingContent thinking;
        private boolean isStreaming = false;
        private boolean isComplete = true;
        private AIError error;
        
        public Builder withId(String id) {
            this.id = id;
            return this;
        }
        
        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder withContent(String content) {
            this.content = content;
            return this;
        }
        
        public Builder withModel(String model) {
            this.model = model;
            return this;
        }
        
        public Builder withUsage(TokenUsage usage) {
            this.usage = usage;
            return this;
        }
        
        public Builder withMetadata(ResponseMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder withFinishReason(FinishReason finishReason) {
            this.finishReason = finishReason;
            return this;
        }
        
        public Builder withToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls != null ? new ArrayList<>(toolCalls) : new ArrayList<>();
            return this;
        }
        
        public Builder addToolCall(ToolCall toolCall) {
            this.toolCalls.add(toolCall);
            return this;
        }
        
        public Builder withCitations(List<Citation> citations) {
            this.citations = citations != null ? new ArrayList<>(citations) : new ArrayList<>();
            return this;
        }
        
        public Builder addCitation(Citation citation) {
            this.citations.add(citation);
            return this;
        }
        
        public Builder withThinking(ThinkingContent thinking) {
            this.thinking = thinking;
            return this;
        }
        
        public Builder isStreaming(boolean streaming) {
            this.isStreaming = streaming;
            return this;
        }
        
        public Builder isComplete(boolean complete) {
            this.isComplete = complete;
            return this;
        }
        
        public Builder withError(AIError error) {
            this.error = error;
            return this;
        }
        
        public AIResponse build() {
            return new AIResponse(this);
        }
    }
    
    /**
     * Creates a new builder instance.
     * 
     * @return New builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates an error response.
     * 
     * @param requestId The original request ID
     * @param error The error that occurred
     * @return Error response
     */
    public static AIResponse error(String requestId, AIError error) {
        return builder()
            .withRequestId(requestId)
            .withError(error)
            .isComplete(true)
            .build();
    }
    
    /**
     * Enumeration of possible finish reasons for responses.
     */
    public enum FinishReason {
        STOP,           // Natural completion
        LENGTH,         // Hit token limit
        TOOL_CALLS,     // Needs tool execution
        CONTENT_FILTER, // Content was filtered
        ERROR,          // Error occurred
        CANCELLED,      // Request was cancelled
        TIMEOUT         // Request timed out
    }
}