package com.codex.apk.core.config;

import com.codex.apk.core.model.ValidationResult;

import java.time.Duration;

/**
 * Retry policy configuration.
 */
public class RetryPolicy {
    private final int maxRetries;
    private final Duration backoffDelay;
    private final double backoffMultiplier;
    private final Duration maxBackoffDelay;
    
    public RetryPolicy(int maxRetries, Duration backoffDelay, double backoffMultiplier, Duration maxBackoffDelay) {
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.backoffMultiplier = backoffMultiplier;
        this.maxBackoffDelay = maxBackoffDelay;
    }
    
    public int getMaxRetries() { return maxRetries; }
    public Duration getBackoffDelay() { return backoffDelay; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public Duration getMaxBackoffDelay() { return maxBackoffDelay; }
    
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (maxRetries < 0) {
            result.addError("Max retries cannot be negative");
        }
        if (maxRetries > 10) {
            result.addWarning("Max retries is very high: " + maxRetries);
        }
        if (backoffDelay.isNegative()) {
            result.addError("Backoff delay cannot be negative");
        }
        if (backoffMultiplier <= 0) {
            result.addError("Backoff multiplier must be positive");
        }
        
        return result.build();
    }
    
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30));
    }
    
    public static RetryPolicy none() {
        return new RetryPolicy(0, Duration.ZERO, 1.0, Duration.ZERO);
    }
}