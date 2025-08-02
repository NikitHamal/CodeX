package com.codex.apk.CodeEditor;

import android.content.Context;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.codex.apk.R; // Ensure R is imported correctly for colors

/**
 * Provides syntax highlighting colors based on the current theme and syntax type.
 * Supports HTML, CSS, JavaScript, and JSON.
 */
public class SyntaxColorProvider {

    private final Context context;
    private boolean isDarkTheme;
    private int[] syntaxColors;
    private int diffAddedColor;
    private int diffDeletedColor;
    private int diffUnchangedColor;

    public SyntaxColorProvider(Context context) {
        this.context = context;
        this.isDarkTheme = isDarkTheme(context);
        initializeCommonColors();
    }

    /**
     * Initializes the syntax colors for a given SyntaxType and theme.
     * This method should be called whenever the syntax type or theme changes.
     * @param syntaxType The current syntax type.
     */
    public void initializeSyntaxColors(OptimizedSyntaxHighlighter.SyntaxType syntaxType) {
        syntaxColors = new int[15]; // Max number of colors needed for any syntax type
        int[] lightColors;
        int[] darkColors;

        switch (syntaxType) {
            case HTML:
                lightColors = new int[]{R.color.primary, R.color.tertiary, R.color.secondary, R.color.outline_variant, R.color.warning, R.color.success};
                darkColors = new int[]{R.color.primary_light, R.color.tertiary_container, R.color.secondary_container, R.color.outline, R.color.warning_container, R.color.success_container};
                loadColors(lightColors, darkColors);
                break;
            case CSS:
                lightColors = new int[]{R.color.primary, R.color.tertiary, R.color.secondary, R.color.outline_variant, R.color.error, R.color.success, R.color.warning, R.color.primary, R.color.secondary, R.color.info};
                darkColors = new int[]{R.color.primary_light, R.color.tertiary_container, R.color.secondary_container, R.color.outline, R.color.error_container, R.color.success_container, R.color.warning_container, R.color.primary_container, R.color.secondary_container, R.color.info_container};
                loadColors(lightColors, darkColors);
                break;
            case JAVASCRIPT:
                lightColors = new int[]{R.color.primary, R.color.secondary, R.color.tertiary, R.color.success, R.color.warning, R.color.outline_variant, R.color.tertiary, R.color.error};
                darkColors = new int[]{R.color.primary_light, R.color.secondary_container, R.color.tertiary_container, R.color.success_container, R.color.warning_container, R.color.outline, R.color.tertiary_container, R.color.error_container};
                loadColors(lightColors, darkColors);
                break;
            case JSON:
                lightColors = new int[]{R.color.primary, R.color.tertiary, R.color.success, R.color.warning, R.color.outline_variant};
                darkColors = new int[]{R.color.primary_light, R.color.tertiary_container, R.color.success_container, R.color.warning_container, R.color.outline};
                loadColors(lightColors, darkColors);
                break;
        }
    }

    private void loadColors(int[] lightColors, int[] darkColors) {
        int[] colorsToLoad = isDarkTheme ? darkColors : lightColors;
        for (int i = 0; i < colorsToLoad.length; i++) {
            syntaxColors[i] = ContextCompat.getColor(context, colorsToLoad[i]);
        }
    }

    /**
     * Initializes common colors like diff highlighting colors.
     */
    private void initializeCommonColors() {
        this.diffAddedColor = ContextCompat.getColor(context, R.color.color_border_diff_added);
        this.diffDeletedColor = ContextCompat.getColor(context, R.color.color_border_diff_deleted);
        this.diffUnchangedColor = ContextCompat.getColor(context, R.color.on_surface_variant);
    }

    /**
     * Checks if the current theme is dark.
     * @param context The context.
     * @return True if dark theme, false otherwise.
     */
    private boolean isDarkTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        // Resolve the isLightTheme attribute from the current theme.
        // If isLightTheme is false (0), it means it's a dark theme.
        context.getTheme().resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
        return typedValue.data == 0;
    }

    /**
     * Gets the array of syntax colors for the current configuration.
     * @return An array of integer color values.
     */
    public int[] getSyntaxColors() {
        return syntaxColors;
    }

    /**
     * Gets the color for added lines in diff.
     * @return The integer color value.
     */
    public int getDiffAddedColor() {
        return diffAddedColor;
    }

    /**
     * Gets the color for deleted lines in diff.
     * @return The integer color value.
     */
    public int getDiffDeletedColor() {
        return diffDeletedColor;
    }

    /**
     * Gets the color for unchanged lines in diff.
     * @return The integer color value.
     */
    public int getDiffUnchangedColor() {
        return diffUnchangedColor;
    }

    /**
     * Sets the dark theme status and re-initializes colors.
     * @param darkTheme True for dark theme, false for light theme.
     */
    public void setDarkTheme(boolean darkTheme) {
        if (this.isDarkTheme != darkTheme) {
            this.isDarkTheme = darkTheme;
            // Re-initialize all common colors (like diff colors) based on the new theme
            initializeCommonColors();
            // Note: The calling class (OptimizedSyntaxHighlighter) is responsible for calling
            // initializeSyntaxColors with the current syntax type after this.
        }
    }

    /**
     * Returns the context used by this color provider.
     * @return The Context instance.
     */
    public Context getContext() {
        return context;
    }
}
