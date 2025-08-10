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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private RecyclerView projectsRecyclerView;
    private TextView textEmptyProjects;
    private LinearLayout layoutEmptyState;

    private ArrayList<HashMap<String, Object>> projectsList;
    private ProjectsAdapter projectsAdapter;

    public ProjectManager projectManager;
    public PermissionManager permissionManager;
    public ProjectImportExportManager importExportManager;
    public GitManager gitManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.main);

        // Initialize Views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        projectsRecyclerView = findViewById(R.id.listview_projects);
        textEmptyProjects = findViewById(R.id.text_empty_projects);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        // Initialize Managers
        projectsList = new ArrayList<>();
        projectsAdapter = new ProjectsAdapter(this, projectsList, this);
        projectManager = new ProjectManager(this, projectsList, projectsAdapter);
        permissionManager = new PermissionManager(this);
        importExportManager = new ProjectImportExportManager(this);
        gitManager = new GitManager(this);

        // Setup UI
        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        projectsRecyclerView.setAdapter(projectsAdapter);
        projectsRecyclerView.setNestedScrollingEnabled(false);

        // Setup Listeners
        findViewById(R.id.button_refresh_projects).setOnClickListener(v -> refreshProjectsList());
        findViewById(R.id.fab_quick_actions).setOnClickListener(v -> showQuickActionsMenu());

        permissionManager.checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        projectManager.loadProjectsList();
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
            projectManager.loadProjectsList();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (permissionManager.hasStoragePermission()) {
                projectManager.loadProjectsList();
            }
        } else if (requestCode == ProjectImportExportManager.REQUEST_CODE_PICK_ZIP_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importExportManager.handleImportZipFile(uri);
            }
        }
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
        } else if (!hasPermission) {
            projectsRecyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            textEmptyProjects.setText(getString(R.string.storage_permission_required));
        } else {
            projectsRecyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void refreshProjectsList() {
        projectManager.loadProjectsList();
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
                            progressStatus.setText("Cloning repository... " + progress + "%");
                        }
                    });
                }

                @Override
                public void onSuccess(String projectPath, String projectName) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Repository cloned successfully: " + projectName, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        projectManager.loadProjectsList();
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
        projectManager.loadProjectsList();
    }
}
