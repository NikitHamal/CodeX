package com.codex.apk;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * PerformanceMonitor tracks app performance metrics and provides optimization suggestions.
 */
public class PerformanceMonitor {
    
    private static final String TAG = "PerformanceMonitor";
    private static final long MONITORING_INTERVAL = 5000; // 5 seconds
    private static final int MAX_MEMORY_SAMPLES = 20;
    
    private static PerformanceMonitor instance;
    private final Context context;
    private final Handler monitoringHandler;
    private final List<Long> memorySamples;
    private final List<Long> timestamps;
    
    private boolean isMonitoring = false;
    private long startTime;
    private PerformanceListener listener;
    
    /**
     * Interface for performance monitoring callbacks.
     */
    public interface PerformanceListener {
        void onMemoryWarning(long currentMemory, long maxMemory);
        void onPerformanceSuggestion(String suggestion);
    }
    
    private PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.monitoringHandler = new Handler(Looper.getMainLooper());
        this.memorySamples = new ArrayList<>();
        this.timestamps = new ArrayList<>();
    }
    
    /**
     * Gets the singleton instance of PerformanceMonitor.
     */
    public static synchronized PerformanceMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceMonitor(context);
        }
        return instance;
    }
    
    /**
     * Starts performance monitoring.
     */
    public void startMonitoring(PerformanceListener listener) {
        this.listener = listener;
        if (!isMonitoring) {
            isMonitoring = true;
            startTime = System.currentTimeMillis();
            scheduleNextMonitoring();
            Log.d(TAG, "Performance monitoring started");
        }
    }
    
    /**
     * Stops performance monitoring.
     */
    public void stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            monitoringHandler.removeCallbacksAndMessages(null);
            Log.d(TAG, "Performance monitoring stopped");
        }
    }
    
    /**
     * Gets current memory usage information.
     */
    public MemoryInfo getCurrentMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return new MemoryInfo(usedMemory, maxMemory, totalMemory, freeMemory, memoryInfo.availMem, memoryInfo.lowMemory);
    }
    
    /**
     * Gets performance statistics.
     */
    public PerformanceStats getPerformanceStats() {
        long uptime = System.currentTimeMillis() - startTime;
        double avgMemoryUsage = 0;
        
        if (!memorySamples.isEmpty()) {
            long sum = 0;
            for (long sample : memorySamples) {
                sum += sample;
            }
            avgMemoryUsage = (double) sum / memorySamples.size();
        }
        
        MemoryInfo currentMemory = getCurrentMemoryInfo();
        
        return new PerformanceStats(uptime, avgMemoryUsage, memorySamples.size(), 
                                   currentMemory.usedMemory, currentMemory.maxMemory);
    }
    
    /**
     * Forces a garbage collection and returns memory freed.
     */
    public long forceGarbageCollection() {
        MemoryInfo beforeGC = getCurrentMemoryInfo();
        System.gc();
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        MemoryInfo afterGC = getCurrentMemoryInfo();
        long memoryFreed = beforeGC.usedMemory - afterGC.usedMemory;
        
        Log.d(TAG, "Garbage collection freed " + (memoryFreed / 1024 / 1024) + " MB");
        return memoryFreed;
    }
    
    /**
     * Schedules the next monitoring cycle.
     */
    private void scheduleNextMonitoring() {
        if (!isMonitoring) return;
        
        monitoringHandler.postDelayed(() -> {
            performMonitoringCycle();
            scheduleNextMonitoring();
        }, MONITORING_INTERVAL);
    }
    
    /**
     * Performs a single monitoring cycle.
     */
    private void performMonitoringCycle() {
        MemoryInfo memoryInfo = getCurrentMemoryInfo();
        long currentTime = System.currentTimeMillis();
        
        // Add sample
        memorySamples.add(memoryInfo.usedMemory);
        timestamps.add(currentTime);
        
        // Keep only recent samples
        while (memorySamples.size() > MAX_MEMORY_SAMPLES) {
            memorySamples.remove(0);
            timestamps.remove(0);
        }
        
        // Check for memory warnings
        double memoryUsagePercent = (double) memoryInfo.usedMemory / memoryInfo.maxMemory;
        if (memoryUsagePercent > 0.8 && listener != null) {
            listener.onMemoryWarning(memoryInfo.usedMemory, memoryInfo.maxMemory);
        }
        
        // Analyze trends and provide suggestions
        analyzeTrendsAndSuggest();
    }
    
    /**
     * Analyzes memory trends and provides optimization suggestions.
     */
    private void analyzeTrendsAndSuggest() {
        if (memorySamples.size() < 5 || listener == null) return;
        
        // Check for memory leaks (consistently increasing memory)
        int increasingCount = 0;
        for (int i = 1; i < memorySamples.size(); i++) {
            if (memorySamples.get(i) > memorySamples.get(i - 1)) {
                increasingCount++;
            }
        }
        
        double increasingRatio = (double) increasingCount / (memorySamples.size() - 1);
        if (increasingRatio > 0.7) {
            listener.onPerformanceSuggestion("Possible memory leak detected. Consider closing unused tabs or restarting the app.");
        }
        
        // Check for high memory usage
        MemoryInfo currentMemory = getCurrentMemoryInfo();
        double memoryUsagePercent = (double) currentMemory.usedMemory / currentMemory.maxMemory;
        
        if (memoryUsagePercent > 0.75) {
            listener.onPerformanceSuggestion("High memory usage detected (" + 
                Math.round(memoryUsagePercent * 100) + "%). Consider closing some tabs or files.");
        }
        
        // Check for low system memory
        if (currentMemory.isLowMemory) {
            listener.onPerformanceSuggestion("System memory is low. Consider closing other apps or saving your work.");
        }
    }
    
    /**
     * Memory information container class.
     */
    public static class MemoryInfo {
        public final long usedMemory;
        public final long maxMemory;
        public final long totalMemory;
        public final long freeMemory;
        public final long availableSystemMemory;
        public final boolean isLowMemory;
        
        public MemoryInfo(long usedMemory, long maxMemory, long totalMemory, long freeMemory,
                         long availableSystemMemory, boolean isLowMemory) {
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.availableSystemMemory = availableSystemMemory;
            this.isLowMemory = isLowMemory;
        }
        
        public String getFormattedUsedMemory() {
            return formatBytes(usedMemory);
        }
        
        public String getFormattedMaxMemory() {
            return formatBytes(maxMemory);
        }
        
        public double getUsagePercentage() {
            return (double) usedMemory / maxMemory * 100;
        }
    }
    
    /**
     * Performance statistics container class.
     */
    public static class PerformanceStats {
        public final long uptimeMs;
        public final double avgMemoryUsage;
        public final int sampleCount;
        public final long currentMemoryUsage;
        public final long maxMemoryAvailable;
        
        public PerformanceStats(long uptimeMs, double avgMemoryUsage, int sampleCount,
                               long currentMemoryUsage, long maxMemoryAvailable) {
            this.uptimeMs = uptimeMs;
            this.avgMemoryUsage = avgMemoryUsage;
            this.sampleCount = sampleCount;
            this.currentMemoryUsage = currentMemoryUsage;
            this.maxMemoryAvailable = maxMemoryAvailable;
        }
        
        public String getFormattedUptime() {
            long seconds = uptimeMs / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            
            if (hours > 0) {
                return String.format("%dh %dm", hours, minutes % 60);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds % 60);
            } else {
                return String.format("%ds", seconds);
            }
        }
    }
    
    /**
     * Formats bytes into human-readable format.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
}