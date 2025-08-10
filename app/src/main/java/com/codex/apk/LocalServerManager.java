package com.codex.apk;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
    private ServerSocket serverSocket;
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
                // Create a simple server socket
                serverSocket = new ServerSocket(port);
                isRunning = true;
                
                Log.i(TAG, "Local server started on port " + port);
                callback.onServerStarted(port);
                
                // Keep the server running
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        // Accept connections but don't process them for now
                        // This is a simplified implementation
                        serverSocket.accept();
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
        if (!isRunning || serverSocket == null) {
            callback.onServerStopped();
            return;
        }

        executor.submit(() -> {
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

    private void configureServerForProjectType(String projectPath, String projectType) {
        // Simplified configuration - just log the project type
        Log.i(TAG, "Configuring server for project type: " + projectType + " at path: " + projectPath);
    }

    private void serveStaticFiles(File projectDir) {
        // Simplified static file serving - just log
        Log.i(TAG, "Serving static files from: " + projectDir.getAbsolutePath());
    }

    private void serveReactApp(File projectDir) {
        Log.i(TAG, "Serving React app from: " + projectDir.getAbsolutePath());
    }

    private void serveNextJsApp(File projectDir) {
        Log.i(TAG, "Serving Next.js app from: " + projectDir.getAbsolutePath());
    }

    private void serveVueApp(File projectDir) {
        Log.i(TAG, "Serving Vue app from: " + projectDir.getAbsolutePath());
    }

    private void serveAngularApp(File projectDir) {
        Log.i(TAG, "Serving Angular app from: " + projectDir.getAbsolutePath());
    }

    private void serveNodeBackend(File projectDir) {
        Log.i(TAG, "Serving Node.js backend from: " + projectDir.getAbsolutePath());
    }

    private void servePythonBackend(File projectDir) {
        Log.i(TAG, "Serving Python backend from: " + projectDir.getAbsolutePath());
    }

    private void servePhpBackend(File projectDir) {
        Log.i(TAG, "Serving PHP backend from: " + projectDir.getAbsolutePath());
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