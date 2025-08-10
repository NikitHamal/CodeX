package com.codex.apk.lint;

import java.util.ArrayList;
import java.util.List;

public class JsLinter {
    public List<LintIssue> lint(String path, String content) {
        List<LintIssue> issues = new ArrayList<>();
        // Tokenize and track line/col with a simple scanner; ignore strings/comments for balance
        int line = 1, col = 1;
        boolean inSgl = false, inDbl = false, inTpl = false, inLineCmt = false, inBlkCmt = false;
        int balParen = 0, balBrace = 0, balBracket = 0;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            char next = i + 1 < content.length() ? content.charAt(i + 1) : '\0';
            if (inLineCmt) {
                if (ch == '\n') { inLineCmt = false; line++; col = 1; } else { col++; }
                continue;
            }
            if (inBlkCmt) {
                if (ch == '*' && next == '/') { inBlkCmt = false; i++; col += 2; } else { if (ch=='\n'){ line++; col=1; } else col++; }
                continue;
            }
            if (!inSgl && !inDbl && !inTpl && ch == '/' && next == '/') { inLineCmt = true; i++; col += 2; continue; }
            if (!inSgl && !inDbl && !inTpl && ch == '/' && next == '*') { inBlkCmt = true; i++; col += 2; continue; }
            if (!inDbl && !inTpl && ch == '\'' ) { inSgl = !inSgl; col++; continue; }
            if (!inSgl && !inTpl && ch == '"' ) { inDbl = !inDbl; col++; continue; }
            if (!inSgl && !inDbl && ch == '`' ) { inTpl = !inTpl; col++; continue; }
            if (ch == '\n') { line++; col = 1; continue; }
            if (inSgl || inDbl || inTpl) { col++; continue; }
            // Not in string/comment → track brackets
            if (ch == '(') balParen++; else if (ch == ')') balParen--;
            if (ch == '{') balBrace++; else if (ch == '}') balBrace--;
            if (ch == '[') balBracket++; else if (ch == ']') balBracket--;
            col++;
        }
        if (balParen != 0) issues.add(new LintIssue(path, line, col, LintIssue.Severity.ERROR, "Unbalanced parentheses"));
        if (balBrace != 0) issues.add(new LintIssue(path, line, col, LintIssue.Severity.ERROR, "Unbalanced braces"));
        if (balBracket != 0) issues.add(new LintIssue(path, line, col, LintIssue.Severity.ERROR, "Unbalanced brackets"));
        if (content.contains("console.log(\"\")")) {
            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.WARNING, "Suspicious console.log call"));
        }
        return issues;
    }

    // intentionally no general balance helper; handled by scanner to ignore strings/comments
}


