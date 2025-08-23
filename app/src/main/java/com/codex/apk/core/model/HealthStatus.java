package com.codex.apk.core.model;

/**
 * Health status for providers.
 */
public class HealthStatus {
    private final boolean healthy;
    private final String message;
    private final long responseTimeMs;

    public HealthStatus(boolean healthy, String message, long responseTimeMs) {
        this.healthy = healthy;
        this.message = message;
        this.responseTimeMs = responseTimeMs;
    }

    public boolean isHealthy() { return healthy; }
    public String getMessage() { return message; }
    public long getResponseTimeMs() { return responseTimeMs; }

    public static HealthStatus healthy() { return new HealthStatus(true, "OK", 0); }
    public static HealthStatus unhealthy(String message) { return new HealthStatus(false, message, -1); }
}
