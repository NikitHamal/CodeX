package com.codex.apk.lint;

import java.util.ArrayList;
import java.util.List;

public class CssLinter {
    public List<LintIssue> lint(String path, String content) {
        List<LintIssue> issues = new ArrayList<>();
        int open = count(content, '{');
        int close = count(content, '}');
        if (open != close) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.ERROR, "Unbalanced braces in CSS"));
        }
        // Warn on !important usage
        if (content.contains("!important")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.INFO, "Avoid !important where possible"));
        }
        return issues;
    }
    private int count(String s, char ch) { int c=0; for (int i=0;i<s.length();i++) if (s.charAt(i)==ch) c++; return c; }
}


