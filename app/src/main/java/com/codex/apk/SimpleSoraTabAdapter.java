package com.codex.apk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Simplified TabAdapter using Sora Editor for high-performance code editing.
 * Uses built-in language support without requiring TextMate assets.
 */
public class SimpleSoraTabAdapter extends RecyclerView.Adapter<SimpleSoraTabAdapter.ViewHolder> {
    private static final String TAG = "SimpleSoraTabAdapter";
    private static boolean textMateInitialized = false;
    private static final String TEXTMATE_LANG_INDEX = "textmate/languages.json";
    private static final String TEXTMATE_THEME_NAME = "github";
    private static final String TEXTMATE_THEME_PATH = "textmate/github.json";
    private final Context context;
    private final List<TabItem> openTabs;
    private final TabActionListener tabActionListener;
    private final FileManager fileManager;
    private final Map<Integer, ViewHolder> holders = new HashMap<>();
    // LRU cache for parsed diffs per tabId with content hash to avoid re-parsing
    private static final int MAX_DIFF_CACHE = 16;

    private static class DiffCacheEntry {
        java.util.List<DiffUtils.DiffLine> lines;
        int hash;
    }

    private final LinkedHashMap<String, DiffCacheEntry> diffCache = new LinkedHashMap<String, DiffCacheEntry>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, DiffCacheEntry> eldest) {
            return size() > MAX_DIFF_CACHE;
        }
    };


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
        public androidx.recyclerview.widget.RecyclerView diffRecycler;
        public InlineDiffAdapter diffAdapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            codeEditor = itemView.findViewById(R.id.code_editor);
            diffRecycler = itemView.findViewById(R.id.diff_recycler);
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
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_editor_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holders.put(position, holder);
        if (position >= openTabs.size()) {
            Log.e(TAG, "Position " + position + " is out of bounds for openTabs size " + openTabs.size());
            return;
        }

        TabItem tabItem = openTabs.get(position);
        String tabId = tabItem.getFile().getAbsolutePath();
        CodeEditor codeEditor = holder.codeEditor;
        boolean isDiffTab = tabItem.getFile().getName().startsWith("DIFF_");

        // Only reconfigure if this is a different tab
        if (!tabId.equals(holder.currentTabId)) {
            holder.currentTabId = tabId;

            // Configure the editor only for new tabs (skip heavy setup for DIFF_ tabs)
            if (!isDiffTab) {
                configureEditor(codeEditor, tabItem);
                // Set content without triggering change events
                codeEditor.setText(tabItem.getContent());
                // Apply persistent flags
                codeEditor.setWordwrap(tabItem.isWrapEnabled());
                codeEditor.setEditable(!tabItem.isReadOnly());
            } else {
                // For diff tabs, keep editor lightweight & disabled
                codeEditor.setText("");
                codeEditor.setEditable(false);
            }

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

        // Toggle between editor and diff view every bind to reflect latest state/content
        if (isDiffTab) {
            // Show diff view
            codeEditor.setVisibility(View.GONE);
            if (holder.diffRecycler != null) {
                holder.diffRecycler.setVisibility(View.VISIBLE);
                if (holder.diffRecycler.getLayoutManager() == null) {
                    holder.diffRecycler.setLayoutManager(new LinearLayoutManager(context));
                    holder.diffRecycler.setHasFixedSize(true);
                    holder.diffRecycler.setItemViewCacheSize(64);
                }
                // Parse and bind diff lines with LRU caching
                String key = tabId;
                String content = tabItem.getContent();
                int h = content != null ? content.hashCode() : 0;
                java.util.List<DiffUtils.DiffLine> lines;
                DiffCacheEntry entry = diffCache.get(key);
                if (entry == null || entry.hash != h) {
                    lines = DiffUtils.parseUnifiedDiff(content);
                    DiffCacheEntry newEntry = new DiffCacheEntry();
                    newEntry.lines = lines;
                    newEntry.hash = h;
                    diffCache.put(key, newEntry);
                } else {
                    lines = entry.lines;
                }
                if (holder.diffAdapter == null) {
                    holder.diffAdapter = new InlineDiffAdapter(context, lines);
                    holder.diffRecycler.setAdapter(holder.diffAdapter);
                } else {
                    holder.diffAdapter.updateLines(lines);
                }
            }
        } else {
            // Show normal editor
            codeEditor.setVisibility(View.VISIBLE);
            if (holder.diffRecycler != null) {
                holder.diffRecycler.setVisibility(View.GONE);
                holder.diffRecycler.setAdapter(null);
                holder.diffAdapter = null;
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

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < openTabs.size()) {
            return openTabs.get(position).getFile().getAbsolutePath().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    /**
     * Configure the Sora Editor with appropriate language and theme
     */
    private void configureEditor(CodeEditor codeEditor, TabItem tabItem) {
        String fileName = tabItem.getFileName();
        ensureTextMateInitialized(codeEditor.getContext());

        // Choose language scope based on file extension
        String scope = resolveScopeForFile(fileName);
        try {
            if (scope != null) {
                // Apply TextMate color scheme and language (with completion enabled)
                codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
                codeEditor.setEditorLanguage(TextMateLanguage.create(scope, true));
            } else {
                // Fallback if not supported
                codeEditor.setEditorLanguage(new EmptyLanguage());
                codeEditor.setColorScheme(new EditorColorScheme());
            }
        } catch (Throwable t) {
            Log.w(TAG, "Falling back to empty language for " + fileName, t);
            codeEditor.setEditorLanguage(new EmptyLanguage());
            codeEditor.setColorScheme(new EditorColorScheme());
        }

        // Configure indentation (block) guide colors to ensure they are visible
        // Uses app palette: file tree indent color for normal guides and primary_light for current block
        EditorColorScheme scheme = codeEditor.getColorScheme();
        int indentColor = ContextCompat.getColor(codeEditor.getContext(), R.color.file_tree_indent_color);
        int currentIndentColor = ContextCompat.getColor(codeEditor.getContext(), R.color.primary_light);
        scheme.setColor(EditorColorScheme.BLOCK_LINE, indentColor);
        scheme.setColor(EditorColorScheme.SIDE_BLOCK_LINE, indentColor);
        scheme.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, currentIndentColor);
        codeEditor.invalidate();

        // Configure editor appearance & ergonomics using Settings defaults and tab state
        float textSizeSp = SettingsActivity.getFontSize(codeEditor.getContext());
        codeEditor.setTextSize(textSizeSp);
        codeEditor.setLineNumberEnabled(SettingsActivity.isLineNumbersEnabled(codeEditor.getContext()));
        boolean wrap = tabItem.isWrapEnabled() || SettingsActivity.isDefaultWordWrap(codeEditor.getContext());
        codeEditor.setWordwrap(wrap);
        codeEditor.setHighlightCurrentBlock(true);
        codeEditor.setHighlightCurrentLine(true);
        codeEditor.setHighlightBracketPair(true);
        codeEditor.setBlockLineEnabled(true);
        codeEditor.setScalable(true);
        codeEditor.setCursorAnimationEnabled(true);
        codeEditor.setTabWidth(2);
        codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE);

        // Apply read-only from tab state or default setting
        boolean readOnly = tabItem.isReadOnly() || SettingsActivity.isDefaultReadOnly(codeEditor.getContext());
        codeEditor.setEditable(!readOnly);
        // Performance tweaks
        codeEditor.setInterceptParentHorizontalScrollIfNeeded(true);
        codeEditor.setBasicDisplayMode(false);

        // Reduce scrollbars for a cleaner, mobile-friendly UI
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

    private static synchronized void ensureTextMateInitialized(Context context) {
        if (textMateInitialized) return;
        try {
            Context appContext = context.getApplicationContext();
            // Register assets resolver for TextMate
            FileProviderRegistry.getInstance().addFileProvider(
                    new AssetsFileResolver(appContext.getAssets())
            );

            // Load and activate theme
            String themePath = TEXTMATE_THEME_PATH;
            IThemeSource source = IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(themePath),
                    themePath,
                    null
            );
            ThemeModel model = new ThemeModel(source, TEXTMATE_THEME_NAME);
            ThemeRegistry.getInstance().loadTheme(model);
            ThemeRegistry.getInstance().setTheme(TEXTMATE_THEME_NAME);

            // Load grammars from assets
            GrammarRegistry.getInstance().loadGrammars(TEXTMATE_LANG_INDEX);
            textMateInitialized = true;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to initialize TextMate grammars. Syntax highlight may be limited.", t);
            textMateInitialized = false;
        }
    }

    private static String resolveScopeForFile(String fileName) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            ext = fileName.substring(dot + 1).toLowerCase();
        }
        switch (ext) {
            case "html":
            case "htm":
                return "text.html.basic";
            case "css":
                return "source.css";
            case "js":
            case "mjs":
                return "source.js";
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
     * Toggle word wrap for the editor at given position if bound.
     */
    public void setWrapForPosition(int position, boolean enable) {
        RecyclerView recycler = null;
        try {
            // Try to find a RecyclerView parent to query ViewHolder
            // This adapter is attached to the ViewPager2's internal RecyclerView, but we can't access it directly here.
            // Instead, request a re-bind and apply in onBindViewHolder by storing the preference on TabItem temporarily if needed.
            // For simplicity, just notifyItemChanged to rebind and then apply in onBindViewHolder via a flag on TabItem.
            if (position >= 0 && position < openTabs.size()) {
                TabItem tab = openTabs.get(position);
                tab.setWrapEnabled(enable);
                notifyItemChanged(position);
            }
        } catch (Throwable ignore) {}
    }

    /**
     * Toggle read-only for the editor at given position if bound.
     */
    public void setReadOnlyForPosition(int position, boolean readOnly) {
        try {
            if (position >= 0 && position < openTabs.size()) {
                TabItem tab = openTabs.get(position);
                tab.setReadOnly(readOnly);
                notifyItemChanged(position);
            }
        } catch (Throwable ignore) {}
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

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holders.remove(holder.getAdapterPosition());
        // Detach diff adapter to help GC
        if (holder.diffRecycler != null) {
            holder.diffRecycler.setAdapter(null);
        }
        holder.diffAdapter = null;
        super.onViewRecycled(holder);
    }

    public ViewHolder getHolderForPosition(int position) {
        return holders.get(position);
    }

    /**
     * Purge any cached parsed diff for a specific file/tab.
     */
    public void purgeDiffCacheForFile(File file) {
        if (file == null) return;
        String key = file.getAbsolutePath();
        diffCache.remove(key);
    }

    /**
     * Clear all diff caches.
     */
    public void clearDiffCaches() {
        diffCache.clear();
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        // Detach adapters and clear holder references
        for (ViewHolder vh : holders.values()) {
            if (vh != null && vh.diffRecycler != null) {
                vh.diffRecycler.setAdapter(null);
            }
            if (vh != null) {
                vh.diffAdapter = null;
            }
        }
        holders.clear();
        clearDiffCaches();
    }
}