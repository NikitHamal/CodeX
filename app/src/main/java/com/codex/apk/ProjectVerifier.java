package com.codex.apk;

import java.io.File;
import java.util.List;

/**
 * Deprecated no-op verifier. Post-apply verification has been removed.
 */
@Deprecated
public class ProjectVerifier {

    @Deprecated
    public static class VerificationResult {
        public final boolean ok;
        public VerificationResult(boolean ok) { this.ok = ok; }
    }

    @Deprecated
    public VerificationResult verify(List<ChatMessage.FileActionDetail> appliedActions, File projectDir) {
        return new VerificationResult(true);
    }
}


