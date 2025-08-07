package com.codex.apk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.codex.apk.editor.AiAssistantManager;
import com.codex.apk.editor.EditorUiManager;
import com.codex.apk.editor.FileTreeManager;
import com.codex.apk.editor.TabManager;
import com.codex.apk.editor.EditorViewModel;
import com.codex.apk.editor.adapters.MainPagerAdapter;
import com.codex.apk.SimpleSoraTabAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// EditorActivity now implements the listeners for its child fragments,
// but delegates the actual logic to the new manager classes.
public class EditorActivity extends AppCompatActivity implements
        CodeEditorFragment.CodeEditorFragmentListener,
        AIChatFragment.AIChatFragmentListener {

    private static final String TAG = "EditorActivity";

    // Managers
    public EditorUiManager uiManager; // Made public for external access from managers
    public FileTreeManager fileTreeManager; // Made public for external access from managers
    public TabManager tabManager; // Made public for external access from managers
    public AiAssistantManager aiAssistantManager; // Made public for external access from managers

    // References to the fragments hosted in the main ViewPager2 (still needed for direct calls from Activity)
    private CodeEditorFragment codeEditorFragment;
    private AIChatFragment aiChatFragment;

    // Core project properties (still kept here as they define the context of the activity)
    private String projectPath;
    private String projectName;
    private File projectDir;
    private FileManager fileManager; // FileManager is a core utility, might stay here or be managed by a dedicated utility manager
    private DialogHelper dialogHelper; // DialogHelper is a core utility, might stay here or be managed by a dedicated utility manager
    private ExecutorService executorService; // ExecutorService is a core utility, might stay here or be managed by a dedicated utility manager

    private EditorViewModel viewModel;
    private List<File> pendingFilesToOpen = new ArrayList<>();

    // In onCreate or fragment setup logic, ensure chat fragment is attached and visible
    // Remove ensureChatFragment and its call in onCreate, as there is no fragment_container_chat in the layout.

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up theme based on user preferences
        ThemeManager.setupTheme(this);
        setContentView(R.layout.editor);

        // Initialize core utilities
        executorService = Executors.newCachedThreadPool();
        projectPath = getIntent().getStringExtra("projectPath");
        projectName = getIntent().getStringExtra("projectName");

        if (projectPath == null || projectName == null) {
            Toast.makeText(this, getString(R.string.error_project_information_missing), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            Toast.makeText(this, getString(R.string.invalid_project_directory, projectPath), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fileManager = new FileManager(this, projectDir);
        // DialogHelper will need references to the new managers for its callbacks, and it needs EditorActivity
        dialogHelper = new DialogHelper(this, fileManager, this);

        // Initialize ViewModel
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(EditorViewModel.class);

        // Initialize managers, passing necessary dependencies
        // Pass 'this' (EditorActivity) to managers so they can access Activity-level context and methods
        uiManager = new EditorUiManager(this, projectDir, fileManager, dialogHelper, executorService, viewModel.getOpenTabs());
        fileTreeManager = new FileTreeManager(this, fileManager, dialogHelper, viewModel.getFileItems(), viewModel.getOpenTabs());
        tabManager = new TabManager(this, fileManager, dialogHelper, viewModel.getOpenTabs());
        // Pass projectDir to AiAssistantManager for FileWatcher initialization
        aiAssistantManager = new AiAssistantManager(this, projectDir, projectName, fileManager, executorService);

        // Setup components using managers
        uiManager.initializeViews();
        uiManager.setupToolbar(); // Toolbar setup is part of UI
        fileTreeManager.setupFileTree(); // File tree setup

        // Setup TabLayout with ViewPager2
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Disable swipe, only tab clicks

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.chat));
            } else if (position == 1) {
                tab.setText(getString(R.string.editor));
            }
        }).attach();
    }

    public void onCodeEditorFragmentReady() {
        // Open index.html if no tabs are open initially
        if (viewModel.getOpenTabs().isEmpty()) {
            File indexHtml = new File(projectDir, "index.html");
            if (indexHtml.exists() && indexHtml.isFile()) {
                tabManager.openFile(indexHtml); // Use tabManager to open file
            }
        }

        // Process any pending files
        for (File file : pendingFilesToOpen) {
            tabManager.openFile(file);
        }
        pendingFilesToOpen.clear();
    }

    public void addPendingFileToOpen(File file) {
        pendingFilesToOpen.add(file);
    }

    @Override
    protected void onResume() {
        super.onResume();
        aiAssistantManager.onResume(); // Delegate API key refresh
        if (fileTreeManager != null) {
            fileTreeManager.rebuildFileTree();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            uiManager.toggleDrawer();
            return true;
        } else if (id == R.id.action_save) {
            TabItem activeTab = tabManager.getActiveTabItem();
            if (activeTab != null) {
                tabManager.saveFile(activeTab, true);
            }
            return true;
        } else if (id == R.id.action_preview) {
            launchPreviewActivity();
            return true;
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.action_share) {
            uiManager.shareProject();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Check if drawer should handle back press first
        if (uiManager.onBackPressed()) {
            return;
        }
        // Otherwise delegate to normal back press handling
        uiManager.handleBackPressed(); // Delegate back press handling
    }

    @Override
    protected void onPause() {
        super.onPause();
        tabManager.saveAllFiles(); // Delegate saving all files
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        // Shutdown AiAssistant to stop FileWatcher
        if (aiAssistantManager != null) {
            aiAssistantManager.shutdown(); // FIX: Call shutdown on AiAssistantManager
        }
    }

    // --- CodeEditorFragmentListener methods implementation (delegating to TabManager and UiManager) ---

    @Override
    public List<TabItem> getOpenTabsList() {
        return tabManager.getOpenTabs(); // Get from TabManager
    }

    @Override
    public DialogHelper getDialogHelper() {
        return dialogHelper; // Still managed here for now
    }

    @Override
    public FileManager getFileManager() {
        return fileManager; // Still managed here for now
    }

    @Override
    public String getProjectPath() {
        return projectPath; // Still managed here
    }

    @Override
    public String getProjectName() {
        return projectName; // Still managed here
    }

    @Override
    public void openFile(File file) {
        tabManager.openFile(file); // Delegate to TabManager
        closeDrawerIfOpen();
    }

    @Override
    public void closeTab(int position, boolean confirmIfModified) {
        tabManager.closeTab(position, confirmIfModified); // Delegate to TabManager
    }

    @Override
    public void closeOtherTabs(int keepPosition) {
        tabManager.closeOtherTabs(keepPosition); // Delegate to TabManager
    }

    @Override
    public void closeAllTabs() {
        tabManager.closeAllTabs(true); // Delegate to TabManager, always confirm
    }

    @Override
    public void saveFile(TabItem tabItem) {
        tabManager.saveFile(tabItem); // Delegate to TabManager
    }

    @Override
    public void showTabOptionsMenu(View anchorView, int position) {
        tabManager.showTabOptionsMenu(anchorView, position); // Delegate to TabManager
    }

    public void onActiveTabContentChanged(String content, String fileName) {
        // No-op
    }

    @Override
    public void onActiveTabChanged(File newFile) {
        uiManager.onActiveTabChanged(newFile); // Delegate to UiManager for preview update
    }

    // --- AIChatFragmentListener methods implementation (delegating to AiAssistantManager) ---

    @Override
    public AIAssistant getAIAssistant() {
        return aiAssistantManager.getAIAssistant(); // Get from AiAssistantManager
    }

    @Override
    public void sendAiPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState) {
        tabManager.getActiveTabItem(); // Ensure active tab is retrieved before sending prompt
        aiAssistantManager.sendAiPrompt(userPrompt, chatHistory, qwenState, tabManager.getActiveTabItem()); // Delegate to AiAssistantManager
    }

    // Removed direct delegation of onAiErrorReceived, onAiRequestStarted, onAiRequestCompleted
    // as these are handled internally by AiAssistantManager's AIActionListener.
    // The AIChatFragment will call these methods directly on itself, and AiAssistantManager's
    // AIActionListener will then update the AIChatFragment.

    @Override
    public void onAiAcceptActions(int messagePosition, ChatMessage message) {
        aiAssistantManager.onAiAcceptActions(messagePosition, message); // Delegate to AiAssistantManager
    }

    @Override
    public void onAiDiscardActions(int messagePosition, ChatMessage message) {
        aiAssistantManager.onAiDiscardActions(messagePosition, message); // Delegate to AiAssistantManager
    }

    @Override
    public void onReapplyActions(int messagePosition, ChatMessage message) { // Added this method implementation
        aiAssistantManager.onReapplyActions(messagePosition, message); // FIX: Delegate to AiAssistantManager
    }

    @Override
    public void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail) {
        aiAssistantManager.onAiFileChangeClicked(fileActionDetail); // Delegate to AiAssistantManager
    }

    @Override
    public void onQwenConversationStateUpdated(QwenConversationState state) {
        if (aiChatFragment != null) {
            aiChatFragment.onQwenConversationStateUpdated(state);
        }
    }

    // Public methods for managers to call back to EditorActivity for UI updates or core actions
    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(EditorActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    public void closeDrawerIfOpen() {
        if (uiManager != null) {
            uiManager.closeDrawerIfOpen();
        }
    }

    public void loadFileTree() {
        if (fileTreeManager != null) {
            fileTreeManager.loadFileTree();
        }
    }


    public ViewPager2 getMainViewPager() {
        return uiManager.getMainViewPager();
    }

    // Setters for fragment references, called by MainPagerAdapter
    public void setCodeEditorFragment(CodeEditorFragment fragment) {
        this.codeEditorFragment = fragment;
    }

    public void setAIChatFragment(AIChatFragment fragment) {
        this.aiChatFragment = fragment;
    }

    // Getters for fragment references, used by managers
    public AIChatFragment getAiChatFragment() {
        return aiChatFragment;
    }

    public CodeEditorFragment getCodeEditorFragment() {
        return codeEditorFragment;
    }

    public File getProjectDirectory() {
        return projectDir;
    }

    public TabItem getActiveTab() {
        return tabManager != null ? tabManager.getActiveTabItem() : null;
    }

    public QwenConversationState getQwenState() {
        return aiChatFragment != null ? aiChatFragment.getQwenState() : new QwenConversationState();
    }

    // Public methods for DialogHelper/FileTreeAdapter to call back to EditorActivity for manager actions
    public void showNewFileDialog(File parentDirectory) {
        fileTreeManager.showNewFileDialog(parentDirectory);
    }

    public void showNewFolderDialog(File parentDirectory) {
        fileTreeManager.showNewFolderDialog(parentDirectory);
    }

    public void renameFileOrDir(File oldFile, File newFile) {
        fileTreeManager.renameFileOrDir(oldFile, newFile);
    }

    public void deleteFileByPath(File fileOrDirectory) {
        fileTreeManager.deleteFileByPath(fileOrDirectory);
    }

    // Launch the new PreviewActivity
    private void launchPreviewActivity() {
        Intent previewIntent = new Intent(this, PreviewActivity.class);
        previewIntent.putExtra(PreviewActivity.EXTRA_PROJECT_PATH, projectPath);
        previewIntent.putExtra(PreviewActivity.EXTRA_PROJECT_NAME, projectName);

        // Get current active file content if available
        TabItem activeTab = tabManager.getActiveTabItem();
        if (activeTab != null) {
            previewIntent.putExtra(PreviewActivity.EXTRA_HTML_CONTENT, activeTab.getContent());
            previewIntent.putExtra(PreviewActivity.EXTRA_FILE_NAME, activeTab.getFileName());
        }

        startActivity(previewIntent);
    }
}