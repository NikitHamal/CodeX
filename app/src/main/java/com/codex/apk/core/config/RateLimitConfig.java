package com.codex.apk.core.config;

/**
 * Rate limiting configuration.
 */
public class RateLimitConfig {
    private final int requestsPerSecond;
    private final int burstCapacity;
    private final int queueSize;
    
    public RateLimitConfig(int requestsPerSecond, int burstCapacity, int queueSize) {
        this.requestsPerSecond = requestsPerSecond;
        this.burstCapacity = burstCapacity;
        this.queueSize = queueSize;
    }
    
    public int getRequestsPerSecond() { return requestsPerSecond; }
    public int getBurstCapacity() { return burstCapacity; }
    public int getQueueSize() { return queueSize; }
    
    public static RateLimitConfig defaults() {
        return new RateLimitConfig(10, 20, 100);
    }
    
    public static RateLimitConfig unlimited() {
        return new RateLimitConfig(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    }
}