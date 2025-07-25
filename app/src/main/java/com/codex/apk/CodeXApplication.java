package com.codex.apk;

import android.app.Application;
import android.content.Intent;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Custom Application class for CodeX
 * Handles app-wide initialization including theme setup and crash handling
 */
public class CodeXApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up theme based on user preferences at app startup
        ThemeManager.setupTheme(this);

        // Set up crash handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(e);
            }
        });
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