package com.codex.apk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
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
        view.findViewById(R.id.action_open_settings).setOnClickListener(v -> {
            bottomSheet.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        view.findViewById(R.id.action_open_about).setOnClickListener(v -> {
            bottomSheet.dismiss();
            startActivity(new Intent(this, AboutActivity.class));
        });
        bottomSheet.setContentView(view);
        bottomSheet.show();
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
