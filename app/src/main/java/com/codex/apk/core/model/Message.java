package com.codex.apk.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal message representation that works across different AI providers.
 * Messages form the core of AI conversations and can contain text, attachments,
 * and tool call information.
 */
public class Message {
    
    private final MessageRole role;
    private final String content;
    private final List<Attachment> attachments;
    private final List<ToolCall> toolCalls;
    private final Instant timestamp;
    private final String name; // Optional name for user messages or assistant responses
    
    private Message(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.attachments = new ArrayList<>(builder.attachments);
        this.toolCalls = new ArrayList<>(builder.toolCalls);
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.name = builder.name;
    }
    
    // Getters
    public MessageRole getRole() { return role; }
    public String getContent() { return content != null ? content : ""; }
    public List<Attachment> getAttachments() { return new ArrayList<>(attachments); }
    public List<ToolCall> getToolCalls() { return new ArrayList<>(toolCalls); }
    public Instant getTimestamp() { return timestamp; }
    public String getName() { return name; }
    
    // Convenience methods
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }
    
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
    
    public boolean isEmpty() {
        return !hasContent() && !hasAttachments() && !hasToolCalls();
    }
    
    /**
     * Estimates the token count for this message.
     * This is a rough approximation and may vary by provider.
     * 
     * @return Estimated token count
     */
    public int getEstimatedTokenCount() {
        int tokens = 0;
        
        // Rough approximation: 1 token per 4 characters for English text
        if (content != null) {
            tokens += content.length() / 4;
        }
        
        // Add overhead for role and structure
        tokens += 10;
        
        // Add tokens for attachments (rough estimate)
        tokens += attachments.size() * 100;
        
        // Add tokens for tool calls
        for (ToolCall toolCall : toolCalls) {
            tokens += toolCall.getEstimatedTokenCount();
        }
        
        return Math.max(tokens, 1);
    }
    
    /**
     * Serializes the message to a string representation.
     * 
     * @return String representation of the message
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(role.name()).append(": ");
        
        if (hasContent()) {
            sb.append(content);
        }
        
        if (hasAttachments()) {
            sb.append(" [").append(attachments.size()).append(" attachments]");
        }
        
        if (hasToolCalls()) {
            sb.append(" [").append(toolCalls.size()).append(" tool calls]");
        }
        
        return sb.toString();
    }
    
    /**
     * Builder pattern for constructing Message instances.
     */
    public static class Builder {
        private MessageRole role;
        private String content;
        private List<Attachment> attachments = new ArrayList<>();
        private List<ToolCall> toolCalls = new ArrayList<>();
        private Instant timestamp;
        private String name;
        
        public Builder withRole(MessageRole role) {
            this.role = role;
            return this;
        }
        
        public Builder withContent(String content) {
            this.content = content;
            return this;
        }
        
        public Builder withAttachments(List<Attachment> attachments) {
            this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
            return this;
        }
        
        public Builder addAttachment(Attachment attachment) {
            this.attachments.add(attachment);
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
        
        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Message build() {
            if (role == null) {
                throw new IllegalArgumentException("Message role is required");
            }
            return new Message(this);
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
     * Creates a user message with the given content.
     * 
     * @param content Message content
     * @return User message
     */
    public static Message user(String content) {
        return builder()
            .withRole(MessageRole.USER)
            .withContent(content)
            .build();
    }
    
    /**
     * Creates an assistant message with the given content.
     * 
     * @param content Message content
     * @return Assistant message
     */
    public static Message assistant(String content) {
        return builder()
            .withRole(MessageRole.ASSISTANT)
            .withContent(content)
            .build();
    }
    
    /**
     * Creates a system message with the given content.
     * 
     * @param content Message content
     * @return System message
     */
    public static Message system(String content) {
        return builder()
            .withRole(MessageRole.SYSTEM)
            .withContent(content)
            .build();
    }
    
    /**
     * Creates a tool response message.
     * 
     * @param toolCallId Tool call ID this is responding to
     * @param content Tool response content
     * @return Tool message
     */
    public static Message tool(String toolCallId, String content) {
        return builder()
            .withRole(MessageRole.TOOL)
            .withContent(content)
            .withName(toolCallId)
            .build();
    }
    
    /**
     * Enumeration of message roles in AI conversations.
     */
    public enum MessageRole {
        SYSTEM,     // System/instruction messages
        USER,       // User messages
        ASSISTANT,  // AI assistant responses
        TOOL        // Tool execution results
    }
}