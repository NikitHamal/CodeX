package com.codex.apk.codebase;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitors a directory and its subdirectories for file system changes
 * and notifies a listener. Implements debouncing for rapid changes.
 */
public class FileWatcher implements Runnable {
    private static final String TAG = "FileWatcher";
    private final WatchService watchService;
    private final File projectDir;
    private final FileChangeListener listener;
    private final ExecutorService watchExecutor;
    private final ScheduledExecutorService debounceExecutor;
    private final Map<Path, Long> lastEventTimestamps = new HashMap<>();
    private static final long DEBOUNCE_DELAY_MS = 1000; // 1 second debounce

    public FileWatcher(File projectDir, FileChangeListener listener) throws IOException {
        this.projectDir = projectDir;
        this.listener = listener;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchExecutor = Executors.newSingleThreadExecutor();
        this.debounceExecutor = Executors.newSingleThreadScheduledExecutor();
        registerDirectory(projectDir.toPath());
    }

    /**
     * Recursively registers a directory and its subdirectories with the WatchService.
     * @param path The path to the directory to register.
     * @throws IOException If an I/O error occurs.
     */
    private void registerDirectory(Path path) throws IOException {
        if (!path.toFile().isDirectory()) {
            return;
        }
        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        Log.d(TAG, "Registered directory for watching: " + path);

        // Register subdirectories
        File[] files = path.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    registerDirectory(file.toPath());
                }
            }
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "FileWatcher started for project: " + projectDir.getAbsolutePath());
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take(); // Blocks until an event is present
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path context = (Path) event.context();
                    Path changedPath = dir.resolve(context);
                    File changedFile = changedPath.toFile();

                    Log.d(TAG, "Detected event: " + kind + " on " + changedFile.getAbsolutePath());

                    // Handle directory creation/deletion to register/unregister
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && changedFile.isDirectory()) {
                        try {
                            registerDirectory(changedPath);
                        } catch (IOException e) {
                            Log.e(TAG, "Error registering new directory: " + changedPath, e);
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE && changedFile.isDirectory()) {
                        // WatchKey for deleted directory becomes invalid automatically
                        Log.d(TAG, "Directory deleted: " + changedPath);
                    }

                    // Debounce and notify listener
                    long currentTime = System.currentTimeMillis();
                    lastEventTimestamps.put(changedPath, currentTime);

                    debounceExecutor.schedule(() -> {
                        if (lastEventTimestamps.containsKey(changedPath) &&
                                (currentTime == lastEventTimestamps.get(changedPath))) {
                            // No new events for this path within the debounce period
                            if (listener != null) {
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    listener.onFileCreated(changedFile);
                                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    listener.onFileModified(changedFile);
                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    listener.onFileDeleted(changedFile);
                                }
                            }
                            lastEventTimestamps.remove(changedPath); // Clean up
                        }
                    }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
                }

                boolean valid = key.reset();
                if (!valid) {
                    Log.w(TAG, "WatchKey no longer valid. Directory might have been deleted or unmounted.");
                    // If a directory is deleted, its key becomes invalid.
                    // We might need to re-register the parent if the project structure allows.
                    // For now, if the projectDir itself is deleted, the watcher will stop.
                    if (!projectDir.exists()) {
                        Log.e(TAG, "Project directory no longer exists. Stopping FileWatcher.");
                        break;
                    }
                }
            }
        } finally {
            try {
                watchService.close();
                watchExecutor.shutdownNow();
                debounceExecutor.shutdownNow();
                Log.d(TAG, "FileWatcher stopped and resources closed.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing WatchService: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Starts the file watcher in a new thread.
     */
    public void start() {
        watchExecutor.execute(this);
    }

    /**
     * Stops the file watcher.
     */
    public void stop() {
        watchExecutor.shutdownNow();
        debounceExecutor.shutdownNow();
        try {
            watchService.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing WatchService during stop: " + e.getMessage(), e);
        }
    }
}
