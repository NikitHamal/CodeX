package com.codex.apk.core.tools;

import com.codex.apk.core.model.ExecutionContext;
import com.codex.apk.core.model.ToolCall;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

/**
 * Minimal file system tool executor to satisfy references.
 * NOTE: Implementations are simple and should be hardened before production use.
 */
class FileSystemToolExecutor implements ToolExecutor {

    @Override
    public ToolResult execute(ToolCall call, ExecutionContext context) throws Exception {
        String name = call.getName();
        String args = call.getArguments();
        // Very basic argument handling: args is expected to be a simple JSON-like string.
        // For compile/unblock purposes, we support a minimal subset.
        try {
            switch (name) {
                case "createFile": {
                    Path p = resolvePath(context, extract(args, "path"));
                    String content = extract(args, "content");
                    Files.createDirectories(p.getParent());
                    Files.write(p, (content == null ? "" : content).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    return ToolResult.success(call.getId(), "created:" + p.toString());
                }
                case "readFile": {
                    Path p = resolvePath(context, extract(args, "path"));
                    String content = Files.exists(p) ? new String(Files.readAllBytes(p)) : "";
                    return ToolResult.success(call.getId(), content);
                }
                case "updateFile": {
                    Path p = resolvePath(context, extract(args, "path"));
                    String content = extract(args, "content");
                    Files.write(p, (content == null ? "" : content).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    return ToolResult.success(call.getId(), "updated:" + p.toString());
                }
                case "deleteFile": {
                    Path p = resolvePath(context, extract(args, "path"));
                    Files.deleteIfExists(p);
                    return ToolResult.success(call.getId(), "deleted:" + p.toString());
                }
                case "listFiles": {
                    Path dir = resolvePath(context, extract(args, "path"));
                    if (!Files.isDirectory(dir)) return ToolResult.success(call.getId(), "[]");
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    java.util.stream.Stream<Path> s = Files.list(dir);
                    boolean first = true;
                    for (Path p : (Iterable<Path>) s::iterator) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append('"').append(p.getFileName().toString()).append('"');
                    }
                    sb.append("]");
                    return ToolResult.success(call.getId(), sb.toString());
                }
                case "searchFiles": {
                    Path dir = resolvePath(context, extract(args, "path"));
                    String pattern = extract(args, "pattern");
                    if (!Files.isDirectory(dir)) return ToolResult.success(call.getId(), "[]");
                    java.util.List<String> matches = new java.util.ArrayList<>();
                    java.nio.file.FileSystem fs = dir.getFileSystem();
                    java.nio.file.PathMatcher matcher = fs.getPathMatcher("glob:" + (pattern == null ? "*" : pattern));
                    Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .forEach(p -> { if (matcher.matches(p.getFileName())) matches.add(p.toString()); });
                    return ToolResult.success(call.getId(), matches.toString());
                }
                default:
                    return ToolResult.error(call.getId(), "Unsupported file tool: " + name, null);
            }
        } catch (Exception e) {
            return ToolResult.error(call.getId(), e.getMessage(), e);
        }
    }

    @Override
    public Set<Permission> getRequiredPermissions() {
        return EnumSet.of(Permission.FILE_READ, Permission.FILE_WRITE, Permission.FILE_DELETE);
    }

    @Override
    public boolean isCompatibleWith(ExecutionContext context) {
        File project = context != null ? context.getProjectDir() : null;
        return project != null && project.exists();
    }

    private Path resolvePath(ExecutionContext ctx, String rel) {
        File base = ctx.getProjectDir();
        if (rel == null) rel = "";
        return new File(base, rel).toPath();
    }

    // extremely naive key extractor from a json-ish string: "key":"value"
    private String extract(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = json.indexOf('"', i + needle.length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}
