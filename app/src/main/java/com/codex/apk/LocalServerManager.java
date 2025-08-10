package com.codex.apk;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocalServerManager {
    private static final String TAG = "LocalServerManager";
    private final Context context;
    private ServerSocket serverSocket;
    private ExecutorService acceptExecutor;
    private ExecutorService handlerExecutor;
    private int currentPort = 8080;
    private volatile boolean isRunning = false;
    private String projectPath;
    private String projectType;
    private File serveRootDir;
    private File fallbackIndexFile;

    public interface ServerCallback {
        void onServerStarted(int port);
        void onServerStopped();
        void onError(String error);
    }

    public LocalServerManager(Context context) {
        this.context = context;
        this.acceptExecutor = Executors.newSingleThreadExecutor();
        this.handlerExecutor = Executors.newFixedThreadPool(4);
    }

    public void startServer(String projectPath, String projectType, int port, ServerCallback callback) {
        if (isRunning) {
            callback.onError("Server is already running");
            return;
        }

        this.projectPath = projectPath;
        this.projectType = projectType == null ? "html" : projectType.toLowerCase(Locale.ROOT);
        this.currentPort = port;

        if (!isPortAvailable(port)) {
            callback.onError("Port " + port + " is already in use");
            return;
        }

        try {
            configureServerForProjectType();
        } catch (Exception e) {
            callback.onError("Failed to configure server: " + e.getMessage());
            return;
        }

        acceptExecutor.submit(() -> {
            try {
                serverSocket = new ServerSocket(currentPort);
                isRunning = true;
                Log.i(TAG, "Local server started on port " + currentPort + ", root=" + serveRootDir);
                callback.onServerStarted(currentPort);

                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        handlerExecutor.submit(() -> handleClient(client));
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting connection", e);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to start local server", e);
                callback.onError("Failed to start server: " + e.getMessage());
            }
        });
    }

    public void stopServer(ServerCallback callback) {
        if (!isRunning) {
            callback.onServerStopped();
            return;
        }

        acceptExecutor.submit(() -> {
            try {
                isRunning = false;
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                serverSocket = null;
                Log.i(TAG, "Local server stopped");
                callback.onServerStopped();
            } catch (IOException e) {
                Log.e(TAG, "Error stopping server", e);
                callback.onError("Error stopping server: " + e.getMessage());
            }
        });
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                client.close();
                return;
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "/";

            // Consume remaining headers
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {}

            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                writeResponse(out, 405, "Method Not Allowed", "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            File target = resolvePath(path);
            if (target == null) {
                writeResponse(out, 404, "Not Found", "text/plain", "Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            if (target.isDirectory()) {
                File index = new File(target, "index.html");
                if (index.exists()) {
                    serveFile(out, index);
                } else if (fallbackIndexFile != null && fallbackIndexFile.exists()) {
                    serveFile(out, fallbackIndexFile);
                } else {
                    writeResponse(out, 403, "Forbidden", "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            if (target.exists() && target.isFile()) {
                serveFile(out, target);
                return;
            }

            // SPA fallback
            if (fallbackIndexFile != null && fallbackIndexFile.exists()) {
                serveFile(out, fallbackIndexFile);
                return;
            }

            writeResponse(out, 404, "Not Found", "text/plain", "Not Found".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Client handling error", e);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private File resolvePath(String path) {
        if (serveRootDir == null) return null;
        try {
            String clean = path;
            int queryIdx = clean.indexOf('?');
            if (queryIdx >= 0) clean = clean.substring(0, queryIdx);
            clean = clean.replace("..", "");
            if (clean.startsWith("/")) clean = clean.substring(1);
            File candidate = new File(serveRootDir, clean);
            if (!candidate.getCanonicalPath().startsWith(serveRootDir.getCanonicalPath())) {
                return null; // path traversal
            }
            return candidate;
        } catch (IOException e) {
            return null;
        }
    }

    private void serveFile(OutputStream out, File file) throws IOException {
        String mime = getMimeType(file.getName());
        byte[] header = ("HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mime + "; charset=UTF-8\r\n" +
                "Content-Length: " + file.length() + "\r\n" +
                "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        out.write(header);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        out.flush();
    }

    private void writeResponse(OutputStream out, int code, String message, String contentType, byte[] body) throws IOException {
        String status = "HTTP/1.1 " + code + " " + message + "\r\n";
        String headers = "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(status.getBytes(StandardCharsets.UTF_8));
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private void configureServerForProjectType() {
        File projectDir = new File(projectPath);
        if (!projectDir.exists()) {
            throw new IllegalStateException("Project directory does not exist: " + projectPath);
        }

        // Determine serve root and fallback index based on project type and common build outputs
        serveRootDir = determineServeRootDir(projectDir, projectType);
        fallbackIndexFile = determineFallbackIndexFile(serveRootDir, projectDir, projectType);

        Log.i(TAG, "Configured server type=" + projectType + ", serveRoot=" + serveRootDir + ", fallbackIndex=" + fallbackIndexFile);
    }

    private File determineServeRootDir(File projectDir, String projectType) {
        // Prefer built assets first
        switch (projectType) {
            case "nextjs":
                // Next.js export directory
                if (new File(projectDir, "out").exists()) return new File(projectDir, "out");
                if (new File(projectDir, ".next/static").exists()) return new File(projectDir, ".next");
                if (new File(projectDir, "public").exists()) return new File(projectDir, "public");
                return projectDir;
            case "react":
            case "material_ui":
                if (new File(projectDir, "build").exists()) return new File(projectDir, "build");
                if (new File(projectDir, "dist").exists()) return new File(projectDir, "dist");
                if (new File(projectDir, "public").exists()) return new File(projectDir, "public");
                return projectDir;
            case "vue":
                if (new File(projectDir, "dist").exists()) return new File(projectDir, "dist");
                if (new File(projectDir, "public").exists()) return new File(projectDir, "public");
                return projectDir;
            case "angular":
                File dist = new File(projectDir, "dist");
                if (dist.exists() && dist.isDirectory()) {
                    // If dist contains a single subdir, serve that
                    File[] children = dist.listFiles(File::isDirectory);
                    if (children != null && children.length == 1) return children[0];
                    return dist;
                }
                return projectDir;
            case "tailwind":
                if (new File(projectDir, "dist").exists()) return new File(projectDir, "dist");
                return projectDir;
            case "bootstrap":
            case "html":
            default:
                return projectDir;
        }
    }

    private File determineFallbackIndexFile(File serveRoot, File projectDir, String projectType) {
        // Find the most likely index.html to serve for SPA fallback
        File candidate;
        switch (projectType) {
            case "react":
            case "material_ui":
                candidate = new File(serveRoot, "index.html");
                if (candidate.exists()) return candidate;
                candidate = new File(projectDir, "public/index.html");
                if (candidate.exists()) return candidate;
                break;
            case "nextjs":
                candidate = new File(serveRoot, "index.html");
                if (candidate.exists()) return candidate;
                candidate = new File(projectDir, "out/index.html");
                if (candidate.exists()) return candidate;
                break;
            case "vue":
            case "angular":
            case "tailwind":
            case "bootstrap":
            case "html":
            default:
                candidate = new File(serveRoot, "index.html");
                if (candidate.exists()) return candidate;
                candidate = new File(projectDir, "index.html");
                if (candidate.exists()) return candidate;
        }
        return null;
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isServerRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public String getServerUrl() {
        if (isRunning) {
            return "http://localhost:" + currentPort + "/";
        }
        return null;
    }

    public void shutdown() {
        if (isRunning) {
            stopServer(new ServerCallback() {
                @Override public void onServerStarted(int port) {}
                @Override public void onServerStopped() {}
                @Override public void onError(String error) {}
            });
        }

        try {
            if (acceptExecutor != null) {
                acceptExecutor.shutdownNow();
                acceptExecutor.awaitTermination(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ignored) {}

        try {
            if (handlerExecutor != null) {
                handlerExecutor.shutdownNow();
                handlerExecutor.awaitTermination(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ignored) {}
    }

    private String getMimeType(String fileName) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "html":
            case "htm": return "text/html";
            case "css": return "text/css";
            case "js": return "application/javascript";
            case "mjs": return "application/javascript";
            case "json": return "application/json";
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "svg": return "image/svg+xml";
            case "ico": return "image/x-icon";
            case "woff": return "font/woff";
            case "woff2": return "font/woff2";
            case "ttf": return "font/ttf";
            case "eot": return "application/vnd.ms-fontobject";
            case "map": return "application/json";
            default: return "text/plain";
        }
    }
}