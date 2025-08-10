package com.codex.apk;

import android.util.Log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight project verifier for HTML/CSS/JS sanity checks after AI edits.
 * Not a full linter; aims to catch obvious issues and feed them back to the agent.
 */
public class ProjectVerifier {
    private static final String TAG = "ProjectVerifier";

    public static class VerificationResult {
        public final boolean ok;
        public final List<String> issues;
        public VerificationResult(boolean ok, List<String> issues) {
            this.ok = ok;
            this.issues = issues;
        }
    }

    public VerificationResult verify(List<ChatMessage.FileActionDetail> appliedActions, File projectDir) {
        List<String> issues = new ArrayList<>();
        if (appliedActions == null || appliedActions.isEmpty()) {
            return new VerificationResult(true, issues);
        }

        for (ChatMessage.FileActionDetail d : appliedActions) {
            try {
                String path = d.type != null && d.type.equals("renameFile") ? d.newPath : d.path;
                if (path == null || path.trim().isEmpty()) continue;
                File f = new File(projectDir, path);
                if (!f.exists() || f.isDirectory()) continue;
                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                issues.addAll(verifyFile(path, content));
            } catch (Exception e) {
                Log.w(TAG, "Verification error for action on path " + d.path + ": " + e.getMessage());
            }
        }

        boolean ok = issues.isEmpty();
        return new VerificationResult(ok, issues);
    }

    private List<String> verifyFile(String path, String content) {
        List<String> out = new ArrayList<>();
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            verifyHtml(path, content, out);
        } else if (lower.endsWith(".css")) {
            verifyCss(path, content, out);
        } else if (lower.endsWith(".js")) {
            verifyJs(path, content, out);
        }
        return out;
    }

    private void verifyHtml(String path, String content, List<String> out) {
        if (!content.contains("<html") && !content.contains("<!DOCTYPE")) {
            out.add(path + ": Missing <html> or <!DOCTYPE> tag");
        }
        int open = count(content, '<');
        int close = count(content, '>');
        if (close < open) {
            out.add(path + ": Unbalanced angle brackets; possible unclosed tag(s)");
        }
        if (content.toLowerCase().contains("<script src=\"https://cdn.tailwindcss.com\"") == false &&
            content.toLowerCase().contains("class=\"") && content.toLowerCase().contains("grid") ) {
            // Heuristic: grid classes but no tailwind include
            out.add(path + ": Tailwind utility classes detected but Tailwind CDN script not found");
        }
    }

    private void verifyCss(String path, String content, List<String> out) {
        int open = count(content, '{');
        int close = count(content, '}');
        if (open != close) out.add(path + ": Unbalanced braces in CSS");
    }

    private void verifyJs(String path, String content, List<String> out) {
        int paren = balance(content, '(', ')');
        int brace = balance(content, '{', '}');
        int bracket = balance(content, '[', ']');
        if (paren != 0) out.add(path + ": Unbalanced parentheses in JS");
        if (brace != 0) out.add(path + ": Unbalanced braces in JS");
        if (bracket != 0) out.add(path + ": Unbalanced brackets in JS");
        if (content.contains("console.log("")") ) { // silly but catches an easy typo
            out.add(path + ": Suspicious console.log syntax");
        }
    }

    private int count(String s, char ch) {
        int c = 0; for (int i = 0; i < s.length(); i++) if (s.charAt(i) == ch) c++; return c;
    }
    private int balance(String s, char open, char close) {
        int bal = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) bal++;
            else if (c == close) bal--;
        }
        return bal;
    }
}


