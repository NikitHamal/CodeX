package com.codex.apk.util;

public final class FileContentValidator {
    private FileContentValidator() {}

    public static ValidationResult validate(String content, String contentType) {
        ValidationResult result = new ValidationResult();
        if (content == null || content.trim().isEmpty()) {
            result.setValid(false);
            result.setReason("Content is empty");
            return result;
        }

        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "html":
                    if (!content.contains("<html") && !content.contains("<!DOCTYPE")) {
                        result.setValid(false);
                        result.setReason("Invalid HTML content");
                        return result;
                    }
                    break;
                case "css":
                    if (!content.contains("{") || !content.contains("}")) {
                        result.setValid(false);
                        result.setReason("Invalid CSS content");
                        return result;
                    }
                    break;
                case "javascript":
                case "js":
                    if (!content.contains("function") && !content.contains("var") && !content.contains("const")) {
                        result.setValid(false);
                        result.setReason("Invalid JavaScript content");
                        return result;
                    }
                    break;
                default:
                    break;
            }
        }

        result.setValid(true);
        return result;
    }

    public static final class ValidationResult {
        private boolean valid;
        private String reason;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
