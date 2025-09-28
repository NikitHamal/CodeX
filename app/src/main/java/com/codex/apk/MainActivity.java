package com.codex.apk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;
import android.widget.RadioGroup;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.AIProvider;

public class MainActivity extends AppCompatActivity {

    private RecyclerView projectsRecyclerView;
    private RecyclerView recentProjectsRecyclerView;
    private TextView textEmptyProjects;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutRecentProjects;

    private ArrayList<HashMap<String, Object>> projectsList;
    private ArrayList<HashMap<String, Object>> recentProjectsList;
    private ProjectsAdapter projectsAdapter;
    private RecentProjectsAdapter recentProjectsAdapter;
    private ExtendedFloatingActionButton fabQuickActions;
    private ExtendedFloatingActionButton fabDeleteSelection;

    public ProjectManager projectManager;
    public PermissionManager permissionManager;
    public ProjectImportExportManager importExportManager;
    public GitManager gitManager;
    private AIAssistant aiAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.main);

        // Initialize Views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        projectsRecyclerView = findViewById(R.id.listview_projects);
        recentProjectsRecyclerView = findViewById(R.id.listview_recent_projects);
        textEmptyProjects = findViewById(R.id.text_empty_projects);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutRecentProjects = findViewById(R.id.layout_recent_projects);

        // Initialize Managers
        projectsList = new ArrayList<>();
        recentProjectsList = new ArrayList<>();
        projectsAdapter = new ProjectsAdapter(this, projectsList, this);
        recentProjectsAdapter = new RecentProjectsAdapter(this, recentProjectsList, this);
        projectManager = new ProjectManager(this, projectsList, projectsAdapter);
        permissionManager = new PermissionManager(this);
        importExportManager = new ProjectImportExportManager(this);
        gitManager = new GitManager(this);

        // Setup UI
        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        projectsRecyclerView.setAdapter(projectsAdapter);
        projectsRecyclerView.setNestedScrollingEnabled(false);

        recentProjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentProjectsRecyclerView.setAdapter(recentProjectsAdapter);

        // Setup Listeners
        findViewById(R.id.button_filter_projects).setOnClickListener(v -> showFilterDialog());
        fabQuickActions = findViewById(R.id.fab_quick_actions);
        fabDeleteSelection = findViewById(R.id.fab_delete_selection);
        fabQuickActions.setOnClickListener(v -> showQuickActionsMenu());

        // Selection listener to toggle FABs
        projectsAdapter.setSelectionListener((selectedCount, selectionMode) -> {
            if (selectionMode) {
                if (fabQuickActions.getVisibility() == View.VISIBLE) fabQuickActions.setVisibility(View.GONE);
                fabDeleteSelection.setVisibility(View.VISIBLE);
                fabDeleteSelection.setText(getString(R.string.delete) + (selectedCount > 0 ? " (" + selectedCount + ")" : ""));
            } else {
                fabDeleteSelection.setVisibility(View.GONE);
                fabQuickActions.setVisibility(View.VISIBLE);
            }
        });

        fabDeleteSelection.setOnClickListener(v -> onDeleteSelectionRequested());

        permissionManager.checkAndRequestPermissions();

        aiAssistant = new AIAssistant(this, null, null);
        if (SettingsActivity.getOpenRouterApiKey(this) != null && !SettingsActivity.getOpenRouterApiKey(this).isEmpty()) {
            aiAssistant.refreshModelsForProvider(AIProvider.OPENROUTER, (success, message) -> {
                // Do nothing, this is a background refresh
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_CODE_STORAGE_PERMISSION) {
            loadProjects();
        }
    }

    @Override
    public void onBackPressed() {
        if (projectsAdapter != null && projectsAdapter.isSelectionMode()) {
            projectsAdapter.exitSelectionMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (permissionManager.hasStoragePermission()) {
                loadProjects();
            }
        } else if (requestCode == ProjectImportExportManager.REQUEST_CODE_PICK_ZIP_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importExportManager.handleImportZipFile(uri);
            }
        }
    }

    private void onDeleteSelectionRequested() {
        if (projectsAdapter == null || projectsAdapter.getSelectedCount() == 0) return;
        final int count = projectsAdapter.getSelectedCount();
        new MaterialAlertDialogBuilder(this, R.style.AlertDialogCustom)
                .setTitle(getString(R.string.delete_project))
                .setMessage("Delete " + count + " selected project" + (count > 1 ? "s" : "") + "?")
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    for (File projectDir : projectsAdapter.getSelectedProjectFiles()) {
                        if (projectDir != null) {
                            projectManager.deleteProjectDirectory(projectDir);
                        }
                    }
                    projectsAdapter.exitSelectionMode();
                    loadProjects();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    public void openProject(String projectPath, String projectName) {
        if (!permissionManager.hasStoragePermission()) {
            permissionManager.checkAndRequestPermissions();
            return;
        }
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
        intent.putExtra("projectPath", projectPath);
        intent.putExtra("projectName", projectName);
        startActivity(intent);
    }

    public void updateEmptyStateVisibility() {
        boolean hasPermission = permissionManager.hasStoragePermission();
        if (projectsList.isEmpty() && hasPermission) {
            projectsRecyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            layoutRecentProjects.setVisibility(View.GONE);
        } else if (!hasPermission) {
            projectsRecyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            textEmptyProjects.setText(getString(R.string.storage_permission_required));
            layoutRecentProjects.setVisibility(View.GONE);
        } else {
            projectsRecyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            if (recentProjectsList.isEmpty()) {
                layoutRecentProjects.setVisibility(View.GONE);
            } else {
                layoutRecentProjects.setVisibility(View.VISIBLE);
            }
        }
    }

    private int currentSortOption = R.id.radio_date_newest;

    private void loadProjects() {
        projectManager.loadProjectsList();

        // First, populate recent projects, always sorted by date
        ArrayList<HashMap<String, Object>> sortedForRecent = new ArrayList<>(projectsList);
        Collections.sort(sortedForRecent, (p1, p2) -> ((String) p2.get("lastModified")).compareTo((String) p1.get("lastModified")));
        recentProjectsList.clear();
        if (sortedForRecent.size() > 5) {
            recentProjectsList.addAll(sortedForRecent.subList(0, 5));
        } else {
            recentProjectsList.addAll(sortedForRecent);
        }
        recentProjectsAdapter.notifyDataSetChanged();

        // Now, sort the main list according to the user's preference
        sortProjects();
        projectsAdapter.notifyDataSetChanged();

        updateEmptyStateVisibility();
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_projects, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radiogroup_sort_options);
        radioGroup.check(currentSortOption);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort Projects")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    currentSortOption = selectedId;
                    loadProjects();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sortProjects() {
        Comparator<HashMap<String, Object>> comparator = null;
        if (currentSortOption == R.id.radio_name_asc) {
            comparator = (p1, p2) -> ((String) p1.get("name")).compareToIgnoreCase((String) p2.get("name"));
        } else if (currentSortOption == R.id.radio_name_desc) {
            comparator = (p1, p2) -> ((String) p2.get("name")).compareToIgnoreCase((String) p1.get("name"));
        } else if (currentSortOption == R.id.radio_date_oldest) {
            comparator = Comparator.comparing(p -> (String) p.get("lastModified"));
        } else { // Default to newest
            comparator = (p1, p2) -> ((String) p2.get("lastModified")).compareTo((String) p1.get("lastModified"));
        }
        Collections.sort(projectsList, comparator);
    }

    private void showQuickActionsMenu() {
        // This could also be moved to a UIManager class in a larger app
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_quick_actions, null);
        view.findViewById(R.id.action_new_project).setOnClickListener(v -> {
            bottomSheet.dismiss();
            projectManager.showNewProjectDialog();
        });
        view.findViewById(R.id.action_import_project).setOnClickListener(v -> {
            bottomSheet.dismiss();
            importExportManager.importProject();
        });
        view.findViewById(R.id.action_git_clone).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showGitCloneDialog();
        });
        view.findViewById(R.id.action_open_about).setOnClickListener(v -> {
            bottomSheet.dismiss();
            startActivity(new Intent(this, AboutActivity.class));
        });
        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    private void showGitCloneDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_git_clone, null);
        TextInputEditText urlInput = dialogView.findViewById(R.id.edittext_repo_url);
        View progressLayout = dialogView.findViewById(R.id.layout_progress);
        TextView progressStatus = dialogView.findViewById(R.id.text_progress_status);
        TextView progressDetails = dialogView.findViewById(R.id.text_progress_details);
        MaterialButton cloneButton = dialogView.findViewById(R.id.button_clone);
        MaterialButton cancelButton = dialogView.findViewById(R.id.button_cancel);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        cloneButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a repository URL", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!gitManager.isValidGitUrl(url)) {
                Toast.makeText(this, "Please enter a valid Git repository URL", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress
            progressLayout.setVisibility(View.VISIBLE);
            cloneButton.setEnabled(false);
            urlInput.setEnabled(false);

            // Start cloning
            String projectName = gitManager.extractProjectNameFromUrl(url);
            gitManager.cloneRepository(url, projectName, new GitManager.GitCloneCallback() {
                @Override
                public void onProgress(String message, int progress) {
                    runOnUiThread(() -> {
                        progressDetails.setText(message);
                        if (progress >= 0) {
                            int safe = Math.max(0, Math.min(100, progress));
                            progressStatus.setText("Cloning repository... " + safe + "%");
                        }
                    });
                }

                @Override
                public void onSuccess(String projectPath, String projectName) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Repository cloned successfully: " + projectName, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        loadProjects();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        progressLayout.setVisibility(View.GONE);
                        cloneButton.setEnabled(true);
                        urlInput.setEnabled(true);
                    });
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Getter methods for managers to access activity context or other managers if needed
    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public ProjectImportExportManager getImportExportManager() {
        return importExportManager;
    }

    public void onPermissionsGranted() {
        loadProjects();
    }
}
