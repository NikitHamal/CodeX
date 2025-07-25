package com.codex.apk;

import android.content.Context;
import android.net.Uri;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreviewConsoleFragment extends Fragment {

    private static final String TAG = "PreviewConsoleFragment";
    private WebView webViewPreview;
    private TextView textConsoleOutput;
    private ScrollView scrollViewConsole;
    private LinearLayout consoleContainer;

    private File projectDir;

    private PreviewConsoleFragmentListener listener;

    private boolean isConsoleVisible = false;

    // Default CDN links to ensure common web dev libraries are available
    // These will be injected if not already present in the HTML.
    private static final String TAILWIND_CDN = "<script src=\"https://cdn.tailwindcss.com\"></script>";
    private static final String INTER_FONT_LINK = "<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap\" rel=\"stylesheet\">";
    private static final String DEFAULT_FONT_STYLE = "<style>body { font-family: 'Inter', sans-serif; }</style>";
    // Add other common CDNs if desired, or assume user will include them in their HTML.
    // For now, we stick to Inter for default text style and Tailwind for CSS utility.


    public interface PreviewConsoleFragmentListener {
        String getActiveFileContent();
        String getActiveFileName();
        File getProjectDirectory();
    }

    public static PreviewConsoleFragment newInstance(File projectDir) {
        PreviewConsoleFragment fragment = new PreviewConsoleFragment();
        Bundle args = new Bundle();
        args.putSerializable("projectDir", projectDir);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
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
        View view = inflater.inflate(R.layout.layout_preview_console_tab, container, false);

        webViewPreview = view.findViewById(R.id.webview_preview);
        textConsoleOutput = view.findViewById(R.id.text_console_output);
        scrollViewConsole = view.findViewById(R.id.scrollview_console);
        consoleContainer = view.findViewById(R.id.console_container);

        setupWebView();
        clearConsole();

        consoleContainer.setVisibility(View.GONE);
        isConsoleVisible = false;

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (listener != null) {
            if (projectDir == null) {
                projectDir = listener.getProjectDirectory();
            }
            String content = listener.getActiveFileContent();
            String fileName = listener.getActiveFileName();
            updatePreview(content, fileName);
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webViewPreview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        // CRITICAL: Allow loading of content from http/https sources even if the main page is local or vice-versa
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);


        webViewPreview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Load all URLs within this WebView itself
                view.loadUrl(url);
                return true; // Indicate that we are handling the URL loading
            }
        });

        webViewPreview.setWebChromeClient(new WebChromeClient() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String timestamp = dateFormat.format(new Date());
                String message = String.format("[%s] %s: %s (Line: %d, Source: %s)",
                        timestamp,
                        consoleMessage.messageLevel().name(),
                        consoleMessage.message(),
                        consoleMessage.lineNumber(),
                        consoleMessage.sourceId());
                addConsoleMessage(message);
                return true;
            }
        });
    }

    public void updatePreview(String htmlContent, String fileName) {
        if (webViewPreview == null) {
            Log.e(TAG, "WebView is null, cannot update preview.");
            return;
        }

        if (projectDir == null && listener != null) {
            projectDir = listener.getProjectDirectory();
        }

        if (projectDir == null) {
            Log.e(TAG, "Project directory is null, cannot load preview with relative paths.");
            String placeholderHtml = "<html><body style='background-color: #f0f0f0; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; font-family: sans-serif; color: #333; text-align: center;'>" +
                                     "<h2 style='color: #666;'>Preview Error</h2>" +
                                     "<p>Project directory not found. Cannot load assets.</p>" +
                                     "</body></html>";
            webViewPreview.loadDataWithBaseURL(null, placeholderHtml, "text/html", "UTF-8", null);
            clearConsole();
            addConsoleMessage("Error: Project directory not set for preview.");
            return;
        }

        String baseUrl = Uri.fromFile(projectDir).toString() + "/";

        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".html")) {
            // Ensure the HTML has a <head> tag to inject into, or wrap it
            String processedHtmlContent = ensureHtmlStructureAndInjectCDNs(htmlContent);

            webViewPreview.loadDataWithBaseURL(baseUrl, processedHtmlContent, "text/html", "UTF-8", null);
            clearConsole();
            addConsoleMessage("Previewing: " + fileName + " (Default CDNs and font enabled)");
        } else {
            String placeholderHtml = "<html><body style='background-color: #f0f0f0; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; font-family: sans-serif; color: #333; text-align: center;'>" +
                                     "<h2 style='color: #666;'>Preview Not Available</h2>" +
                                     "<p>This file type (" + (fileName != null ? fileName : "unknown") + ") cannot be directly previewed here.</p>" +
                                     "<p>Open an HTML file to see a live preview.</p>" +
                                     "</body></html>";
            webViewPreview.loadDataWithBaseURL(null, placeholderHtml, "text/html", "UTF-8", null);
            clearConsole();
            addConsoleMessage("Preview not available for " + (fileName != null ? fileName : "this file type") + ".");
        }
    }

    /**
     * Ensures basic HTML structure (<html>, <head>, <body>) and injects default CDN links
     * for Tailwind CSS and Inter font if they are not already present.
     * This makes the preview more robust for simple HTML snippets.
     * User-provided CDNs for other libraries (like Material Symbols, Feather Icons, Plus Jakarta Sans)
     * should be placed directly in their HTML and will be loaded by the WebView.
     *
     * @param originalHtml The original HTML content from the editor.
     * @return HTML content with basic structure and default CDNs injected.
     */
    private String ensureHtmlStructureAndInjectCDNs(String originalHtml) {
        String htmlLower = originalHtml.toLowerCase(Locale.ROOT);
        StringBuilder resultHtml = new StringBuilder(originalHtml);

        // Check if <html> tag exists
        if (htmlLower.indexOf("<html>") == -1) {
            resultHtml.insert(0, "<!DOCTYPE html>\n<html>\n");
            resultHtml.append("\n</html>");
            htmlLower = resultHtml.toString().toLowerCase(Locale.ROOT); // Update for further checks
        }

        // Check if <head> tag exists, if not, insert one after <html>
        int headOpenIndex = htmlLower.indexOf("<head>");
        if (headOpenIndex == -1) {
            int htmlOpenIndex = htmlLower.indexOf("<html>");
            if (htmlOpenIndex != -1) {
                resultHtml.insert(htmlOpenIndex + "<html>".length(), "\n<head>\n</head>");
                htmlLower = resultHtml.toString().toLowerCase(Locale.ROOT); // Update for further checks
            } else {
                // Fallback: This should ideally not happen after the <html> check, but for safety
                Log.w(TAG, "No <html> or <head> found, prepending basic structure.");
                resultHtml.insert(0, "<!DOCTYPE html><html><head></head><body>");
                resultHtml.append("</body></html>");
                htmlLower = resultHtml.toString().toLowerCase(Locale.ROOT);
            }
        }

        // Now, inject CDNs into the <head> if they are not already there
        int headCloseIndex = htmlLower.indexOf("</head>");
        if (headCloseIndex != -1) {
            StringBuilder headContentToInject = new StringBuilder();

            if (!htmlLower.contains(TAILWIND_CDN.toLowerCase(Locale.ROOT).replace("https:", ""))) { // Check without protocol for robustness
                headContentToInject.append(TAILWIND_CDN).append("\n");
            }
            if (!htmlLower.contains(INTER_FONT_LINK.toLowerCase(Locale.ROOT).replace("https:", ""))) {
                headContentToInject.append(INTER_FONT_LINK).append("\n");
            }
            if (!htmlLower.contains(DEFAULT_FONT_STYLE.toLowerCase(Locale.ROOT))) {
                headContentToInject.append(DEFAULT_FONT_STYLE).append("\n");
            }

            // Insert content just before </head>
            if (headContentToInject.length() > 0) {
                int originalHeadCloseIndex = originalHtml.toLowerCase(Locale.ROOT).indexOf("</head>");
                if (originalHeadCloseIndex != -1) {
                    // Inject into the original HTML string to avoid issues with StringBuilder's changing indices
                    originalHtml = originalHtml.substring(0, originalHeadCloseIndex) +
                                   headContentToInject.toString() +
                                   originalHtml.substring(originalHeadCloseIndex);
                } else {
                    // Fallback if </head> was just added by us, find it in the current string builder
                    // This case is less ideal as it means multiple string operations
                    String currentResult = resultHtml.toString();
                    int newHeadCloseIndex = currentResult.toLowerCase(Locale.ROOT).indexOf("</head>");
                    if (newHeadCloseIndex != -1) {
                         resultHtml.insert(newHeadCloseIndex, headContentToInject.toString());
                         originalHtml = resultHtml.toString(); // Use the resultHtml for the final output
                    } else {
                         Log.e(TAG, "Failed to find </head> for CDN injection after initial structure setup.");
                    }
                }
            }
        }
        return originalHtml; // Return the potentially modified originalHtml
    }


    public void addConsoleMessage(String message) {
        if (textConsoleOutput != null) {
            textConsoleOutput.append(message + "\n");
            scrollViewConsole.post(() -> scrollViewConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    public void clearConsole() {
        if (textConsoleOutput != null) {
            textConsoleOutput.setText("");
            addConsoleMessage("Console cleared.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webViewPreview != null) {
            webViewPreview.removeAllViews();
            webViewPreview.destroy();
            webViewPreview = null;
        }
    }
}
