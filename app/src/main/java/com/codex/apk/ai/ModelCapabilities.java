package com.codex.apk.ai;

public class ModelCapabilities {
    public final boolean supportsThinking;
    public final boolean supportsWebSearch;
    public final boolean supportsVision;
    public final boolean supportsDocument;
    public final boolean supportsVideo;
    public final boolean supportsAudio;
    public final boolean supportsCitations;
    public final int maxContextLength;
    public final int maxGenerationLength;

    public ModelCapabilities(boolean supportsThinking, boolean supportsWebSearch,
                            boolean supportsVision, boolean supportsDocument,
                            boolean supportsVideo, boolean supportsAudio,
                            boolean supportsCitations, int maxContextLength,
                            int maxGenerationLength) {
        this.supportsThinking = supportsThinking;
        this.supportsWebSearch = supportsWebSearch;
        this.supportsVision = supportsVision;
        this.supportsDocument = supportsDocument;
        this.supportsVideo = supportsVideo;
        this.supportsAudio = supportsAudio;
        this.supportsCitations = supportsCitations;
        this.maxContextLength = maxContextLength;
        this.maxGenerationLength = maxGenerationLength;
    }
}
