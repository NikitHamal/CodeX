package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {

    private final Context context;
    private final ArrayList<HashMap<String, Object>> projectsList;
    private final MainActivity mainActivity;

    public ProjectsAdapter(Context context, ArrayList<HashMap<String, Object>> projectsList, MainActivity mainActivity) {
        this.context = context;
        this.projectsList = projectsList;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        HashMap<String, Object> project = projectsList.get(position);
        holder.bind(project, position);
    }

    @Override
    public int getItemCount() {
        return projectsList.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView textProjectName;
        TextView textProjectDate;
        ImageView imageMore;

        ProjectViewHolder(View itemView) {
            super(itemView);
            textProjectName = itemView.findViewById(R.id.text_project_name);
            textProjectDate = itemView.findViewById(R.id.text_project_date);
            imageMore = itemView.findViewById(R.id.image_more);
        }

        void bind(final HashMap<String, Object> project, final int position) {
            textProjectName.setText((String) project.get("name"));
            textProjectDate.setText((String) project.get("lastModified"));

            itemView.setOnClickListener(v -> {
                String projectPath = (String) project.get("path");
                String projectName = (String) project.get("name");
                if (projectPath != null && projectName != null) {
                    mainActivity.openProject(projectPath, projectName);
                } else {
                    Toast.makeText(context, context.getString(R.string.error_invalid_project_data), Toast.LENGTH_SHORT).show();
                }
            });

            imageMore.setOnClickListener(v -> showProjectOptions(v, position));
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
                        mainActivity.getImportExportManager().exportProject(new File(projectPath), projectName);
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_invalid_project_data_for_export), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, context.getString(R.string.error_project_data_is_invalid), Toast.LENGTH_SHORT).show();
                return;
            }

            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename_project, null);
            TextInputEditText editTextNewName = dialogView.findViewById(R.id.edittext_new_name);
            editTextNewName.setText(oldName);
            editTextNewName.setSelection(oldName.length());

            AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AlertDialogCustom)
                    .setTitle(context.getString(R.string.rename_project))
                    .setView(dialogView)
                    .setPositiveButton(context.getString(R.string.rename), null)
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                MaterialButton positiveButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(v -> {
                    String newName = editTextNewName.getText().toString().trim();

                    if (newName.isEmpty()) {
                        editTextNewName.setError(context.getString(R.string.new_name_cannot_be_empty));
                        return;
                    }

                    if (newName.equals(oldName)) {
                        dialog.dismiss();
                        return;
                    }

                    File oldDir = new File(oldPath);
                    File parentDir = oldDir.getParentFile();
                    if (parentDir == null) {
                        Toast.makeText(context, context.getString(R.string.error_could_not_determine_parent_directory), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File newDir = new File(parentDir, newName);

                    if (newDir.exists()) {
                        editTextNewName.setError(context.getString(R.string.project_with_this_name_already_exists));
                        return;
                    }

                    mainActivity.getProjectManager().renameFileOrDir(oldDir, newDir);
                    dialog.dismiss();
                });
            });
            dialog.show();
        }

        private void showDeleteProjectDialog(final int position) {
            HashMap<String, Object> project = projectsList.get(position);
            String projectName = (String) project.get("name");
            String projectPath = (String) project.get("path");

            if (projectName == null || projectPath == null) {
                Toast.makeText(context, context.getString(R.string.error_project_data_is_invalid), Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(context, R.style.AlertDialogCustom)
                    .setTitle(context.getString(R.string.delete_project))
                    .setMessage(context.getString(R.string.are_you_sure_you_want_to_delete_project, projectName))
                    .setPositiveButton(context.getString(R.string.delete), (dialog, which) -> {
                        File projectDir = new File(projectPath);
                        mainActivity.getProjectManager().deleteProjectDirectory(projectDir);
                    })
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .show();
        }
    }
}
