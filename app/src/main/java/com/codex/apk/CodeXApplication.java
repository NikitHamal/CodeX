package com.codex.apk;

import android.app.Application;

/**
 * Custom Application class for CodeX
 * Handles app-wide initialization including theme setup
 */
public class CodeXApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up theme based on user preferences at app startup
        ThemeManager.setupTheme(this);
    }
}