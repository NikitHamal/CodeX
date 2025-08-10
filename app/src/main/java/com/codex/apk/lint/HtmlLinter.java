package com.codex.apk.lint;

import java.util.ArrayList;
import java.util.List;

public class HtmlLinter {
    public List<LintIssue> lint(String path, String content) {
        List<LintIssue> issues = new ArrayList<>();
        String lower = content.toLowerCase();

        if (!lower.contains("<html") && !lower.contains("<!doctype")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.ERROR, "Missing <html> or <!DOCTYPE>"));
        }
        if (!lower.contains("<head")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.WARNING, "Missing <head> section"));
        }
        if (!lower.contains("<body")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.WARNING, "Missing <body> section"));
        }
        // Simple check for unclosed tags by angle bracket balance
        int lt = count(content, '<');
        int gt = count(content, '>');
        if (gt < lt) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.ERROR, "Unbalanced angle brackets; possible unclosed tag(s)"));
        }

        // Tag stack validation (basic): detect unclosed closing mismatch
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        int line = 1, col = 1;
        for (int i = 0; i < content.length();) {
            char ch = content.charAt(i);
            if (ch == '\n') { line++; col = 1; i++; continue; }
            if (ch == '<') {
                int startCol = col;
                int j = content.indexOf('>', i+1);
                if (j < 0) break;
                String tag = content.substring(i+1, j).trim();
                if (!tag.startsWith("!")) {
                    boolean closing = tag.startsWith("/");
                    boolean selfClose = tag.endsWith("/");
                    String name = tag.replaceAll("^/|/\\s*$", "").split("\\s+")[0].toLowerCase();
                    if (!closing && !selfClose && isContainerTag(name)) {
                        stack.push(name);
                    } else if (closing) {
                        if (!stack.isEmpty() && stack.peek().equals(name)) {
                            stack.pop();
                        } else {
                            issues.add(new LintIssue(path, line, startCol, LintIssue.Severity.ERROR, "Mismatched closing tag: </" + name + ">"));
                        }
                    }
                }
                int consumed = (j - i) + 1;
                col += consumed; i += consumed; continue;
            }
            col++; i++;
        }
        if (!stack.isEmpty()) {
            issues.add(new LintIssue(path, line, col, LintIssue.Severity.ERROR, "Unclosed tag(s): " + String.join(", ", stack)));
        }
        // Accessibility nudges
        if (lower.contains("<img ") && !lower.contains(" alt=")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.WARNING, "Images should include alt attributes"));
        }
        if (lower.contains("<button") && !lower.contains("type=")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.INFO, "Buttons should declare type (button/submit/reset)"));
        }
        return issues;
    }

    private int count(String s, char ch) { int c=0; for (int i=0;i<s.length();i++) if (s.charAt(i)==ch) c++; return c; }

    private boolean isContainerTag(String name) {
        // Common container tags to track; void tags omitted
        switch (name) {
            case "html": case "head": case "body": case "div": case "section": case "header": case "footer":
            case "main": case "nav": case "article": case "aside": case "ul": case "ol": case "li": case "span":
            case "a": case "p": case "button": case "form": case "label": case "input": case "textarea": case "select":
            case "h1": case "h2": case "h3": case "h4": case "h5": case "h6": case "table": case "thead": case "tbody": case "tr": case "td": case "th":
                return true;
            default: return false;
        }
    }
}


