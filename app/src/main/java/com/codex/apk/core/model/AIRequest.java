package com.codex.apk.core.model;

import com.codex.apk.ToolSpec;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Universal AI request model that encapsulates all information needed to make
 * an AI service call across different providers. This model is provider-agnostic
 * and gets transformed into provider-specific formats by individual services.
 */
public class AIRequest {
    
    private final String id;
    private final List<Message> messages;
    private final String model;
    private final RequestParameters parameters;
    private final RequiredCapabilities requiredCapabilities;
    private final List<Attachment> attachments;
    private final List<ToolSpec> tools;
    private final Map<String, Object> metadata;
    private final ExecutionContext context;
    private final Instant timestamp;
    
    private AIRequest(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.messages = new ArrayList<>(builder.messages);
        this.model = builder.model;
        this.parameters = builder.parameters != null ? builder.parameters : new RequestParameters();
        this.requiredCapabilities = builder.requiredCapabilities != null ? builder.requiredCapabilities : new RequiredCapabilities();
        this.attachments = new ArrayList<>(builder.attachments);
        this.tools = new ArrayList<>(builder.tools);
        this.metadata = new HashMap<>(builder.metadata);
        this.context = builder.context;
        this.timestamp = Instant.now();
    }
    
    // Getters
    public String getId() { return id; }
    public List<Message> getMessages() { return new ArrayList<>(messages); }
    public String getModel() { return model; }
    public RequestParameters getParameters() { return parameters; }
    public RequiredCapabilities getRequiredCapabilities() { return requiredCapabilities; }
    public List<Attachment> getAttachments() { return new ArrayList<>(attachments); }
    public List<ToolSpec> getTools() { return new ArrayList<>(tools); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public ExecutionContext getContext() { return context; }
    public Instant getTimestamp() { return timestamp; }
    
    // Convenience methods
    public boolean isStreaming() {
        return parameters.isStream();
    }
    
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }
    
    public boolean hasTools() {
        return !tools.isEmpty();
    }
    
    public boolean requiresCapability(String capability) {
        return requiredCapabilities.requires(capability);
    }
    
    /**
     * Validates the request for basic consistency and completeness.
     * 
     * @return ValidationResult indicating any issues found
     */
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (messages == null || messages.isEmpty()) {
            result.addError("Request must contain at least one message");
        }
        
        if (model == null || model.trim().isEmpty()) {
            result.addError("Model must be specified");
        }
        
        // Validate messages
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
                if (msg.getAttachments().isEmpty()) {
                    result.addWarning("Message " + i + " has no content or attachments");
                }
            }
        }
        
        // Validate attachments if present
        for (Attachment attachment : attachments) {
            if (attachment.getType() == null) {
                result.addError("Attachment missing type");
            }
        }
        
        // Validate parameters
        ValidationResult paramValidation = parameters.validate();
        result.merge(paramValidation);
        
        return result.build();
    }
    
    /**
     * Creates a copy of this request with provider-specific optimizations applied.
     * 
     * @param providerType The target provider type
     * @return Optimized request copy
     */
    public AIRequest optimizeForProvider(com.codex.apk.ai.AIProvider providerType) {
        // This could be extended to apply provider-specific optimizations
        return new Builder(this).build();
    }
    
    /**
     * Builder pattern for constructing AIRequest instances.
     */
    public static class Builder {
        private String id;
        private List<Message> messages = new ArrayList<>();
        private String model;
        private RequestParameters parameters;
        private RequiredCapabilities requiredCapabilities;
        private List<Attachment> attachments = new ArrayList<>();
        private List<ToolSpec> tools = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        private ExecutionContext context;
        
        public Builder() {}
        
        public Builder(AIRequest request) {
            this.id = request.id;
            this.messages = new ArrayList<>(request.messages);
            this.model = request.model;
            this.parameters = request.parameters;
            this.requiredCapabilities = request.requiredCapabilities;
            this.attachments = new ArrayList<>(request.attachments);
            this.tools = new ArrayList<>(request.tools);
            this.metadata = new HashMap<>(request.metadata);
            this.context = request.context;
        }
        
        public Builder withId(String id) {
            this.id = id;
            return this;
        }
        
        public Builder withMessages(List<Message> messages) {
            this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
            return this;
        }
        
        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }
        
        public Builder withModel(String model) {
            this.model = model;
            return this;
        }
        
        public Builder withParameters(RequestParameters parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder requireCapabilities(RequiredCapabilities capabilities) {
            this.requiredCapabilities = capabilities;
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
        
        public Builder withTools(List<ToolSpec> tools) {
            this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
            return this;
        }
        
        public Builder addTool(ToolSpec tool) {
            this.tools.add(tool);
            return this;
        }
        
        public Builder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder withContext(ExecutionContext context) {
            this.context = context;
            return this;
        }
        
        public AIRequest build() throws ValidationException {
            AIRequest request = new AIRequest(this);
            ValidationResult validation = request.validate();
            if (validation.hasErrors()) {
                throw new ValidationException("Request validation failed: " + validation.getErrors());
            }
            return request;
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
     * Exception thrown when request validation fails.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}