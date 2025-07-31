package com.codex.apk.editor;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import com.google.android.material.appbar.MaterialToolbar; // Corrected import for MaterialToolbar
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.viewpager2.widget.ViewPager2;

import com.codex.apk.EditorActivity;
import com.codex.apk.FileManager;

import com.codex.apk.R;
import com.codex.apk.SettingsActivity;
import com.codex.apk.TabItem;
import com.codex.apk.DialogHelper; // Added import for DialogHelper
import com.codex.apk.editor.adapters.MainPagerAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class EditorUiManager {
    private static final String TAG = "EditorUiManager";
    private final EditorActivity activity;
    private final File projectDir;
    private final FileManager fileManager;
    private final DialogHelper dialogHelper;
    private final ExecutorService executorService;
    private final List<TabItem> openTabs; // Need access to openTabs for preview logic

    // UI components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private ViewPager2 mainViewPager;

    private ActionBarDrawerToggle drawerToggle;

    public EditorUiManager(EditorActivity activity, File projectDir, FileManager fileManager, DialogHelper dialogHelper, ExecutorService executorService, List<TabItem> openTabs) {
        this.activity = activity;
        this.projectDir = projectDir;
        this.fileManager = fileManager;
        this.dialogHelper = dialogHelper;
        this.executorService = executorService;
        this.openTabs = openTabs;
    }

    /**
     * Initializes the main UI components from the layout.
     */
    public void initializeViews() {
        try {
            // Initialize drawer components
            drawerLayout = activity.findViewById(R.id.drawer_layout);
            navigationView = activity.findViewById(R.id.navigation_drawer);
            toolbar = activity.findViewById(R.id.toolbar);
            mainViewPager = activity.findViewById(R.id.view_pager);

            // Setup refresh button
            View refreshButton = activity.findViewById(R.id.btn_refresh_file_tree);
            if (refreshButton != null) {
                refreshButton.setOnClickListener(v -> {
                    if (activity.fileTreeManager != null) {
                        activity.fileTreeManager.rebuildFileTree();
                        Toast.makeText(activity, "File tree refreshed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.e("EditorUiManager", "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(activity, "Error initializing UI components", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets up the main toolbar for the activity.
     */
    public void setupToolbar() {
        if (toolbar != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(activity.getProjectName()); // Get project name from activity
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            
            // Setup drawer toggle
            if (drawerLayout != null) {
                drawerToggle = new ActionBarDrawerToggle(
                    activity, drawerLayout, toolbar,
                    R.string.app_name, R.string.app_name) {
                    
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        super.onDrawerOpened(drawerView);
                        activity.invalidateOptionsMenu();
                    }
                    
                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        activity.invalidateOptionsMenu();
                    }
                };
                
                drawerLayout.addDrawerListener(drawerToggle);
                drawerToggle.syncState();
            }
        }
    }

    /**
     * Toggles the navigation drawer.
     */
    public void toggleDrawer() {
        if (drawerLayout != null) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }
    
    /**
     * Handles back press for drawer.
     */
    public boolean onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    /**
     * Handles the back press logic for the activity.
     */
    public void handleBackPressed() {
        // No drawer to close, go directly to exit check
        checkUnsavedChangesBeforeExit();
    }

    /**
     * Checks for unsaved changes before exiting the activity and prompts the user if any exist.
     */
    private void checkUnsavedChangesBeforeExit() {
        boolean hasUnsavedChanges = false;
        for (TabItem tab : openTabs) { // Access openTabs directly
            if (tab.isModified()) {
                hasUnsavedChanges = true;
                break;
            }
        }

        if (hasUnsavedChanges) {
            dialogHelper.showUnsavedChangesDialog(
                    () -> {
                        activity.tabManager.saveAllFiles(); // Call saveAllFiles via activity's TabManager
                        activity.finish();
                    },
                    activity::finish
            );
        } else {
            activity.finish(); // Changed from super.onBackPressed() to activity.finish()
        }
    }


    /**
     * Runs the code by launching the PreviewActivity.
     * Saves all open files before running.
     */
    public void runCode() {
        activity.tabManager.saveAllFiles(); // Call saveAllFiles via activity's TabManager
        Intent intent = new Intent(activity, SettingsActivity.class); // Changed to SettingsActivity as per original code
        intent.putExtra("projectPath", activity.getProjectPath()); // Get project path from activity

        File indexFile = new File(projectDir, "index.html");

        if (fileManager == null) {
            activity.showToast("File manager not initialized.");
            return;
        }

        if (!indexFile.exists()){
            File firstHtmlFile = fileManager.findFirstHtmlFile();
            if (firstHtmlFile != null) {
                intent.putExtra("entryFile", firstHtmlFile.getName());
                activity.showToast("index.html not found. Running " + firstHtmlFile.getName());
            } else {
                activity.showToast("No HTML file found in project root to run.");
                return;
            }
        } else {
            intent.putExtra("entryFile", "index.html");
        }
        activity.startActivity(intent);
    }

    /**
     * Shares the current project.
     */
    public void shareProject() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out my project: " + activity.getProjectName()); // Get project name from activity
        shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm working on the project '" + activity.getProjectName() + "' using CodeX editor!");
        activity.startActivity(Intent.createChooser(shareIntent, "Share Project"));
    }

    /**
     * Closes the file tree panel if it is open.
     */
    public void closeDrawerIfOpen() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * Handles content changes of the active tab (preview is now in separate activity)
     * @param content The new content of the active file.
     * @param fileName The name of the active file.
     */
    public void onActiveTabContentChanged(String content, String fileName) {
        // Preview is now in a separate activity, no need to update here
        // This method can be removed or used for other UI updates in the future
    }

    /**
     * Handles active tab changes (preview is now in separate activity)
     * @param newFile The File object of the newly active tab.
     */
    public void onActiveTabChanged(File newFile) {
        // Preview is now in a separate activity, no need to update here
        // This method can be removed or used for other UI updates in the future
    }

    public ViewPager2 getMainViewPager() {
        return mainViewPager;
    }
}