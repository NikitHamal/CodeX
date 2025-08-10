package com.codex.apk;

import android.util.Log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import com.codex.apk.lint.LintIssue;
import com.codex.apk.lint.HtmlLinter;
import com.codex.apk.lint.CssLinter;
import com.codex.apk.lint.JsLinter;

/**
 * Lightweight project verifier for HTML/CSS/JS sanity checks after AI edits.
 * Not a full linter; aims to catch obvious issues and feed them back to the agent.
 */
public class ProjectVerifier {
    private static final String TAG = "ProjectVerifier";

    public static class VerificationResult {
        public final boolean ok;
        public final List<String> issues;
        public final List<LintIssue> lintIssues;
        public VerificationResult(boolean ok, List<String> issues, List<LintIssue> lintIssues) {
            this.ok = ok;
            this.issues = issues;
            this.lintIssues = lintIssues;
        }
    }

    public VerificationResult verify(List<ChatMessage.FileActionDetail> appliedActions, File projectDir) {
        List<String> issues = new ArrayList<>();
        List<LintIssue> lintIssues = new ArrayList<>();
        HtmlLinter html = new HtmlLinter();
        CssLinter css = new CssLinter();
        JsLinter js = new JsLinter();
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
                if (path.toLowerCase().endsWith(".html") || path.toLowerCase().endsWith(".htm")) {
                    lintIssues.addAll(html.lint(path, content));
                } else if (path.toLowerCase().endsWith(".css")) {
                    lintIssues.addAll(css.lint(path, content));
                } else if (path.toLowerCase().endsWith(".js")) {
                    lintIssues.addAll(js.lint(path, content));
                }
            } catch (Exception e) {
                Log.w(TAG, "Verification error for action on path " + d.path + ": " + e.getMessage());
            }
        }

        boolean ok = issues.isEmpty() && lintIssues.stream().noneMatch(i -> i.severity == LintIssue.Severity.ERROR);
        return new VerificationResult(ok, issues, lintIssues);
    }

    private List<String> verifyFile(String path, String content) {
        List<String> out = new ArrayList<>();
        String lower = path.toLowerCase();
        int line = 1, col = 1;
        // Basic structural quick checks with line/col estimation by scanning
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            if (!content.toLowerCase().contains("<html") && !content.toLowerCase().contains("<!doctype")) {
                out.add(path + ":1:1 Missing <html> or <!DOCTYPE>");
            }
        }
        if (lower.endsWith(".js")) {
            // Find first bracket imbalance quick signal (heuristic)
            int bal = 0; for (int i=0;i<content.length();i++){ char c=content.charAt(i); if (c=='\n'){line++; col=1; continue;} if (c=='{') bal++; else if (c=='}') bal--; if (bal<0){ out.add(path+":"+line+":"+col+" Unexpected '}'"); break;} col++; }
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
        if (content.contains("console.log(\"\")") ) { // silly but catches an easy typo
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


