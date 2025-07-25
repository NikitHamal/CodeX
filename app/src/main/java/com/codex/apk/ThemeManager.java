package com.codex.apk;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * ThemeManager handles dynamic theme switching without requiring app restart.
 */
public class ThemeManager {
    
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";
    
    /**
     * Applies the selected theme to the app.
     * @param context The application context
     * @param theme The theme to apply ("light", "dark", "system")
     */
    public static void applyTheme(Context context, String theme) {
        switch (theme) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    /**
     * Gets the current theme from settings.
     * @param context The application context
     * @return The current theme string
     */
    public static String getCurrentTheme(Context context) {
        return SettingsActivity.getAppTheme(context);
    }
    
    /**
     * Checks if the current theme is dark.
     * @param context The application context
     * @return True if dark theme is active
     */
    public static boolean isDarkTheme(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Sets up theme based on saved preferences.
     * Should be called in Application.onCreate() or Activity.onCreate()
     * @param context The application context
     */
    public static void setupTheme(Context context) {
        String savedTheme = getCurrentTheme(context);
        applyTheme(context, savedTheme);
    }
    
    /**
     * Switches theme and applies it immediately.
     * @param activity The current activity
     * @param newTheme The new theme to apply
     */
    public static void switchTheme(Activity activity, String newTheme) {
        // Save the new theme
        SettingsActivity.getPreferences(activity)
            .edit()
            .putString("app_theme", newTheme)
            .apply();
            
        // Apply the theme
        applyTheme(activity, newTheme);
        
        // Recreate the activity to apply theme changes
        activity.recreate();
    }
}