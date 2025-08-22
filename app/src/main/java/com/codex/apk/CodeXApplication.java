package com.codex.apk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import java.io.PrintWriter;
import java.io.StringWriter;
import com.codex.apk.core.ServiceLocator;

/**
 * Custom Application class for CodeX
 * Handles app-wide initialization including theme setup and crash handling
 */
public class CodeXApplication extends Application {

    private static CodeXApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Set up theme based on user preferences at app startup
        ThemeManager.setupTheme(this);

        // Initialize service locator for global services
        ServiceLocator.init(this);

        // Set up crash handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(e);
            }
        });
    }

    public static Context getAppContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }

    private void handleUncaughtException(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        Intent intent = new Intent(this, DebugActivity.class);
        intent.putExtra("crash_log", stackTrace);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        System.exit(1);
    }
}