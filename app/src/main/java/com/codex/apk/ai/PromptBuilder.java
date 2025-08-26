package com.codex.apk.ai;

import com.codex.apk.ChatMessage;
import com.codex.apk.TabItem;

import java.io.File;
import java.util.Deque;

public class PromptBuilder {

    public static String buildPromptForStep(ChatMessage.PlanStep target, ChatMessage planMsg, int idx, Deque<String> executedStepSummaries, File projectDir, TabItem activeTab) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are executing an approved plan step.\\n");
        prompt.append("Step ID: ").append(target.id != null ? target.id : String.valueOf(idx + 1)).append("\\n");
        prompt.append("Step Title: ").append(target.title != null ? target.title : "").append("\\n\\n");
        prompt.append("Output strictly a single JSON object with action=\\\"file_operation\\\" inside a ```json fenced code block.\\n");
        prompt.append("No natural language outside the JSON. Use fields appropriate to file operations.\\n\\n");
        prompt.append("Context summary:\\n");
        prompt.append("- Plan (truncated): ").append(safeTruncate(planToJson(planMsg), 2000)).append("\\n");
        if (!executedStepSummaries.isEmpty()) {
            prompt.append("- Executed steps so far:\\n");
            int count = 0;
            for (String ssum : executedStepSummaries) {
                if (count++ >= 10) break;
                prompt.append("  • ").append(ssum).append("\\n");
            }
        }
        prompt.append("- File tree (project root):\\n");
        prompt.append(safeTruncate(buildFileTree(projectDir, 3, 200), 3000)).append("\\n");
        if (activeTab != null) {
            prompt.append("- Active file: ").append(activeTab.getFileName()).append("\\n");
            prompt.append("- Active file content (truncated):\\n---\\n");
            prompt.append(safeTruncate(activeTab.getContent(), 2000)).append("\\n---\\n");
        }
        prompt.append("Proceed now with the step. Return only the JSON.\\n");
        return prompt.toString();
    }

    private static String planToJson(ChatMessage planMsg) {
        try {
            String raw = planMsg.getRawApiResponse();
            if (raw != null) {
                String trimmed = raw.trim();
                if (trimmed.startsWith("{")) return raw;
                String extracted = extractFirstJsonObjectFromText(raw);
                if (extracted != null) return extracted;
            }
        } catch (Exception ignored) {}
        return "{\\\"action\\\":\\\"plan\\\"}";
    }

    private static String safeTruncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "\\n...";
    }

    private static String buildFileTree(File root, int maxDepth, int maxEntries) {
        StringBuilder sb = new StringBuilder();
        buildFileTreeRec(root, 0, maxDepth, sb, new int[]{0}, maxEntries);
        return sb.toString();
    }

    private static void buildFileTreeRec(File dir, int depth, int maxDepth, StringBuilder sb, int[] count, int maxEntries) {
        if (dir == null || !dir.exists() || count[0] >= maxEntries) return;
        if (depth > maxDepth) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (count[0]++ >= maxEntries) return;
            for (int i = 0; i < depth; i++) sb.append("  ");
            sb.append(f.isDirectory() ? "[d] " : "[f] ").append(f.getName()).append("\\n");
            if (f.isDirectory()) buildFileTreeRec(f, depth + 1, maxDepth, sb, count, maxEntries);
        }
    }

    private static String extractFirstJsonObjectFromText(String input) {
        if (input == null) return null;
        try {
            String s = input.trim();
            int fenceStart = indexOfIgnoreCase(s, "```json");
            if (fenceStart >= 0) {
                fenceStart = s.indexOf('{', fenceStart);
                if (fenceStart >= 0) {
                    int end = findMatchingBraceEnd(s, fenceStart);
                    if (end > fenceStart) return s.substring(fenceStart, end + 1);
                }
            }
            int firstBrace = s.indexOf('{');
            if (firstBrace >= 0) {
                int end = findMatchingBraceEnd(s, firstBrace);
                if (end > firstBrace) return s.substring(firstBrace, end + 1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().indexOf(needle.toLowerCase());
    }

    private static int findMatchingBraceEnd(String s, int startIdx) {
        int depth = 0; boolean inString = false; boolean escape = false;
        for (int i = startIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) { escape = false; continue; }
                if (c == '\\') { escape = true; continue; }
                if (c == '\"') inString = false;
                continue;
            }
            if (c == '\"') { inString = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
