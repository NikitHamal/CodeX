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
import io.github.rosemoe.sora.lang.EmptyLanguage;
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

    // Auto-save functionality
    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> autoSaveRunnables = new HashMap<>();
    private static final int AUTO_SAVE_DELAY = 2000; // 2 seconds delay

    // Debounce functionality for content changes
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> debounceRunnables = new HashMap<>();
    private static final int DEBOUNCE_DELAY = 300; // 300ms delay

    // Current active tab position
    private int activeTabPosition = 0;

    /**
     * Interface for actions related to tabs that need to be handled by the parent (e.g., Fragment/Activity)
     */
    public interface TabActionListener {
        void onTabModifiedStateChanged();
        void onActiveTabContentChanged(String content, String fileName);
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
    public SimpleSoraTabAdapter(Context context, List<TabItem> openTabs, TabActionListener tabActionListener, FileManager fileManager) {
        this.context = context;
        this.openTabs = openTabs;
        this.tabActionListener = tabActionListener;
        this.fileManager = fileManager;
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

                            // Debounce the modified state update to prevent flickering
                            debounceModifiedStateUpdate(currentTabItem, currentPos, newContent);
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

        // Set appropriate language based on file extension
        try {
            // For now, use EmptyLanguage for all file types
            // This still provides line numbers and basic editing features
            codeEditor.setEditorLanguage(new EmptyLanguage());
        } catch (Exception e) {
            Log.w(TAG, "Failed to set language, using empty language", e);
            codeEditor.setEditorLanguage(new EmptyLanguage());
        }

        // Configure editor appearance
        codeEditor.setTextSize(14f);
        codeEditor.setLineNumberEnabled(true);
        codeEditor.setWordwrap(false);
        codeEditor.setHighlightCurrentBlock(true);
        codeEditor.setHighlightCurrentLine(true);
        codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE);

        // Set a clean color scheme
        codeEditor.setColorScheme(new EditorColorScheme());

        // Enable features for better editing experience
        codeEditor.setScrollBarEnabled(true);
        codeEditor.setVerticalScrollBarEnabled(true);
        codeEditor.setHorizontalScrollBarEnabled(true);
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

    /**
     * Debounce modified state updates to prevent flickering
     */
    private void debounceModifiedStateUpdate(TabItem tabItem, int position, String content) {
        String tabId = tabItem.getFile().getAbsolutePath();

        // Cancel existing debounce for this tab
        Runnable existingRunnable = debounceRunnables.get(tabId);
        if (existingRunnable != null) {
            debounceHandler.removeCallbacks(existingRunnable);
        }

        // Schedule new debounced update
        Runnable debounceRunnable = () -> {
            // Set modified state
            tabItem.setModified(true);

            // Notify listener
            if (tabActionListener != null) {
                tabActionListener.onTabModifiedStateChanged();
                if (position == activeTabPosition) {
                    tabActionListener.onActiveTabContentChanged(content, tabItem.getFileName());
                }
            }

            // Schedule auto-save
            scheduleAutoSave(tabItem, content);

            debounceRunnables.remove(tabId);
        };

        debounceRunnables.put(tabId, debounceRunnable);
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
    }

    /**
     * Schedule auto-save for a tab
     */
    private void scheduleAutoSave(TabItem tabItem, String content) {
        String tabId = tabItem.getFile().getAbsolutePath();

        // Cancel existing auto-save for this tab
        Runnable existingRunnable = autoSaveRunnables.get(tabId);
        if (existingRunnable != null) {
            autoSaveHandler.removeCallbacks(existingRunnable);
        }

        // Schedule new auto-save
        Runnable autoSaveRunnable = () -> {
            if (fileManager != null) {
                try {
                    fileManager.writeFileContent(tabItem.getFile(), content);
                    Log.d(TAG, "Auto-saved: " + tabItem.getFileName());
                } catch (Exception e) {
                    Log.e(TAG, "Auto-save failed for: " + tabItem.getFileName(), e);
                }
            }
            autoSaveRunnables.remove(tabId);
        };

        autoSaveRunnables.put(tabId, autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY);
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
        // Cancel all pending auto-saves
        for (Runnable runnable : autoSaveRunnables.values()) {
            autoSaveHandler.removeCallbacks(runnable);
        }
        autoSaveRunnables.clear();

        // Cancel all pending debounce operations
        for (Runnable runnable : debounceRunnables.values()) {
            debounceHandler.removeCallbacks(runnable);
        }
        debounceRunnables.clear();
    }
}