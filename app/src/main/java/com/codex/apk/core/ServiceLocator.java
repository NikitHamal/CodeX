package com.codex.apk.core;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ultra-simple Service Locator for app-wide singletons.
 * Avoids external DI libs and keeps wiring explicit.
 */
public final class ServiceLocator {
    private static volatile boolean initialized = false;
    private static Context appContext;

    private static ExecutorService ioExecutor;

    private ServiceLocator() {}

    public static synchronized void init(Context context) {
        if (initialized) return;
        appContext = context.getApplicationContext();
        ioExecutor = Executors.newCachedThreadPool();
        initialized = true;
    }

    public static Context appContext() {
        if (appContext == null) throw new IllegalStateException("ServiceLocator not initialized");
        return appContext;
    }

    public static ExecutorService io() {
        if (ioExecutor == null) throw new IllegalStateException("ServiceLocator not initialized");
        return ioExecutor;
    }
}
