package com.codex.apk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "PreviewActivity";

    // Intent extras
    public static final String EXTRA_PROJECT_PATH = "project_path";
    public static final String EXTRA_PROJECT_NAME = "project_name";
    public static final String EXTRA_HTML_CONTENT = "html_content";
    public static final String EXTRA_FILE_NAME = "file_name";

    // UI Components
    private WebView webViewPreview;
    private TextView textConsoleOutput;
    private ScrollView scrollViewConsole;
    private LinearLayout consoleContainer;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    // Data
    private File projectDir;
    private String projectPath;
    private String projectName;
    private String htmlContent;
    private String fileName;
    private boolean isConsoleVisible = false;

    // Performance optimizations
    private Map<String, byte[]> fileCache = new HashMap<>();
    private static final long MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB cache limit
    private long currentCacheSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.activity_preview);

        // Get intent data
        extractIntentData();

        // Initialize views
        initializeViews();

        // Setup toolbar
        setupToolbar();

        // Setup WebView with full capabilities
        setupWebView();


        // Load initial content
        loadContent();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH);
        projectName = intent.getStringExtra(EXTRA_PROJECT_NAME);
        htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT);
        fileName = intent.getStringExtra(EXTRA_FILE_NAME);

        if (projectPath != null) {
            projectDir = new File(projectPath);
        }

        if (projectName == null) {
            projectName = "Preview";
        }

        if (fileName == null) {
            fileName = "index.html";
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        webViewPreview = findViewById(R.id.webview_preview);
        textConsoleOutput = findViewById(R.id.text_console_output);
        scrollViewConsole = findViewById(R.id.scrollview_console);
        consoleContainer = findViewById(R.id.console_container);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(projectName + " - Preview");
        }
    }

    private void setupWebView() {
        // Get WebView settings
        WebSettings webSettings = webViewPreview.getSettings();

        // Enable all JavaScript capabilities
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Enable DOM storage
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Enable local storage
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Performance optimizations
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Note: setAppCacheEnabled is deprecated in API 33+, but we keep it for backward compatibility
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);

        // Media and rendering optimizations
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Performance settings for low-end devices
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // Disable zoom controls but allow pinch zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // User agent for better compatibility
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " CodeX/1.0");

        // Setup WebViewClient with local file serving
        webViewPreview.setWebViewClient(new OptimizedWebViewClient());

        // Setup WebChromeClient for console output and progress
        webViewPreview.setWebChromeClient(new OptimizedWebChromeClient());

        // Enable hardware acceleration for better performance
        webViewPreview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }


    private void loadContent() {
        if (htmlContent != null && !htmlContent.trim().isEmpty()) {
            String enhancedHtml = enhanceHtmlContent(htmlContent);
            webViewPreview.loadDataWithBaseURL(
                "file://" + projectDir.getAbsolutePath() + "/",
                enhancedHtml,
                "text/html",
                "UTF-8",
                null
            );
        } else if (projectDir != null) {
            // Try to load index.html or the specified file
            File htmlFile = new File(projectDir, fileName);
            if (!htmlFile.exists()) {
                htmlFile = new File(projectDir, "index.html");
            }

            if (htmlFile.exists()) {
                String fileUrl = "file://" + htmlFile.getAbsolutePath();
                webViewPreview.loadUrl(fileUrl);
            } else {
                // Load a default HTML template
                loadDefaultTemplate();
            }
        } else {
            loadDefaultTemplate();
        }
    }

    private String enhanceHtmlContent(String originalHtml) {
        // Enhanced version of the HTML processing from PreviewConsoleFragment
        String htmlLower = originalHtml.toLowerCase(Locale.ROOT);
        StringBuilder resultHtml = new StringBuilder();

        // Ensure DOCTYPE and HTML structure
        if (!htmlLower.contains("<!doctype")) {
            resultHtml.append("<!DOCTYPE html>\n");
        }

        if (!htmlLower.contains("<html")) {
            resultHtml.append("<html lang=\"en\">\n");
        }

        // Enhanced head section with performance optimizations
        if (!htmlLower.contains("<head")) {
            resultHtml.append("<head>\n");
            resultHtml.append("<meta charset=\"UTF-8\">\n");
            resultHtml.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">\n");
            resultHtml.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");

            // Performance hints
            resultHtml.append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
            resultHtml.append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n");
            resultHtml.append("<link rel=\"preconnect\" href=\"https://cdn.tailwindcss.com\">\n");

            // Default fonts and styles
            resultHtml.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n");
            resultHtml.append("<script src=\"https://cdn.tailwindcss.com\"></script>\n");

            // Performance and development enhancements
            resultHtml.append("<style>\n");
            resultHtml.append("* { box-sizing: border-box; }\n");
            resultHtml.append("body { font-family: 'Inter', sans-serif; margin: 0; padding: 0; }\n");
            resultHtml.append("img { max-width: 100%; height: auto; }\n");
            resultHtml.append("</style>\n");

            resultHtml.append("</head>\n");
        }

        if (!htmlLower.contains("<body")) {
            resultHtml.append("<body>\n");
        }

        resultHtml.append(originalHtml);

        // Close tags if needed
        if (!htmlLower.contains("</body>")) {
            resultHtml.append("\n</body>");
        }
        if (!htmlLower.contains("</html>")) {
            resultHtml.append("\n</html>");
        }

        return resultHtml.toString();
    }

    private void loadDefaultTemplate() {
        String defaultHtml = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>" + projectName + "</title>\n" +
            "    <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
            "    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap\" rel=\"stylesheet\">\n" +
            "    <style>body { font-family: 'Inter', sans-serif; }</style>\n" +
            "</head>\n" +
            "<body class=\"bg-gray-50 p-8\">\n" +
            "    <div class=\"max-w-4xl mx-auto\">\n" +
            "        <h1 class=\"text-3xl font-bold text-gray-900 mb-4\">Welcome to " + projectName + "</h1>\n" +
            "        <p class=\"text-gray-600 mb-6\">Start editing your HTML files to see the preview here.</p>\n" +
            "        <div class=\"bg-white p-6 rounded-lg shadow-sm border\">\n" +
            "            <h2 class=\"text-xl font-semibold mb-3\">Features:</h2>\n" +
            "            <ul class=\"space-y-2 text-gray-600\">\n" +
            "                <li>• Full HTML5, CSS3, and JavaScript support</li>\n" +
            "                <li>• Responsive design testing</li>\n" +
            "                <li>• Real-time console output</li>\n" +
            "                <li>• Performance optimized for all devices</li>\n" +
            "                <li>• No security restrictions for local development</li>\n" +
            "            </ul>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";

        webViewPreview.loadDataWithBaseURL(
            "file://" + (projectDir != null ? projectDir.getAbsolutePath() : "") + "/",
            defaultHtml,
            "text/html",
            "UTF-8",
            null
        );
    }

    private void toggleConsole() {
        isConsoleVisible = !isConsoleVisible;
        consoleContainer.setVisibility(isConsoleVisible ? View.VISIBLE : View.GONE);
        fabToggleConsole.setImageResource(isConsoleVisible ?
            R.drawable.icon_expand_less_round : R.drawable.icon_expand_more_round);
    }

    private void refreshPreview() {
        clearConsole();
        if (webViewPreview != null) {
            webViewPreview.reload();
        }
    }

    private void clearConsole() {
        if (textConsoleOutput != null) {
            textConsoleOutput.setText("");
            addConsoleMessage("Console cleared.");
        }
    }

    private void addConsoleMessage(String message) {
        if (textConsoleOutput != null) {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            textConsoleOutput.append("[" + timestamp + "] " + message + "\n");
            scrollViewConsole.post(() -> scrollViewConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshPreview();
            return true;
        } else if (id == R.id.action_toggle_console) {
            toggleConsole();
            return true;
        } else if (id == R.id.action_clear_console) {
            clearConsole();
            return true;
        } else if (id == R.id.action_view_source) {
            viewPageSource();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void viewPageSource() {
        if (webViewPreview != null) {
            webViewPreview.evaluateJavascript(
                "document.documentElement.outerHTML",
                value -> {
                    // Create a new activity or dialog to show source
                    addConsoleMessage("Page source logged to console");
                }
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webViewPreview != null) {
            webViewPreview.removeAllViews();
            webViewPreview.destroy();
        }
        // Clear cache
        fileCache.clear();
    }

    @Override
    public void onBackPressed() {
        if (webViewPreview != null && webViewPreview.canGoBack()) {
            webViewPreview.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Optimized WebViewClient for better performance
    private class OptimizedWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            // Handle local file URLs
            if (url.startsWith("file://")) {
                return false; // Let WebView handle it
            }

            // Handle external URLs
            if (url.startsWith("http://") || url.startsWith("https://")) {
                // For now, allow external URLs for CDNs and resources
                return false;
            }

            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            // Cache local files for better performance
            if (url.startsWith("file://") && projectDir != null) {
                try {
                    String filePath = url.replace("file://", "");
                    File file = new File(filePath);

                    if (file.exists() && file.isFile()) {
                        // Check cache first
                        byte[] cachedData = fileCache.get(filePath);
                        if (cachedData != null) {
                            String mimeType = getMimeType(filePath);
                            return new WebResourceResponse(mimeType, "UTF-8",
                                new java.io.ByteArrayInputStream(cachedData));
                        }

                        // Read and cache file if cache size allows
                        if (file.length() < 5 * 1024 * 1024 && // Max 5MB per file
                            currentCacheSize + file.length() < MAX_CACHE_SIZE) {

                            FileInputStream fis = new FileInputStream(file);
                            byte[] data = new byte[(int) file.length()];
                            fis.read(data);
                            fis.close();

                            fileCache.put(filePath, data);
                            currentCacheSize += data.length;

                            String mimeType = getMimeType(filePath);
                            return new WebResourceResponse(mimeType, "UTF-8",
                                new java.io.ByteArrayInputStream(data));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error intercepting request: " + url, e);
                }
            }

            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            addConsoleMessage("Loading: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            addConsoleMessage("Loaded: " + url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            addConsoleMessage("Error loading " + failingUrl + ": " + description);
        }
    }

    // Optimized WebChromeClient
    private class OptimizedWebChromeClient extends WebChromeClient {

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            String message = String.format("%s: %s (Line %d)",
                consoleMessage.messageLevel().name(),
                consoleMessage.message(),
                consoleMessage.lineNumber());

            addConsoleMessage(message);
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }

        // Enable file upload capability
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                       FileChooserParams fileChooserParams) {
            // For a local development app, we can implement file picker if needed
            // For now, just return false to use default behavior
            return false;
        }
    }

    private String getMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            case "woff":
                return "font/woff";
            case "woff2":
                return "font/woff2";
            case "ttf":
                return "font/ttf";
            case "eot":
                return "application/vnd.ms-fontobject";
            default:
                return "text/plain";
        }
    }
}