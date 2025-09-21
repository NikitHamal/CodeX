package com.codex.apk.editor;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;

import com.codex.apk.EditorActivity;
import com.codex.apk.FileManager;
import com.codex.apk.R;
import com.codex.apk.SimpleSoraTabAdapter;
import com.codex.apk.TabItem;
import com.codex.apk.DialogHelper; // Added import for DialogHelper
import com.codex.apk.SettingsActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TabManager {
    private static final String TAG = "TabManager";
    private final EditorActivity activity;
    private final FileManager fileManager;
    private final DialogHelper dialogHelper;
    private final List<TabItem> openTabs; // The list of open file tabs

    public TabManager(EditorActivity activity, FileManager fileManager, DialogHelper dialogHelper, List<TabItem> openTabs) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.dialogHelper = dialogHelper;
        this.openTabs = openTabs;
    }

    public List<TabItem> getOpenTabs() {
        return openTabs;
    }

    public TabItem getActiveTabItem() {
        if (activity.getCodeEditorFragment() != null) {
            return activity.getCodeEditorFragment().getActiveTabItem();
        }
        return null;
    }

    /**
     * Opens a file in a new tab or switches to it if already open.
     * @param file The file to open.
     */
    public void openFile(File file) {
        if (!file.isFile() || !file.exists()) {
            activity.showToast("Cannot open: File does not exist or is a directory.");
            return;
        }

        if (activity.getCodeEditorFragment() == null) {
            activity.addPendingFileToOpen(file);
            activity.getMainViewPager().setCurrentItem(1, true); // Switch to editor view
            return;
        }

        for (int i = 0; i < openTabs.size(); i++) {
            if (openTabs.get(i).getFile().equals(file)) {
                // If file is already open, switch to it
                activity.getCodeEditorFragment().setFileViewPagerCurrentItem(i, true);
                // Also ensure we are on the Code tab (position 1)
                activity.getMainViewPager().setCurrentItem(1, true);
                return;
            }
        }

        try {
            if (fileManager == null) {
                activity.showToast("File manager not initialized.");
                return;
            }
            String content = fileManager.readFileContent(file);
            TabItem tabItem = new TabItem(file, content);
            // Initialize tab defaults from Settings
            tabItem.setWrapEnabled(SettingsActivity.isDefaultWordWrap(activity));
            tabItem.setReadOnly(SettingsActivity.isDefaultReadOnly(activity));
            openTabs.add(tabItem);
            activity.getCodeEditorFragment().addFileTab(tabItem); // Add to fragment's adapter
            // Switch to Code tab (position 1) and then to the newly opened file
            activity.getMainViewPager().setCurrentItem(1, false);
            activity.getCodeEditorFragment().setFileViewPagerCurrentItem(openTabs.size() - 1, true);
            activity.getCodeEditorFragment().refreshFileTabLayout();
        } catch (IOException e) {
            Log.e(TAG, "Error opening file: " + file.getAbsolutePath(), e);
            activity.showToast("Error opening file: " + e.getMessage());
        }
    }

    /**
     * Opens a new tab specifically for displaying a diff.
     * @param fileName The name of the file being diffed.
     * @param diffContent The content of the diff (e.g., unified diff format).
     */
    public void openDiffTab(String fileName, String diffContent) {
        if (activity.getCodeEditorFragment() == null) {
            activity.addPendingDiffToOpen(fileName, diffContent);
            activity.getMainViewPager().setCurrentItem(1, true); // Switch to editor view
            return;
        }

        // Create a unique file object for the diff tab.
        File diffFile = new File(activity.getProjectDirectory(), "DIFF_" + fileName); // Use projectDir from activity

        // Check if a diff tab for this file is already open
        for (int i = 0; i < openTabs.size(); i++) {
            TabItem existingTab = openTabs.get(i);
            if (existingTab.getFile().equals(diffFile)) {
                // Update content and switch to existing diff tab
                existingTab.setContent(diffContent);
                existingTab.setModified(false); // Diff tabs are not "modified" in the save sense
                activity.getCodeEditorFragment().setFileViewPagerCurrentItem(i, true);
                activity.getCodeEditorFragment().refreshFileTabLayout(); // Refresh to update content
                activity.getMainViewPager().setCurrentItem(1, true); // Switch to Code tab
                return;
            }
        }

        // Create a new TabItem for the diff
        TabItem diffTabItem = new TabItem(diffFile, diffContent);
        diffTabItem.setModified(false); // Diffs are not user-editable in this context

        openTabs.add(diffTabItem);
        activity.getCodeEditorFragment().addFileTab(diffTabItem);
        activity.getMainViewPager().setCurrentItem(1, false); // Switch to code tab without smooth scroll
        activity.getCodeEditorFragment().setFileViewPagerCurrentItem(openTabs.size() - 1, true);
        activity.getCodeEditorFragment().refreshFileTabLayout();
        activity.showToast("Opened diff for: " + fileName);
    }

    /**
     * Saves content of a single open tab.
     * @param tabItem The TabItem to save.
     */
    public void saveFile(TabItem tabItem) {
        if (tabItem == null || tabItem.getFile() == null) {
            Log.e(TAG, "Cannot save, TabItem or its file is null");
            return;
        }
        // Prevent saving for diff tabs
        if (tabItem.getFile().getName().startsWith("DIFF_")) {
            activity.showToast("Diff tabs cannot be saved.");
            tabItem.setModified(false); // Ensure it's not marked as modified
            activity.getCodeEditorFragment().refreshFileTabLayout();
            return;
        }
        try {
            if (fileManager == null) {
                activity.showToast("File manager not initialized.");
                return;
            }
            fileManager.writeFileContent(tabItem.getFile(), tabItem.getContent());
            tabItem.setModified(false);
            activity.showToast("File saved.");
            activity.getCodeEditorFragment().refreshFileTabLayout();
        } catch (IOException e) {
            Log.e(TAG, "Error saving file: " + tabItem.getFile().getAbsolutePath(), e);
            activity.showToast("Error saving " + tabItem.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * Saves content of all modified open tabs.
     */
    public void saveAllFiles() {
        for (TabItem tabItem : openTabs) {
            if (tabItem.isModified()) {
                saveFile(tabItem, false);
            }
        }
    }

    /**
     * Refreshes the content of a single tab from the file system.
     * @param position The position of the tab to refresh.
     */
    public void refreshTab(int position) {
        if (position < 0 || position >= openTabs.size()) {
            return;
        }

        TabItem tabItem = openTabs.get(position);
        File file = tabItem.getFile();

        // Don't refresh diff tabs
        if (file.getName().startsWith("DIFF_")) {
            activity.showToast("Cannot refresh a diff tab.");
            return;
        }

        if (!file.exists()) {
            activity.showToast("File no longer exists.");
            // Optionally, close the tab
            removeTabAtPosition(position);
            return;
        }

        try {
            String newContent = fileManager.readFileContent(file);
            tabItem.setContent(newContent);
            tabItem.setModified(false); // Content is now synced with the file
            activity.getCodeEditorFragment().refreshFileTab(position); // Refresh the specific tab in the adapter
            activity.showToast("Tab refreshed.");
        } catch (IOException e) {
            Log.e(TAG, "Error refreshing tab: " + file.getAbsolutePath(), e);
            activity.showToast("Error refreshing tab: " + e.getMessage());
        }
    }

    public void saveFile(TabItem tabItem, boolean showToast) {
        if (tabItem == null || tabItem.getFile() == null) {
            Log.e(TAG, "Cannot save, TabItem or its file is null");
            return;
        }
        // Prevent saving for diff tabs
        if (tabItem.getFile().getName().startsWith("DIFF_")) {
            if (showToast) {
                activity.showToast("Diff tabs cannot be saved.");
            }
            tabItem.setModified(false); // Ensure it's not marked as modified
            activity.getCodeEditorFragment().refreshFileTabLayout();
            return;
        }
        try {
            if (fileManager == null) {
                if (showToast) {
                    activity.showToast("File manager not initialized.");
                }
                return;
            }
            fileManager.writeFileContent(tabItem.getFile(), tabItem.getContent());
            tabItem.setModified(false);
            if (showToast) {
                activity.showToast("File saved.");
            }
            activity.getCodeEditorFragment().refreshFileTabLayout();
        } catch (IOException e) {
            Log.e(TAG, "Error saving file: " + tabItem.getFile().getAbsolutePath(), e);
            if (showToast) {
                activity.showToast("Error saving " + tabItem.getFileName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Closes a tab at a specific position.
     * @param position The position of the tab to close.
     * @param confirmIfModified Whether to ask for confirmation if the tab is modified.
     */
    public void closeTab(int position, boolean confirmIfModified) {
        if (position < 0 || position >= openTabs.size()) return;

        TabItem tabItem = openTabs.get(position);

        // If it's a diff tab, just close it without confirmation or saving
        if (tabItem.getFile().getName().startsWith("DIFF_")) {
            removeTabAtPosition(position);
            return;
        }

        if (confirmIfModified && tabItem.isModified()) {
            if (dialogHelper == null) {
                activity.showToast("Dialog helper not initialized.");
                return;
            }
            dialogHelper.showTabCloseConfirmationDialog(
                    tabItem.getFileName(),
                    () -> {
                        saveFile(tabItem);
                        removeTabAtPosition(position);
                    },
                    () -> removeTabAtPosition(position)
            );
        } else {
            removeTabAtPosition(position);
        }
    }

    /**
     * Removes a tab at the given position from the openTabs list and updates the UI.
     * @param position The position of the tab to remove.
     */
    private void removeTabAtPosition(int position) {
        if (position >= 0 && position < openTabs.size()) {
            // Purge diff cache for the tab being removed
            TabItem removed = openTabs.get(position);
            if (activity.getCodeEditorFragment() != null) {
                SimpleSoraTabAdapter adapter = activity.getCodeEditorFragment().getFileTabAdapter();
                if (adapter != null && removed != null) {
                    adapter.purgeDiffCacheForFile(removed.getFile());
                }
            }
            openTabs.remove(position);
            activity.getCodeEditorFragment().removeFileTab(position);
            activity.getCodeEditorFragment().refreshFileTabLayout();
        }
    }

    /**
     * Closes all tabs except the one at the specified position.
     * @param keepPosition The position of the tab to keep open.
     */
    public void closeOtherTabs(int keepPosition) {
        if (keepPosition < 0 || keepPosition >= openTabs.size()) return;

        boolean hasModifiedTabsToClose = false;
        for (int i = 0; i < openTabs.size(); i++) {
            if (i != keepPosition && openTabs.get(i).isModified() && !openTabs.get(i).getFile().getName().startsWith("DIFF_")) {
                hasModifiedTabsToClose = true;
                break;
            }
        }

        if (hasModifiedTabsToClose) {
            if (dialogHelper == null) {
                activity.showToast("Dialog helper not initialized.");
                return;
            }
            dialogHelper.showCloseOtherTabsDialog(
                    () -> { // On Save and Close
                        for (int i = 0; i < openTabs.size(); i++) {
                            if (i != keepPosition && openTabs.get(i).isModified() && !openTabs.get(i).getFile().getName().startsWith("DIFF_")) {
                                saveFile(openTabs.get(i));
                            }
                        }
                        performCloseOtherTabs(keepPosition);
                    },
                    () -> performCloseOtherTabs(keepPosition)
            );
        } else {
            performCloseOtherTabs(keepPosition);
        }
    }

    /**
     * Performs the actual closing of other tabs, keeping only the specified tab.
     * @param keepPosition The position of the tab to keep.
     */
    private void performCloseOtherTabs(int keepPosition) {
        if (keepPosition < 0 || keepPosition >= openTabs.size()) return;

        TabItem tabToKeep = openTabs.get(keepPosition);
        // Clear diff caches as many tabs are being closed at once
        if (activity.getCodeEditorFragment() != null && activity.getCodeEditorFragment().getFileTabAdapter() != null) {
            activity.getCodeEditorFragment().getFileTabAdapter().clearDiffCaches();
        }
        openTabs.clear();
        openTabs.add(tabToKeep);

        activity.getCodeEditorFragment().refreshAllFileTabs();
        if (!openTabs.isEmpty()) {
            activity.getCodeEditorFragment().setFileViewPagerCurrentItem(0, false);
        }
        activity.getCodeEditorFragment().refreshFileTabLayout();
    }

    /**
     * Closes all open tabs.
     * @param confirmIfModified Whether to ask for confirmation if any tab is modified.
     */
    public void closeAllTabs(boolean confirmIfModified) {
        boolean hasAnyModifiedTabs = false;
        if (confirmIfModified) {
            for (TabItem tab : openTabs) {
                if (tab.isModified() && !tab.getFile().getName().startsWith("DIFF_")) {
                    hasAnyModifiedTabs = true;
                    break;
                }
            }
        }

        if (hasAnyModifiedTabs) {
            if (dialogHelper == null) {
                activity.showToast("Dialog helper not initialized.");
                return;
            }
            dialogHelper.showCloseAllTabsDialog(
                    () -> {
                        saveAllFiles();
                        performCloseAllTabs();
                    },
                    this::performCloseAllTabs
            );
        } else {
            performCloseAllTabs();
        }
    }

    /**
     * Performs the actual closing of all open tabs.
     */
    private void performCloseAllTabs() {
        // Clear diff caches as all tabs are being closed
        if (activity.getCodeEditorFragment() != null && activity.getCodeEditorFragment().getFileTabAdapter() != null) {
            activity.getCodeEditorFragment().getFileTabAdapter().clearDiffCaches();
        }
        openTabs.clear();
        activity.getCodeEditorFragment().refreshAllFileTabs();
        activity.getCodeEditorFragment().refreshFileTabLayout();
    }

    /**
     * Shows a popup menu for tab options (close, close others, close all, save).
     * @param anchorView The view to anchor the popup menu to.
     * @param position The position of the tab.
     */
    public void showTabOptionsMenu(View anchorView, int position) {
        if (position < 0 || position >= openTabs.size()) return;

        PopupMenu popup = new PopupMenu(new android.view.ContextThemeWrapper(activity, R.style.TabPopupMenu), anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_tab_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_refresh_tab) {
                refreshTab(position);
                return true;
            } else if (id == R.id.action_close_tab) {
                closeTab(position, true);
                return true;
            } else if (id == R.id.action_close_other_tabs) {
                closeOtherTabs(position);
                return true;
            } else if (id == R.id.action_close_all_tabs) {
                closeAllTabs(true);
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Refreshes the content of open tabs after an AI action,
     * checking for file existence, content changes, and path updates.
     * This method is now called *after* user approval.
     */
    public void refreshOpenTabsAfterAi() {
        boolean tabsChanged = false;
        List<TabItem> toRemove = new ArrayList<>();
        List<TabItem> currentOpenTabs = new ArrayList<>(openTabs);

        if (fileManager == null || activity.getProjectDirectory() == null) {
            Log.e(TAG, "refreshOpenTabsAfterAi: FileManager or projectDir not initialized!");
            activity.showToast("Error refreshing tabs after AI.");
            return;
        }

        for (TabItem tab : currentOpenTabs) {
            // Skip diff tabs as they are not real files and don't need content refresh from disk
            if (tab.getFile().getName().startsWith("DIFF_")) {
                continue;
            }

            File tabFile = tab.getFile();
            String relativePath = fileManager.getRelativePath(tabFile, activity.getProjectDirectory());
            if (relativePath == null) {
                Log.e(TAG, "Could not determine relative path for tab: " + tabFile.getAbsolutePath());
                toRemove.add(tab);
                tabsChanged = true;
                continue;
            }
            File currentFileInProjectDir = new File(activity.getProjectDirectory(), relativePath);

            if (!currentFileInProjectDir.exists()) {
                Log.d(TAG, "Tab file " + currentFileInProjectDir.getPath() + " no longer exists. Removing tab.");
                toRemove.add(tab);
                tabsChanged = true;
            } else {
                try {
                    String newContent = fileManager.readFileContent(currentFileInProjectDir);
                    if (!newContent.equals(tab.getContent())) {
                        tab.setContent(newContent);
                        tab.setModified(false); // Mark as not modified as content is synced
                        tabsChanged = true;
                        Log.d(TAG, "Tab content for " + tab.getFileName() + " updated by AI.");
                    }
                    // Check if the file path itself changed (e.g., due to rename)
                    if (!tab.getFile().getAbsolutePath().equals(currentFileInProjectDir.getAbsolutePath())) {
                        Log.d(TAG, "Tab file path updated for " + tab.getFileName() + " from " + tab.getFile().getAbsolutePath() + " to " + currentFileInProjectDir.getAbsolutePath());
                        tab.setFile(currentFileInProjectDir);
                        tabsChanged = true;
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error reloading tab content after AI action: " + currentFileInProjectDir.getName(), e);
                    toRemove.add(tab); // Remove tab if content cannot be reloaded
                    tabsChanged = true;
                }
            }
        }

        openTabs.removeAll(toRemove);

        if (tabsChanged) {
            activity.getCodeEditorFragment().refreshAllFileTabs();
            activity.getCodeEditorFragment().refreshFileTabLayout();
        }
    }
    /**
     * Refreshes the content of open tabs after an AI action,
     * checking for file existence, content changes, and path updates.
     * This method is now called *after* user approval.
     */
    public void refreshOpenTabsAfterAi() {
        boolean tabsChanged = false;
        List<TabItem> toRemove = new ArrayList<>();
        List<TabItem> currentOpenTabs = new ArrayList<>(openTabs);

        if (fileManager == null || activity.getProjectDirectory() == null) {
            Log.e(TAG, "refreshOpenTabsAfterAi: FileManager or projectDir not initialized!");
            activity.showToast("Error refreshing tabs after AI.");
            return;
        }

        for (TabItem tab : currentOpenTabs) {
            // Skip diff tabs as they are not real files and don't need content refresh from disk
            if (tab.getFile().getName().startsWith("DIFF_")) {
                continue;
            }

            File tabFile = tab.getFile();
            String relativePath = fileManager.getRelativePath(tabFile, activity.getProjectDirectory());
            if (relativePath == null) {
                Log.e(TAG, "Could not determine relative path for tab: " + tabFile.getAbsolutePath());
                toRemove.add(tab);
                tabsChanged = true;
                continue;
            }
            File currentFileInProjectDir = new File(activity.getProjectDirectory(), relativePath);

            if (!currentFileInProjectDir.exists()) {
                Log.d(TAG, "Tab file " + currentFileInProjectDir.getPath() + " no longer exists. Removing tab.");
                toRemove.add(tab);
                tabsChanged = true;
            } else {
                try {
                    String newContent = fileManager.readFileContent(currentFileInProjectDir);
                    if (!newContent.equals(tab.getContent())) {
                        tab.setContent(newContent);
                        tab.setModified(false); // Mark as not modified as content is synced
                        tabsChanged = true;
                        Log.d(TAG, "Tab content for " + tab.getFileName() + " updated by AI.");
                    }
                    // Check if the file path itself changed (e.g., due to rename)
                    if (!tab.getFile().getAbsolutePath().equals(currentFileInProjectDir.getAbsolutePath())) {
                        Log.d(TAG, "Tab file path updated for " + tab.getFileName() + " from " + tab.getFile().getAbsolutePath() + " to " + currentFileInProjectDir.getAbsolutePath());
                        tab.setFile(currentFileInProjectDir);
                        tabsChanged = true;
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error reloading tab content after AI action: " + currentFileInProjectDir.getName(), e);
                    toRemove.add(tab); // Remove tab if content cannot be reloaded
                    tabsChanged = true;
                }
            }
        }

        openTabs.removeAll(toRemove);

        if (tabsChanged) {
            if (activity.getCodeEditorFragment() != null) {
                activity.getCodeEditorFragment().refreshAllFileTabs();
                activity.getCodeEditorFragment().refreshFileTabLayout();
            }
        }
    }
}
