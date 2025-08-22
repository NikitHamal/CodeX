package com.codex.apk.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request parameters that control AI response generation behavior.
 * These parameters are normalized across providers, with provider-specific
 * implementations mapping to their native parameter formats.
 */
public class RequestParameters {
    
    private final Double temperature;
    private final Integer maxTokens;
    private final Double topP;
    private final Integer topK;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final List<String> stopSequences;
    private final boolean stream;
    private final Integer seed;
    private final String responseFormat;
    
    private RequestParameters(Builder builder) {
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.topP = builder.topP;
        this.topK = builder.topK;
        this.presencePenalty = builder.presencePenalty;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.stopSequences = new ArrayList<>(builder.stopSequences);
        this.stream = builder.stream;
        this.seed = builder.seed;
        this.responseFormat = builder.responseFormat;
    }
    
    // Default constructor with sensible defaults
    public RequestParameters() {
        this.temperature = null;  // Let provider decide
        this.maxTokens = null;    // Let provider decide
        this.topP = null;
        this.topK = null;
        this.presencePenalty = null;
        this.frequencyPenalty = null;
        this.stopSequences = new ArrayList<>();
        this.stream = false;
        this.seed = null;
        this.responseFormat = null;
    }
    
    // Getters
    public Double getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public Double getTopP() { return topP; }
    public Integer getTopK() { return topK; }
    public Double getPresencePenalty() { return presencePenalty; }
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public List<String> getStopSequences() { return new ArrayList<>(stopSequences); }
    public boolean isStream() { return stream; }
    public Integer getSeed() { return seed; }
    public String getResponseFormat() { return responseFormat; }
    
    // Convenience methods
    public boolean hasTemperature() { return temperature != null; }
    public boolean hasMaxTokens() { return maxTokens != null; }
    public boolean hasTopP() { return topP != null; }
    public boolean hasTopK() { return topK != null; }
    public boolean hasPresencePenalty() { return presencePenalty != null; }
    public boolean hasFrequencyPenalty() { return frequencyPenalty != null; }
    public boolean hasStopSequences() { return !stopSequences.isEmpty(); }
    public boolean hasSeed() { return seed != null; }
    public boolean hasResponseFormat() { return responseFormat != null; }
    
    /**
     * Merges this parameters object with another, with the other taking precedence
     * for non-null values.
     * 
     * @param other Parameters to merge with
     * @return New merged parameters object
     */
    public RequestParameters merge(RequestParameters other) {
        if (other == null) return this;
        
        return builder()
            .withTemperature(other.hasTemperature() ? other.temperature : this.temperature)
            .withMaxTokens(other.hasMaxTokens() ? other.maxTokens : this.maxTokens)
            .withTopP(other.hasTopP() ? other.topP : this.topP)
            .withTopK(other.hasTopK() ? other.topK : this.topK)
            .withPresencePenalty(other.hasPresencePenalty() ? other.presencePenalty : this.presencePenalty)
            .withFrequencyPenalty(other.hasFrequencyPenalty() ? other.frequencyPenalty : this.frequencyPenalty)
            .withStopSequences(other.hasStopSequences() ? other.stopSequences : this.stopSequences)
            .withStream(other.stream) // boolean, so take other's value
            .withSeed(other.hasSeed() ? other.seed : this.seed)
            .withResponseFormat(other.hasResponseFormat() ? other.responseFormat : this.responseFormat)
            .build();
    }
    
    /**
     * Validates the parameters for consistency and valid ranges.
     * 
     * @return ValidationResult indicating any issues found
     */
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (temperature != null) {
            if (temperature < 0.0 || temperature > 2.0) {
                result.addError("Temperature must be between 0.0 and 2.0");
            }
        }
        
        if (maxTokens != null) {
            if (maxTokens <= 0) {
                result.addError("Max tokens must be positive");
            }
            if (maxTokens > 1000000) {
                result.addWarning("Max tokens is very high: " + maxTokens);
            }
        }
        
        if (topP != null) {
            if (topP < 0.0 || topP > 1.0) {
                result.addError("Top P must be between 0.0 and 1.0");
            }
        }
        
        if (topK != null) {
            if (topK <= 0) {
                result.addError("Top K must be positive");
            }
        }
        
        if (presencePenalty != null) {
            if (presencePenalty < -2.0 || presencePenalty > 2.0) {
                result.addError("Presence penalty must be between -2.0 and 2.0");
            }
        }
        
        if (frequencyPenalty != null) {
            if (frequencyPenalty < -2.0 || frequencyPenalty > 2.0) {
                result.addError("Frequency penalty must be between -2.0 and 2.0");
            }
        }
        
        return result.build();
    }
    
    /**
     * Builder pattern for constructing RequestParameters instances.
     */
    public static class Builder {
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Integer topK;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private List<String> stopSequences = new ArrayList<>();
        private boolean stream = false;
        private Integer seed;
        private String responseFormat;
        
        public Builder withTemperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder withMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder withTopP(Double topP) {
            this.topP = topP;
            return this;
        }
        
        public Builder withTopK(Integer topK) {
            this.topK = topK;
            return this;
        }
        
        public Builder withPresencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }
        
        public Builder withFrequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }
        
        public Builder withStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences != null ? new ArrayList<>(stopSequences) : new ArrayList<>();
            return this;
        }
        
        public Builder addStopSequence(String stopSequence) {
            this.stopSequences.add(stopSequence);
            return this;
        }
        
        public Builder withStream(boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public Builder withSeed(Integer seed) {
            this.seed = seed;
            return this;
        }
        
        public Builder withResponseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }
        
        public RequestParameters build() {
            return new RequestParameters(this);
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
     * Creates parameters optimized for creative text generation.
     * 
     * @return Creative parameters
     */
    public static RequestParameters creative() {
        return builder()
            .withTemperature(0.9)
            .withTopP(0.9)
            .build();
    }
    
    /**
     * Creates parameters optimized for factual/deterministic responses.
     * 
     * @return Deterministic parameters
     */
    public static RequestParameters deterministic() {
        return builder()
            .withTemperature(0.1)
            .withTopP(0.1)
            .build();
    }
    
    /**
     * Creates parameters optimized for code generation.
     * 
     * @return Code generation parameters
     */
    public static RequestParameters codeGeneration() {
        return builder()
            .withTemperature(0.2)
            .withTopP(0.95)
            .build();
    }
}