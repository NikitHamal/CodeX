package com.codex.apk;

import android.content.Context;
import android.net.Uri; // Import Uri for file URIs
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout; // Import LinearLayout for the console container
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import FloatingActionButton

import java.io.File; // Import File
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreviewConsoleFragment extends Fragment {

    private static final String TAG = "PreviewConsoleFragment";
    private WebView webViewPreview;
    private TextView textConsoleOutput;
    private ScrollView scrollViewConsole;
    private LinearLayout consoleContainer; // New: Container for console output
    private FloatingActionButton fabToggleConsole; // New: FAB for toggling console visibility

    private File projectDir; // New: Member variable to hold the project directory

    // Listener to communicate with EditorActivity
    private PreviewConsoleFragmentListener listener;

    private boolean isConsoleVisible = false; // State to track console visibility

    /**
     * Interface for actions related to preview and console that need to be handled by the parent activity.
     */
    public interface PreviewConsoleFragmentListener {
        /**
         * Returns the content of the currently active file in the code editor.
         * @return The content as a String.
         */
        String getActiveFileContent();

        /**
         * Returns the name of the currently active file in the code editor.
         * @return The file name as a String.
         */
        String getActiveFileName();

        /**
         * Returns the root directory of the current project.
         * @return The project directory as a File object.
         */
        File getProjectDirectory(); // New method
    }

    /**
     * Factory method to create a new instance of this fragment.
     * @param projectDir The root directory of the current project.
     * @return A new instance of fragment PreviewConsoleFragment.
     */
    public static PreviewConsoleFragment newInstance(File projectDir) {
        PreviewConsoleFragment fragment = new PreviewConsoleFragment();
        Bundle args = new Bundle();
        args.putSerializable("projectDir", projectDir); // Pass File object as serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the hosting activity implements the listener interface
        if (context instanceof PreviewConsoleFragmentListener) {
            listener = (PreviewConsoleFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PreviewConsoleFragmentListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectDir = (File) getArguments().getSerializable("projectDir");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_preview_console_tab, container, false);

        // Initialize UI components from the inflated layout
        webViewPreview = view.findViewById(R.id.webview_preview);
        textConsoleOutput = view.findViewById(R.id.text_console_output);
        scrollViewConsole = view.findViewById(R.id.scrollview_console);
        consoleContainer = view.findViewById(R.id.console_container); // Initialize the console container
        fabToggleConsole = view.findViewById(R.id.fab_toggle_console); // Initialize the FAB

        // Set up WebView settings and console message capture
        setupWebView();
        clearConsole(); // Clear console on fragment creation

        // Set initial visibility of console container to GONE
        consoleContainer.setVisibility(View.GONE);
        isConsoleVisible = false;

        // Set up click listener for the toggle console FAB
        fabToggleConsole.setOnClickListener(v -> toggleConsoleVisibility());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initial load of the current active file content when the view is created
        if (listener != null) {
            // Ensure projectDir is set from arguments or listener
            if (projectDir == null) {
                projectDir = listener.getProjectDirectory();
            }
            String content = listener.getActiveFileContent();
            String fileName = listener.getActiveFileName();
            updatePreview(content, fileName);
        }
    }

    /**
     * Configures the WebView settings and sets up the WebChromeClient for console message capture.
     */
    private void setupWebView() {
        WebSettings webSettings = webViewPreview.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript for web content
        webSettings.setDomStorageEnabled(true); // Enable DOM storage for web apps
        webSettings.setAllowFileAccess(true); // Allow access to file system
        webSettings.setAllowContentAccess(true); // Allow access to content URIs
        webSettings.setAllowUniversalAccessFromFileURLs(true); // Needed for some local file access scenarios
        webSettings.setAllowFileAccessFromFileURLs(true);

        // Prevent opening external links in the default browser, keep them within this WebView
        webViewPreview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Load all URLs within this WebView
                return false;
            }
        });

        // Capture console messages (e.g., console.log, console.error) from the WebView
        webViewPreview.setWebChromeClient(new WebChromeClient() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                // Format the console message with timestamp, level, and source information
                String timestamp = dateFormat.format(new Date());
                String message = String.format("[%s] %s: %s (Line: %d, Source: %s)",
                        timestamp,
                        consoleMessage.messageLevel().name(), // e.g., LOG, ERROR, WARNING
                        consoleMessage.message(),
                        consoleMessage.lineNumber(),
                        consoleMessage.sourceId());
                addConsoleMessage(message); // Add the formatted message to our console TextView
                return true; // Indicate that we've handled the message
            }
        });
    }

    /**
     * Toggles the visibility of the console output.
     */
    private void toggleConsoleVisibility() {
        if (consoleContainer.getVisibility() == View.VISIBLE) {
            consoleContainer.setVisibility(View.GONE);
            isConsoleVisible = false;
        } else {
            consoleContainer.setVisibility(View.VISIBLE);
            isConsoleVisible = true;
            // Scroll to bottom when console is shown to display latest messages
            scrollViewConsole.post(() -> scrollViewConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    /**
     * Updates the WebView with the given HTML content.
     * If the content is not HTML (e.g., CSS, JS), it will display a placeholder message.
     * @param htmlContent The HTML content to load.
     * @param fileName The name of the file, used to determine if it's an HTML file.
     */
    public void updatePreview(String htmlContent, String fileName) {
        if (webViewPreview == null) {
            Log.e(TAG, "WebView is null, cannot update preview.");
            return;
        }

        // Ensure projectDir is available, try to get from listener if not set via arguments
        if (projectDir == null && listener != null) {
            projectDir = listener.getProjectDirectory();
        }

        if (projectDir == null) {
            Log.e(TAG, "Project directory is null, cannot load preview with relative paths.");
            // Fallback to a generic placeholder if projectDir is not available
            String placeholderHtml = "<html><body style='background-color: #f0f0f0; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; font-family: sans-serif; color: #333; text-align: center;'>" +
                                     "<h2 style='color: #666;'>Preview Error</h2>" +
                                     "<p>Project directory not found. Cannot load assets.</p>" +
                                     "</body></html>";
            webViewPreview.loadDataWithBaseURL(null, placeholderHtml, "text/html", "UTF-8", null);
            clearConsole();
            addConsoleMessage("Error: Project directory not set for preview.");
            return;
        }

        // Get the base URL for the project directory
        // Adding a trailing slash is crucial for WebView to correctly resolve relative paths
        String baseUrl = Uri.fromFile(projectDir).toString() + "/";

        // Check if the file is an HTML file based on its extension
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".html")) {
            // Load the HTML content with the project directory as the base URL
            webViewPreview.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null);
            clearConsole(); // Clear console when a new HTML preview is loaded
            addConsoleMessage("Previewing: " + fileName);
        } else {
            // For non-HTML files, display a placeholder message in the WebView
            String placeholderHtml = "<html><body style='background-color: #f0f0f0; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; font-family: sans-serif; color: #333; text-align: center;'>" +
                                     "<h2 style='color: #666;'>Preview Not Available</h2>" +
                                     "<p>This file type (" + (fileName != null ? fileName : "unknown") + ") cannot be directly previewed here.</p>" +
                                     "<p>Open an HTML file to see a live preview.</p>" +
                                     "</body></html>";
            webViewPreview.loadDataWithBaseURL(null, placeholderHtml, "text/html", "UTF-8", null);
            clearConsole(); // Clear console for non-HTML previews
            addConsoleMessage("Preview not available for " + (fileName != null ? fileName : "this file type") + ".");
        }
    }

    /**
     * Appends a message to the console output TextView and scrolls to the bottom.
     * @param message The message to add.
     */
    public void addConsoleMessage(String message) {
        if (textConsoleOutput != null) {
            textConsoleOutput.append(message + "\n");
            // Scroll to the bottom of the ScrollView to show the latest message
            scrollViewConsole.post(() -> scrollViewConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    /**
     * Clears all messages from the console output TextView.
     */
    public void clearConsole() {
        if (textConsoleOutput != null) {
            textConsoleOutput.setText("");
            addConsoleMessage("Console cleared.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Clear the listener to prevent memory leaks
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Destroy the WebView to prevent memory leaks and free up resources
        if (webViewPreview != null) {
            webViewPreview.removeAllViews();
            webViewPreview.destroy();
            webViewPreview = null;
        }
    }
}
