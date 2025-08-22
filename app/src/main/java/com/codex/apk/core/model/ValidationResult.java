package com.codex.apk.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result class for collecting errors and warnings during validation.
 */
public class ValidationResult {
    private final List<String> errors;
    private final List<String> warnings;
    private final boolean valid;
    
    private ValidationResult(Builder builder) {
        this.errors = new ArrayList<>(builder.errors);
        this.warnings = new ArrayList<>(builder.warnings);
        this.valid = this.errors.isEmpty();
    }
    
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public List<String> getWarnings() { return new ArrayList<>(warnings); }
    public boolean isValid() { return valid; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    
    public static class Builder {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public Builder addError(String error) { errors.add(error); return this; }
        public Builder addWarning(String warning) { warnings.add(warning); return this; }
        public Builder merge(ValidationResult other) {
            if (other != null) {
                errors.addAll(other.errors);
                warnings.addAll(other.warnings);
            }
            return this;
        }
        public ValidationResult build() { return new ValidationResult(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}