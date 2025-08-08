package com.codex.apk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.textmate.TextMateLanguage;
import io.github.rosemoe.sora.lang.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.lang.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.lang.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.lang.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.lang.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.lang.textmate.registry.model.IThemeSource;
import io.github.rosemoe.sora.lang.textmate.registry.provider.AssetsFileResolver;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplified TabAdapter using Sora Editor for high-performance code editing.
 * Uses built-in language support without requiring TextMate assets.
 */
public class SimpleSoraTabAdapter extends RecyclerView.Adapter<SimpleSoraTabAdapter.ViewHolder> {
    private static final String TAG = "SimpleSoraTabAdapter";
    private final Context context;
    private final List<TabItem> openTabs;
    private final TabActionListener tabActionListener;
    private final FileManager fileManager;


    // Current active tab position
    private int activeTabPosition = 0;

    /**
     * Interface for actions related to tabs that need to be handled by the parent (e.g., Fragment/Activity)
     */
    public interface TabActionListener {
        void onTabModifiedStateChanged();
        void onActiveTabChanged(File newFile);
    }

    /**
     * ViewHolder for the Sora Editor
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CodeEditor codeEditor;
        public boolean isListenerAttached = false;
        public String currentTabId = null;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            codeEditor = itemView.findViewById(R.id.code_editor);
        }
    }

    /**
     * Constructor for SimpleSoraTabAdapter
     */
    private boolean isTextMateInitialized = false;

    public SimpleSoraTabAdapter(Context context, List<TabItem> openTabs, TabActionListener tabActionListener, FileManager fileManager) {
        this.context = context;
        this.openTabs = openTabs;
        this.tabActionListener = tabActionListener;
        this.fileManager = fileManager;
        ensureTextmateInitialized();
    }

    private void ensureTextmateInitialized() {
        if (isTextMateInitialized) {
            return;
        }
        try {
            // Add a file provider to resolve assets
            FileProviderRegistry.getInstance().addFileProvider(new AssetsFileResolver(context.getAssets()));

            // Load a theme
            String themeName = "darcula";
            String themePath = "textmate/themes/" + themeName + ".json";
            ThemeRegistry.getInstance().loadTheme(new ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(themePath), themePath, null
                ), themeName
            ));

            // Set the theme
            ThemeRegistry.getInstance().setTheme(themeName);

            // Load grammars
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");

            isTextMateInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TextMate resources", e);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_editor_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= openTabs.size()) {
            Log.e(TAG, "Position " + position + " is out of bounds for openTabs size " + openTabs.size());
            return;
        }

        TabItem tabItem = openTabs.get(position);
        String tabId = tabItem.getFile().getAbsolutePath();
        CodeEditor codeEditor = holder.codeEditor;

        // Only reconfigure if this is a different tab
        if (!tabId.equals(holder.currentTabId)) {
            holder.currentTabId = tabId;

            // Configure the editor only for new tabs
            configureEditor(codeEditor, tabItem);

            // Set content without triggering change events
            codeEditor.setText(tabItem.getContent());

            // Set up content change listener only once per tab
            if (!holder.isListenerAttached) {
                codeEditor.subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent.class, (event, unsubscribe) -> {
                    // Get the current tab item for this holder
                    int currentPos = holder.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION && currentPos < openTabs.size()) {
                        TabItem currentTabItem = openTabs.get(currentPos);
                        String newContent = codeEditor.getText().toString();

                        // Only update if content actually changed
                        if (!currentTabItem.getContent().equals(newContent)) {
                            // Update content immediately for responsive typing
                            currentTabItem.setContent(newContent);
                            currentTabItem.setModified(true);
                            if (tabActionListener != null) {
                                tabActionListener.onTabModifiedStateChanged();
                            }
                        }
                    }
                });
                holder.isListenerAttached = true;
            }
        }

        // Apply active tab styling if this is the active tab
        if (position == activeTabPosition) {
            codeEditor.requestFocus();
            if (tabActionListener != null) {
                tabActionListener.onActiveTabChanged(tabItem.getFile());
            }
        }
    }

    /**
     * Configure the Sora Editor with appropriate language and theme
     */
    private void configureEditor(CodeEditor codeEditor, TabItem tabItem) {
        String fileName = tabItem.getFileName();
        String extension = getFileExtension(fileName);

        // Set language and theme
        try {
            String scopeName = getScopeNameForExtension(extension);
            if (scopeName != null) {
                Language language = TextMateLanguage.create(scopeName, true);
                codeEditor.setEditorLanguage(language);
                codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
            } else {
                codeEditor.setEditorLanguage(new io.github.rosemoe.sora.lang.EmptyLanguage());
                codeEditor.setColorScheme(new EditorColorScheme()); // Fallback to default
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to set TextMate language for " + fileName, e);
            codeEditor.setEditorLanguage(new io.github.rosemoe.sora.lang.EmptyLanguage());
            codeEditor.setColorScheme(new EditorColorScheme());
        }

        // Configure editor appearance
        codeEditor.setTextSize(14f);
        codeEditor.setLineNumberEnabled(true);
        codeEditor.setWordwrap(false);
        codeEditor.setHighlightCurrentBlock(false);
        codeEditor.setHighlightCurrentLine(false);
        codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE);

        // Enable features for better editing experience
        codeEditor.setScrollBarEnabled(false);
        codeEditor.setVerticalScrollBarEnabled(false);
        codeEditor.setHorizontalScrollBarEnabled(false);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private String getScopeNameForExtension(String extension) {
        switch (extension) {
            case "java":
                return "source.java";
            case "kt":
                return "source.kotlin";
            case "html":
            case "htm":
                return "text.html.basic";
            case "css":
                return "source.css";
            case "js":
                return "source.js";
            case "json":
                return "source.json";
            case "xml":
                return "text.xml";
            case "md":
                return "text.html.markdown";
            default:
                return null;
        }
    }


    @Override
    public int getItemCount() {
        return openTabs.size();
    }

    /**
     * Set the active tab position and notify changes to update UI.
     */
    public void setActiveTab(int position) {
        if (position >= 0 && position < openTabs.size()) {
            int oldPosition = activeTabPosition;
            activeTabPosition = position;

            // Notify both old and new positions for a more precise update
            notifyItemChanged(oldPosition);
            notifyItemChanged(activeTabPosition);

            if (tabActionListener != null) {
                tabActionListener.onActiveTabChanged(openTabs.get(position).getFile());
            }
        }
    }

    /**
     * Get the active tab position
     */
    public int getActiveTabPosition() {
        return activeTabPosition;
    }

    /**
     * Get the active tab item
     */
    public TabItem getActiveTabItem() {
        if (activeTabPosition >= 0 && activeTabPosition < openTabs.size()) {
            return openTabs.get(activeTabPosition);
        }
        return null;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        // No-op
    }
}