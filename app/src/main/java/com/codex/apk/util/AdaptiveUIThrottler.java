package com.codex.apk.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.common.util.concurrent.RateLimiter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AdaptiveUIThrottler {

    private final RateLimiter rateLimiter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Queue<UIUpdateTask> pendingUpdates = new ConcurrentLinkedQueue<>();
    private final Context context;

    public AdaptiveUIThrottler(Context context) {
        this.context = context.getApplicationContext();
        this.rateLimiter = RateLimiter.create(getOptimalRateForDevice());
    }

    public void scheduleUpdate(Runnable update, int priority) {
        UIUpdateTask task = new UIUpdateTask(update, priority, System.currentTimeMillis());
        pendingUpdates.offer(task);
        processNextUpdate();
    }

    private void processNextUpdate() {
        if (rateLimiter.tryAcquire()) {
            UIUpdateTask task = pendingUpdates.poll();
            if (task != null) {
                mainHandler.post(task.update);
            }
        }
    }

    private double getOptimalRateForDevice() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);

        if (memInfo.totalMem < 2_000_000_000L) { // Less than 2GB
            return 10.0; // 10 updates per second
        } else if (memInfo.totalMem < 4_000_000_000L) { // Less than 4GB
            return 20.0; // 20 updates per second
        } else {
            return 30.0; // 30 updates per second
        }
    }

    private static class UIUpdateTask {
        final Runnable update;
        final int priority;
        final long timestamp;

        UIUpdateTask(Runnable update, int priority, long timestamp) {
            this.update = update;
            this.priority = priority;
            this.timestamp = timestamp;
        }
    }
}
