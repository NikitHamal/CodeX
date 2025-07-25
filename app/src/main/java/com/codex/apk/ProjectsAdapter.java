package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button; // Import Button for casting

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText; // Import for TextInputEditText
import com.google.android.material.button.MaterialButton; // Import for MaterialButton

import java.io.File;
import java.io.IOException; // ADDED: Import for IOException
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ProjectsAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<HashMap<String, Object>> projectsList;
    private final MainActivity mainActivity;

    public ProjectsAdapter(Context context, ArrayList<HashMap<String, Object>> projectsList, MainActivity mainActivity) {
        this.context = context;
        this.projectsList = projectsList;
        this.mainActivity = mainActivity;
    }

    @Override
    public int getCount() {
        return projectsList.size();
    }

    @Override
    public Object getItem(int position) {
        return projectsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        }

        TextView textProjectName = convertView.findViewById(R.id.text_project_name);
        TextView textProjectDate = convertView.findViewById(R.id.text_project_date);
        ImageView imageMore = convertView.findViewById(R.id.image_more);

        HashMap<String, Object> project = projectsList.get(position);
        textProjectName.setText((String) project.get("name"));
        textProjectDate.setText((String) project.get("lastModified"));

        imageMore.setOnClickListener(v -> showProjectOptions(v, position));

        return convertView;
    }

    private void showProjectOptions(View view, final int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.menu_project_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_rename) {
                showRenameProjectDialog(position);
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteProjectDialog(position);
                return true;
            } else if (id == R.id.action_share) {
                HashMap<String, Object> project = projectsList.get(position);
                String projectPath = (String) project.get("path");
                String projectName = (String) project.get("name");
                if (projectPath != null && projectName != null) {
                    mainActivity.exportProject(new File(projectPath), projectName);
                } else {
                    Toast.makeText(context, "Error: Invalid project data for export.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showRenameProjectDialog(final int position) {
        HashMap<String, Object> project = projectsList.get(position);
        String oldName = (String) project.get("name");
        String oldPath = (String) project.get("path");

        if (oldName == null || oldPath == null) {
            Toast.makeText(context, "Error: Project data is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename_project, null);
        TextInputEditText editTextNewName = dialogView.findViewById(R.id.edittext_new_name);
        editTextNewName.setText(oldName); // Pre-fill with current name
        editTextNewName.setSelection(oldName.length()); // Place cursor at end

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AlertDialogCustom)
                .setTitle("Rename Project")
                .setView(dialogView)
                .setPositiveButton("Rename", null) // Set to null initially to control dismissal
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newName = editTextNewName.getText().toString().trim();

                if (newName.isEmpty()) {
                    editTextNewName.setError("New name cannot be empty");
                    return;
                }

                if (newName.equals(oldName)) {
                    Toast.makeText(context, "New name is the same as the old name.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                File oldDir = new File(oldPath);
                File parentDir = oldDir.getParentFile();
                if (parentDir == null) {
                    Toast.makeText(context, "Error: Could not determine parent directory.", Toast.LENGTH_SHORT).show();
                    return;
                }
                File newDir = new File(parentDir, newName);

                if (newDir.exists()) {
                    editTextNewName.setError("A project with this name already exists");
                    return;
                }

                try {
                    mainActivity.renameFileOrDir(oldDir, newDir); // Delegate to MainActivity
                    // MainActivity will update its list and trigger loadProjectsList on resume
                    dialog.dismiss();
                    Toast.makeText(context, "Project renamed to '" + newName + "'", Toast.LENGTH_SHORT).show();
                } catch (IOException e) { // This is the line where the error was
                    Toast.makeText(context, "Failed to rename project: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
    }

    private void showDeleteProjectDialog(final int position) {
        HashMap<String, Object> project = projectsList.get(position);
        String projectName = (String) project.get("name");
        String projectPath = (String) project.get("path");

        if (projectName == null || projectPath == null) {
            Toast.makeText(context, "Error: Project data is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(context, R.style.AlertDialogCustom) // Use custom dialog style
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '" + projectName + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    File projectDir = new File(projectPath);
                    if (mainActivity.deleteProjectDirectory(projectDir)) {
                        // DO NOT modify projectsList or call notifyDataSetChanged here.
                        // MainActivity's onResume will reload the list and update the adapter.
                        Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to delete project", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
