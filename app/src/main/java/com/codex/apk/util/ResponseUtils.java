package com.codex.apk.util;

public final class ResponseUtils {
    private ResponseUtils() {}

    // Build final explanation including thinking content if available
    public static String buildExplanationWithThinking(String baseExplanation, String thinking) {
        if (thinking == null || thinking.trim().isEmpty()) return baseExplanation != null ? baseExplanation : "";
        StringBuilder sb = new StringBuilder();
        if (baseExplanation != null && !baseExplanation.trim().isEmpty()) {
            sb.append(baseExplanation.trim()).append("\n\n");
        }
        sb.append("[Thinking]\n").append(thinking.trim());
        return sb.toString();
    }
}
