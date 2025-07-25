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

        if (syntaxType == OptimizedSyntaxHighlighter.SyntaxType.HTML) {
            if (isDarkTheme) {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary_light);       // Tags
                syntaxColors[1] = ContextCompat.getColor(context, R.color.tertiary_container);  // Attributes
                syntaxColors[2] = ContextCompat.getColor(context, R.color.secondary_container); // Values
                syntaxColors[3] = ContextCompat.getColor(context, R.color.outline);             // Comments
                syntaxColors[4] = ContextCompat.getColor(context, R.color.warning_container);   // Doctype
                syntaxColors[5] = ContextCompat.getColor(context, R.color.success_container);   // Entities
            } else {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary);             // Tags
                syntaxColors[1] = ContextCompat.getColor(context, R.color.tertiary);            // Attributes
                syntaxColors[2] = ContextCompat.getColor(context, R.color.secondary);           // Values
                syntaxColors[3] = ContextCompat.getColor(context, R.color.outline_variant);     // Comments
                syntaxColors[4] = ContextCompat.getColor(context, R.color.warning);             // Doctype
                syntaxColors[5] = ContextCompat.getColor(context, R.color.success);             // Entities
            }
        } else if (syntaxType == OptimizedSyntaxHighlighter.SyntaxType.CSS) {
            if (isDarkTheme) {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary_light);       // Selectors
                syntaxColors[1] = ContextCompat.getColor(context, R.color.tertiary_container);  // Properties
                syntaxColors[2] = ContextCompat.getColor(context, R.color.secondary_container); // Values
                syntaxColors[3] = ContextCompat.getColor(context, R.color.outline);             // Comments
                syntaxColors[4] = ContextCompat.getColor(context, R.color.error_container);     // Important
                syntaxColors[5] = ContextCompat.getColor(context, R.color.success_container);   // Color values
                syntaxColors[6] = ContextCompat.getColor(context, R.color.warning_container);   // Units
                syntaxColors[7] = ContextCompat.getColor(context, R.color.primary_container);   // Media queries / At-rules
                syntaxColors[8] = ContextCompat.getColor(context, R.color.secondary_container); // Pseudo-classes/elements
                syntaxColors[9] = ContextCompat.getColor(context, R.color.info_container);      // Functions (e.g., url(), calc())
            } else {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary);             // Selectors
                syntaxColors[1] = ContextCompat.getColor(context, R.color.tertiary);            // Properties
                syntaxColors[2] = ContextCompat.getColor(context, R.color.secondary);           // Values
                syntaxColors[3] = ContextCompat.getColor(context, R.color.outline_variant);     // Comments
                syntaxColors[4] = ContextCompat.getColor(context, R.color.error);               // Important
                syntaxColors[5] = ContextCompat.getColor(context, R.color.success);             // Color values
                syntaxColors[6] = ContextCompat.getColor(context, R.color.warning);             // Units
                syntaxColors[7] = ContextCompat.getColor(context, R.color.primary);             // Media queries / At-rules
                syntaxColors[8] = ContextCompat.getColor(context, R.color.secondary);           // Pseudo-classes/elements
                syntaxColors[9] = ContextCompat.getColor(context, R.color.info);                // Functions
            }
        } else if (syntaxType == OptimizedSyntaxHighlighter.SyntaxType.JAVASCRIPT) {
            if (isDarkTheme) {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary_light);       // Keywords
                syntaxColors[1] = ContextCompat.getColor(context, R.color.secondary_container); // Built-ins
                syntaxColors[2] = ContextCompat.getColor(context, R.color.tertiary_container);  // Strings
                syntaxColors[3] = ContextCompat.getColor(context, R.color.success_container);   // Numbers
                syntaxColors[4] = ContextCompat.getColor(context, R.color.warning_container);   // Functions
                syntaxColors[5] = ContextCompat.getColor(context, R.color.outline);             // Comments
                syntaxColors[6] = ContextCompat.getColor(context, R.color.tertiary_container);  // Template strings
                syntaxColors[7] = ContextCompat.getColor(context, R.color.error_container);     // Regex
            } else {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary);             // Keywords
                syntaxColors[1] = ContextCompat.getColor(context, R.color.secondary);           // Built-ins
                syntaxColors[2] = ContextCompat.getColor(context, R.color.tertiary);            // Strings
                syntaxColors[3] = ContextCompat.getColor(context, R.color.success);             // Numbers
                syntaxColors[4] = ContextCompat.getColor(context, R.color.warning);             // Functions
                syntaxColors[5] = ContextCompat.getColor(context, R.color.outline_variant);     // Comments
                syntaxColors[6] = ContextCompat.getColor(context, R.color.tertiary);            // Template strings
                syntaxColors[7] = ContextCompat.getColor(context, R.color.error);               // Regex
            }
        } else if (syntaxType == OptimizedSyntaxHighlighter.SyntaxType.JSON) {
            if (isDarkTheme) {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary_light);       // Keys
                syntaxColors[1] = ContextCompat.getColor(context, R.color.tertiary_container);  // Strings
                syntaxColors[2] = ContextCompat.getColor(context, R.color.success_container);   // Numbers
                syntaxColors[3] = ContextCompat.getColor(context, R.color.warning_container);   // Booleans
                syntaxColors[4] = ContextCompat.getColor(context, R.color.outline);             // Null
            } else {
                syntaxColors[0] = ContextCompat.getColor(context, R.color.primary);             // Keys
                syntaxColors[1] = ContextCompat.getColor(context, R.color.tertiary);            // Strings
                syntaxColors[2] = ContextCompat.getColor(context, R.color.success);             // Numbers
                syntaxColors[3] = ContextCompat.getColor(context, R.color.warning);             // Booleans
                syntaxColors[4] = ContextCompat.getColor(context, R.color.outline_variant);     // Null
            }
        }
        // Removed color initialization for JAVA, PYTHON, XML, MARKDOWN, PLAIN
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
