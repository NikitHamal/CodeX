package com.codex.apk.core.config;

import com.codex.apk.core.model.ValidationResult;

import java.time.Duration;

/**
 * Timeout configuration for HTTP requests.
 */
public class TimeoutConfig {
    private final Duration connectionTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;
    private final Duration totalTimeout;
    
    public TimeoutConfig(Duration connectionTimeout, Duration readTimeout, Duration writeTimeout, Duration totalTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.totalTimeout = totalTimeout;
    }
    
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public Duration getWriteTimeout() { return writeTimeout; }
    public Duration getTotalTimeout() { return totalTimeout; }
    
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (connectionTimeout.isNegative()) {
            result.addError("Connection timeout cannot be negative");
        }
        if (readTimeout.isNegative()) {
            result.addError("Read timeout cannot be negative");
        }
        if (writeTimeout.isNegative()) {
            result.addError("Write timeout cannot be negative");
        }
        if (totalTimeout.isNegative()) {
            result.addError("Total timeout cannot be negative");
        }
        
        return result.build();
    }
    
    public static TimeoutConfig defaults() {
        return new TimeoutConfig(
            Duration.ofSeconds(30),  // connection
            Duration.ofSeconds(60),  // read
            Duration.ofSeconds(30),  // write  
            Duration.ofMinutes(5)    // total
        );
    }
}