package com.codex.apk.editor;

import com.codex.apk.ChatMessage;
import com.codex.apk.DiffUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiResponseUtils {
    private AiResponseUtils() {}

    static String extractJsonBlock(String primary, String fallback) {
        String value = extractJsonFromContent(primary);
        if (value != null) {
            return value;
        }
        return extractJsonFromContent(fallback);
    }

    static String extractJsonFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        Pattern fencedJson = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = fencedJson.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        Pattern fenced = Pattern.compile("```\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        matcher = fenced.matcher(content);
        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            if (looksLikeJson(extracted)) {
                return extracted;
            }
        }

        if (looksLikeJson(content)) {
            return content.trim();
        }

        return null;
    }

    static boolean looksLikeJson(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (!((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]")))) {
            return false;
        }
        try {
            JsonParser.parseString(trimmed);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static JsonArray extractToolCalls(String jsonCandidate) {
        if (jsonCandidate == null || jsonCandidate.isEmpty()) {
            return null;
        }
        try {
            JsonObject maybe = JsonParser.parseString(jsonCandidate).getAsJsonObject();
            if (maybe.has("action")
                    && "tool_call".equalsIgnoreCase(maybe.get("action").getAsString())
                    && maybe.has("tool_calls")
                    && maybe.get("tool_calls").isJsonArray()) {
                return maybe.getAsJsonArray("tool_calls");
            }
        } catch (Exception ignored) {}
        return null;
    }

    static String buildFileChangeSummary(List<ChatMessage.FileActionDetail> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }

        LinkedHashMap<String, int[]> countsByPath = new LinkedHashMap<>();
        Set<String> allPaths = new LinkedHashSet<>();
        for (ChatMessage.FileActionDetail detail : details) {
            if (detail.path != null && !detail.path.isEmpty()) {
                allPaths.add(detail.path);
            }
            if ("renameFile".equals(detail.type)) {
                if (detail.oldPath != null && !detail.oldPath.isEmpty()) {
                    allPaths.add(detail.oldPath);
                }
                if (detail.newPath != null && !detail.newPath.isEmpty()) {
                    allPaths.add(detail.newPath);
                }
            }
        }

        Map<String, Set<String>> aliases = new LinkedHashMap<>();
        for (String path : allPaths) {
            Set<String> aliasSet = new LinkedHashSet<>();
            aliasSet.add(path);
            for (int pass = 0; pass < 3; pass++) {
                boolean changed = false;
                for (ChatMessage.FileActionDetail detail : details) {
                    if ("renameFile".equals(detail.type)
                            && detail.newPath != null
                            && aliasSet.contains(detail.newPath)
                            && detail.oldPath != null) {
                        if (aliasSet.add(detail.oldPath)) {
                            changed = true;
                        }
                    }
                }
                if (!changed) {
                    break;
                }
            }
            aliases.put(path, aliasSet);
        }

        Function<ChatMessage.FileActionDetail, String> displayPath = detail -> {
            if ("renameFile".equals(detail.type) && detail.newPath != null && !detail.newPath.isEmpty()) {
                return detail.newPath;
            }
            return detail.path != null ? detail.path : "";
        };

        for (ChatMessage.FileActionDetail detail : details) {
            String key = displayPath.apply(detail);
            if (key.isEmpty()) {
                continue;
            }
            int[] totals = countsByPath.computeIfAbsent(key, unused -> new int[]{0, 0});
            if ("modifyLines".equals(detail.type)) {
                totals[0] += (detail.insertLines != null) ? detail.insertLines.size() : 0;
                totals[1] += Math.max(0, detail.deleteCount);
            } else if (detail.diffPatch != null && !detail.diffPatch.isEmpty()) {
                int[] delta = DiffUtils.countAddRemove(detail.diffPatch);
                totals[0] += delta[0];
                totals[1] += delta[1];
            } else if ("createFile".equals(detail.type) && detail.newContent != null) {
                totals[0] += countLines(detail.newContent);
            } else if ("deleteFile".equals(detail.type) && detail.oldContent != null) {
                totals[1] += countLines(detail.oldContent);
            } else if (detail.oldContent != null || detail.newContent != null) {
                int[] delta = DiffUtils.countAddRemoveFromContents(detail.oldContent, detail.newContent);
                totals[0] += delta[0];
                totals[1] += delta[1];
            }
        }

        if (countsByPath.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder();
        summary.append("Changes:\n");
        for (Map.Entry<String, int[]> entry : countsByPath.entrySet()) {
            int[] totals = entry.getValue();
            summary.append("- ").append(entry.getKey());
            if (totals[0] > 0 || totals[1] > 0) {
                summary.append(" (+").append(totals[0]).append(" -").append(totals[1]).append(")");
            }
            summary.append('\n');
        }
        return summary.toString().trim();
    }

    private static int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
