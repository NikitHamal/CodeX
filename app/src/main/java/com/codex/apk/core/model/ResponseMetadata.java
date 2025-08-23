package com.codex.apk.core.model;

/**
 * Response metadata.
 */
public class ResponseMetadata {
    private final String model;
    private final long processingTimeMs;
    private final String providerId;

    public ResponseMetadata(String model, long processingTimeMs, String providerId) {
        this.model = model;
        this.processingTimeMs = processingTimeMs;
        this.providerId = providerId;
    }

    public String getModel() { return model; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    public String getProviderId() { return providerId; }
}
