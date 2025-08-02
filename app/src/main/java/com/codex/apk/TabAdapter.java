package com.codex.apk;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// Updated import for OptimizedSyntaxHighlighter
import com.codex.apk.CodeEditor.OptimizedSyntaxHighlighter;
import com.codex.apk.CodeEditor.SyntaxHighlightingUtils; // Import for detectSyntaxType

/**
 * Enhanced TabAdapter for managing code editor tabs with improved performance and features.
 */
public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {
	private static final String TAG = "TabAdapter";
	private final Context context;
	private final List<TabItem> openTabs;
	private final TabActionListener tabActionListener;
	private final FileManager fileManager;

	// Cache for editor views to improve performance and maintain state
	private final Map<String, CodeEditorView> editorViewCache = new HashMap<>();
	private final Map<String, OptimizedSyntaxHighlighter> highlighterCache = new HashMap<>();
	
	// Auto-save functionality
	private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
	private final Map<String, Runnable> autoSaveRunnables = new HashMap<>();
	private static final int AUTO_SAVE_DELAY = 2000; // 2 seconds delay

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
	 * Constructor for TabAdapter
	 * @param context The context.
	 * @param openTabs The list of open TabItem objects.
	 * @param tabActionListener The listener for tab-related actions.
	 * @param fileManager The file manager for auto-save functionality.
	 */
	public TabAdapter(Context context, List<TabItem> openTabs, TabActionListener tabActionListener, FileManager fileManager) {
		this.context = context;
		this.openTabs = openTabs;
		this.tabActionListener = tabActionListener;
		this.fileManager = fileManager;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LinearLayout container = new LinearLayout(context);
		container.setLayoutParams(new LinearLayout.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT,
		ViewGroup.LayoutParams.MATCH_PARENT));
		container.setOrientation(LinearLayout.HORIZONTAL);
		container.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		return new ViewHolder(container);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (position >= openTabs.size()) {
			Log.e(TAG, "Position " + position + " is out of bounds for openTabs size " + openTabs.size());
			return;
		}

		TabItem tabItem = openTabs.get(position);
		String tabId = tabItem.getFile().getAbsolutePath();

		holder.container.removeAllViews();

		CodeEditorView codeEditorView;
		if (editorViewCache.containsKey(tabId)) {
			codeEditorView = editorViewCache.get(tabId);
			ViewGroup parent = (ViewGroup) codeEditorView.getParent();
			if (parent != null) {
				parent.removeView(codeEditorView);
			}
		} else {
			codeEditorView = new CodeEditorView(context);
			editorViewCache.put(tabId, codeEditorView);
		}

        // Set a tag on the CodeEditorView to store the file name for syntax detection
        codeEditorView.setTag(tabItem.getFileName());

		boolean isDiffTab = tabItem.getFile().getName().startsWith("DIFF_");

		OptimizedSyntaxHighlighter highlighter;
		if (highlighterCache.containsKey(tabId)) {
			highlighter = highlighterCache.get(tabId);
		} else {
			OptimizedSyntaxHighlighter.SyntaxType syntaxType;
			if (isDiffTab) {
				syntaxType = OptimizedSyntaxHighlighter.SyntaxType.DIFF;
			} else {
				// Corrected: Use SyntaxHighlightingUtils directly to detect syntax type
				syntaxType = SyntaxHighlightingUtils.detectSyntaxType(tabItem.getFileName());
			}
			highlighter = new OptimizedSyntaxHighlighter(context, syntaxType, isDiffTab);
			highlighterCache.put(tabId, highlighter);
		}

		codeEditorView.setHighlighter(highlighter);

		// Set content and apply highlighting based on whether it's a diff or regular file
		// IMPORTANT: Only set text if the content has actually changed or if it's a new view.
		// This prevents unnecessary text setting that can cause cursor issues.
		if (!tabItem.getContent().equals(codeEditorView.getText())) {
            if (isDiffTab) {
                codeEditorView.setDiffContent(tabItem.getContent());
            } else {
                // FIX: Pass both content and fileName to setText
                codeEditorView.setText(tabItem.getContent(), tabItem.getFileName());
            }
        } else {
            // If content is the same, ensure highlighter is still attached and active
            // and trigger a force highlight if it's a regular code file (not diff)
            if (codeEditorView.getCodeEditor().getText().length() > 0 && !isDiffTab && highlighter != null) {
                // Corrected: Cast to CodeEditorView.CodeEditText
                highlighter.attachToEditor((CodeEditorView.CodeEditText) codeEditorView.getCodeEditor());
                highlighter.setSyntaxType(SyntaxHighlightingUtils.detectSyntaxType(tabItem.getFileName())); // Ensure correct syntax type is set
                highlighter.highlightSyntax(true); // Force re-highlight
            } else if (isDiffTab && highlighter != null) {
                highlighter.highlightDiff(codeEditorView.getCodeEditor().getEditableText());
            }
        }

		// Set up content change listener for regular code files
		// This listener should be set AFTER setting the initial text to avoid double-triggering
		if (!isDiffTab) {
			codeEditorView.setOnTextChangedListener(text -> {
				if (!tabItem.getContent().equals(text)) {
					tabItem.setContent(text);
					tabItem.setModified(true);
					if (tabActionListener != null) {
						tabActionListener.onTabModifiedStateChanged();
						if (holder.getAdapterPosition() == activeTabPosition) {
							tabActionListener.onActiveTabContentChanged(text, tabItem.getFileName());
						}
					}
					
					// Auto-save functionality
					scheduleAutoSave(tabItem, text);
				}
			});
		}

		// Apply active tab styling or behavior if this is the active tab
		if (position == activeTabPosition) {
			codeEditorView.requestFocus();
			if (tabActionListener != null) {
				tabActionListener.onActiveTabChanged(tabItem.getFile());
			}
		}

		holder.container.addView(codeEditorView);
	}

	@Override
	public int getItemCount() {
		return openTabs.size();
	}

	/**
	 * Set the active tab position and notify changes to update UI.
	 * @param position The new active tab position.
	 */
	public void setActiveTab(int position) {
		if (position >= 0 && position < openTabs.size()) {
			int oldPosition = activeTabPosition;
			activeTabPosition = position;

			// Notify both old and new positions for a more precise update
			notifyItemChanged(oldPosition);
			notifyItemChanged(activeTabPosition);

			// Notify listener that the active tab has changed
			if (tabActionListener != null) {
				tabActionListener.onActiveTabChanged(openTabs.get(activeTabPosition).getFile());
			}
		}
	}

	/**
	 * Get the active tab item.
	 * @return The active TabItem object, or null if no tab is active or position is invalid.
	 */
	public TabItem getActiveTabItem() {
		if (activeTabPosition >= 0 && activeTabPosition < openTabs.size()) {
			return openTabs.get(activeTabPosition);
		}
		return null;
	}

	/**
	 * Schedules auto-save for a tab item with debouncing to avoid excessive I/O operations.
	 * @param tabItem The tab item to auto-save.
	 * @param content The current content to save.
	 */
	private void scheduleAutoSave(TabItem tabItem, String content) {
		if (fileManager == null || !SettingsActivity.isAutoSaveEnabled(context)) {
			return;
		}
		
		String filePath = tabItem.getFile().getAbsolutePath();
		
		// Cancel any existing auto-save for this file
		Runnable existingRunnable = autoSaveRunnables.get(filePath);
		if (existingRunnable != null) {
			autoSaveHandler.removeCallbacks(existingRunnable);
		}
		
		// Create new auto-save runnable
		Runnable autoSaveRunnable = () -> {
			try {
				fileManager.writeFileContent(tabItem.getFile(), content);
				tabItem.setModified(false);
				if (tabActionListener != null) {
					tabActionListener.onTabModifiedStateChanged();
				}
				Log.d(TAG, "Auto-saved: " + tabItem.getFileName());
			} catch (Exception e) {
				Log.e(TAG, "Auto-save failed for " + tabItem.getFileName(), e);
			} finally {
				autoSaveRunnables.remove(filePath);
			}
		};
		
		// Schedule the auto-save
		autoSaveRunnables.put(filePath, autoSaveRunnable);
		autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY);
	}

	/**
	 * Cleans up resources held by the adapter, such as cached editor views and highlighters.
	 * This should be called when the adapter is no longer needed to prevent memory leaks.
	 */
	public void destroy() {
		// Cancel all pending auto-saves
		for (Runnable runnable : autoSaveRunnables.values()) {
			autoSaveHandler.removeCallbacks(runnable);
		}
		autoSaveRunnables.clear();
		
		for (OptimizedSyntaxHighlighter highlighter : highlighterCache.values()) {
			highlighter.destroy();
		}
		editorViewCache.clear();
		highlighterCache.clear();
	}

	/**
	 * ViewHolder for tab items, holding the LinearLayout container.
	 */
	static class ViewHolder extends RecyclerView.ViewHolder {
		LinearLayout container;

		ViewHolder(View itemView) {
			super(itemView);
			container = (LinearLayout) itemView;
		}
	}
}
