package com.codex.apk;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp3.mockwebserver.MockWebServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalServerManager {
    private static final String TAG = "LocalServerManager";
    private final Context context;
    private MockWebServer server;
    private ScheduledExecutorService executor;
    private int currentPort = 8080;
    private boolean isRunning = false;
    private String projectPath;
    private String projectType;

    public interface ServerCallback {
        void onServerStarted(int port);
        void onServerStopped();
        void onError(String error);
    }

    public LocalServerManager(Context context) {
        this.context = context;
        this.executor = Executors.newScheduledThreadPool(1);
    }

    public void startServer(String projectPath, String projectType, int port, ServerCallback callback) {
        if (isRunning) {
            callback.onError("Server is already running");
            return;
        }

        this.projectPath = projectPath;
        this.projectType = projectType;
        this.currentPort = port;

        // Check if port is available
        if (!isPortAvailable(port)) {
            callback.onError("Port " + port + " is already in use");
            return;
        }

        executor.submit(() -> {
            try {
                server = new MockWebServer();
                
                // Configure server based on project type
                configureServerForProjectType(projectPath, projectType);
                
                // Start the server
                server.start(port);
                isRunning = true;
                
                Log.i(TAG, "Local server started on port " + port);
                callback.onServerStarted(port);
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to start local server", e);
                callback.onError("Failed to start server: " + e.getMessage());
            }
        });
    }

    public void stopServer(ServerCallback callback) {
        if (!isRunning || server == null) {
            callback.onServerStopped();
            return;
        }

        executor.submit(() -> {
            try {
                server.shutdown();
                server = null;
                isRunning = false;
                
                Log.i(TAG, "Local server stopped");
                callback.onServerStopped();
                
            } catch (IOException e) {
                Log.e(TAG, "Error stopping server", e);
                callback.onError("Error stopping server: " + e.getMessage());
            }
        });
    }

    private void configureServerForProjectType(String projectPath, String projectType) {
        File projectDir = new File(projectPath);
        
        switch (projectType) {
            case "html_css_js":
            case "empty":
                // Serve static files
                serveStaticFiles(projectDir);
                break;
                
            case "react":
                // React development server simulation
                serveReactApp(projectDir);
                break;
                
            case "nextjs":
                // Next.js development server simulation
                serveNextJsApp(projectDir);
                break;
                
            case "vue":
                // Vue.js development server simulation
                serveVueApp(projectDir);
                break;
                
            case "angular":
                // Angular development server simulation
                serveAngularApp(projectDir);
                break;
                
            case "node":
                // Node.js backend simulation
                serveNodeBackend(projectDir);
                break;
                
            case "python":
                // Python backend simulation
                servePythonBackend(projectDir);
                break;
                
            case "php":
                // PHP backend simulation
                servePhpBackend(projectDir);
                break;
                
            default:
                // Default to static file serving
                serveStaticFiles(projectDir);
                break;
        }
    }

    private void serveStaticFiles(File projectDir) {
        // Serve static files from project directory
        // This is a simplified implementation - in a real app, you'd want more sophisticated file serving
        try {
            // Add basic routes for common files
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("Static file server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring static file server", e);
        }
    }

    private void serveReactApp(File projectDir) {
        try {
            // Simulate React development server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("React development server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring React server", e);
        }
    }

    private void serveNextJsApp(File projectDir) {
        try {
            // Simulate Next.js development server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("Next.js development server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Next.js server", e);
        }
    }

    private void serveVueApp(File projectDir) {
        try {
            // Simulate Vue.js development server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("Vue.js development server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Vue.js server", e);
        }
    }

    private void serveAngularApp(File projectDir) {
        try {
            // Simulate Angular development server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("Angular development server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Angular server", e);
        }
    }

    private void serveNodeBackend(File projectDir) {
        try {
            // Simulate Node.js backend server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("Node.js backend server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Node.js server", e);
        }
    }

    private void servePythonBackend(File projectDir) {
        try {
            // Simulate Python backend server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("Python backend server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Python server", e);
        }
    }

    private void servePhpBackend(File projectDir) {
        try {
            // Simulate PHP backend server
            server.enqueue(new com.squareup.okhttp3.mockwebserver.MockResponse()
                .setBody("PHP backend server running for: " + projectDir.getName())
                .setResponseCode(200));
        } catch (Exception e) {
            Log.e(TAG, "Error configuring PHP server", e);
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isServerRunning() {
        return isRunning;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public String getServerUrl() {
        if (isRunning) {
            return "http://localhost:" + currentPort;
        }
        return null;
    }

    public void shutdown() {
        if (isRunning) {
            stopServer(new ServerCallback() {
                @Override
                public void onServerStarted(int port) {}

                @Override
                public void onServerStopped() {}

                @Override
                public void onError(String error) {}
            });
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}