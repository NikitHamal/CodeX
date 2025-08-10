package com.codex.apk;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class GitManager {
    private static final String TAG = "GitManager";
    private final Context context;
    private final File projectsDir;

    public interface GitCloneCallback {
        void onProgress(String message, int percentage);
        void onSuccess(String projectPath, String projectName);
        void onError(String error);
    }

    public GitManager(Context context) {
        this.context = context;
        this.projectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");
        if (!projectsDir.exists()) {
            projectsDir.mkdirs();
        }
    }

    public void cloneRepository(String repositoryUrl, String projectName, GitCloneCallback callback) {
        new Thread(() -> {
            try {
                // Validate URL
                if (!isValidGitUrl(repositoryUrl)) {
                    callback.onError(context.getString(R.string.invalid_repository_url));
                    return;
                }

                // Check if project already exists
                File projectDir = new File(projectsDir, projectName);
                if (projectDir.exists()) {
                    callback.onError(context.getString(R.string.project_with_this_name_already_exists));
                    return;
                }

                callback.onProgress(context.getString(R.string.cloning_repository), 0);

                // Clone the repository
                Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(projectDir)
                    .setProgressMonitor(new ProgressMonitor() {
                        private int totalTasks = 0;
                        private int tasksCompleted = 0;
                        private int currentTaskTotal = 0;
                        private int currentTaskDone = 0;

                        private int computeProgressPercent() {
                            if (totalTasks <= 0) {
                                return clamp(currentTaskTotal > 0 ? (int) (100.0 * currentTaskDone / Math.max(1, currentTaskTotal)) : 0, 0, 99);
                            }
                            double perTask = 100.0 / totalTasks;
                            double progress = tasksCompleted * perTask;
                            if (currentTaskTotal > 0) {
                                progress += perTask * ((double) currentTaskDone / (double) currentTaskTotal);
                            }
                            return clamp((int) progress, 0, 99);
                        }

                        @Override
                        public void start(int totalTasks) {
                            this.totalTasks = totalTasks;
                            callback.onProgress("Starting clone operation...", 0);
                        }

                        @Override
                        public void beginTask(String title, int totalWork) {
                            // Reset current task counters
                            currentTaskTotal = totalWork > 0 ? totalWork : 0;
                            currentTaskDone = 0;
                            int pct = computeProgressPercent();
                            callback.onProgress("Cloning: " + title, pct);
                        }

                        @Override
                        public void update(int completed) {
                            // Increment current task progress; avoid overflow
                            if (completed > 0 && currentTaskTotal > 0) {
                                currentTaskDone = Math.min(currentTaskTotal, currentTaskDone + completed);
                            }
                            int pct = computeProgressPercent();
                            callback.onProgress("Cloning in progress...", pct);
                        }

                        @Override
                        public void endTask() {
                            // Mark current task as completed
                            if (tasksCompleted < totalTasks) {
                                tasksCompleted++;
                            }
                            currentTaskDone = currentTaskTotal; // finalize
                        }

                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                    })
                    .call();

                callback.onProgress("Finalizing...", 90);

                // Verify the clone was successful
                if (projectDir.exists() && new File(projectDir, ".git").exists()) {
                    callback.onProgress("Clone completed!", 100);
                    callback.onSuccess(projectDir.getAbsolutePath(), projectName);
                } else {
                    callback.onError("Clone completed but repository structure is invalid");
                }

            } catch (GitAPIException e) {
                Log.e(TAG, "Git clone failed", e);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Unknown Git error occurred";
                }
                callback.onError(context.getString(R.string.failed_to_clone_repository, errorMessage));
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during clone", e);
                callback.onError(context.getString(R.string.failed_to_clone_repository, e.getMessage()));
            }
        }).start();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // Compute overall percentage based on tasks and current task progress
    private int computePercentInternal(int totalTasks, int tasksCompleted, int currentTaskDone, int currentTaskTotal) {
        if (totalTasks <= 0) return clamp(currentTaskTotal > 0 ? (int) (100.0 * currentTaskDone / Math.max(1, currentTaskTotal)) : 0, 0, 99);
        double perTask = 100.0 / totalTasks;
        double progress = tasksCompleted * perTask;
        if (currentTaskTotal > 0) {
            progress += perTask * ((double) currentTaskDone / (double) currentTaskTotal);
        }
        return clamp((int) progress, 0, 99);
    }

    // Wrapper to compute using the instance fields within ProgressMonitor
    private int computeProgressPercent() {
        // This method body will be overridden at runtime by the anonymous ProgressMonitor's context,
        // but the compiler requires it here. Not used outside the monitor.
        return 0;
    }

    public boolean isValidGitUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // Check for common Git URL patterns
        String trimmedUrl = url.trim();
        
        // HTTPS URLs
        if (trimmedUrl.startsWith("https://")) {
            return trimmedUrl.contains("github.com") || 
                   trimmedUrl.contains("gitlab.com") || 
                   trimmedUrl.contains("bitbucket.org") ||
                   trimmedUrl.endsWith(".git");
        }
        
        // SSH URLs
        if (trimmedUrl.startsWith("git@")) {
            return trimmedUrl.contains("github.com") || 
                   trimmedUrl.contains("gitlab.com") || 
                   trimmedUrl.contains("bitbucket.org") ||
                   trimmedUrl.endsWith(".git");
        }
        
        // Git protocol URLs
        if (trimmedUrl.startsWith("git://")) {
            return trimmedUrl.endsWith(".git");
        }

        return false;
    }

    public String extractProjectNameFromUrl(String repositoryUrl) {
        try {
            String url = repositoryUrl.trim();
            
            // Remove .git extension if present
            if (url.endsWith(".git")) {
                url = url.substring(0, url.length() - 4);
            }
            
            // Extract the last part of the URL path
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String[] pathParts = path.split("/");
                if (pathParts.length > 0) {
                    return pathParts[pathParts.length - 1];
                }
            }
            
            // Fallback: use a default name
            return "cloned-project";
            
        } catch (URISyntaxException e) {
            Log.w(TAG, "Failed to parse URL for project name extraction", e);
            return "cloned-project";
        }
    }
}