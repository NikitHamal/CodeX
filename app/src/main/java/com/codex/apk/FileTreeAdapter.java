package com.codex.apk;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.ViewHolder> {
    private static final String TAG = "FileTreeAdapter"; // Changed TAG for clarity
    private final Context context;
    private final List<FileItem> items;
    private final DrawerLayout drawerLayout;
    private final EditorActivity editorActivity; // Keep reference to EditorActivity
    private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");

    public FileTreeAdapter(Context context, List<FileItem> items, DrawerLayout drawerLayout, EditorActivity editorActivity) {
        this.context = context;
        this.items = items;
        this.drawerLayout = drawerLayout;
        this.editorActivity = editorActivity;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file_tree, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = items.get(position);

        int paddingStartPx = (int) (20 * context.getResources().getDisplayMetrics().density * item.getLevel());
        holder.itemView.setPadding(paddingStartPx, holder.itemView.getPaddingTop(),
                holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());

        holder.textFileName.setText(item.getName());

        if (item.isDirectory()) {
            holder.imageFileIcon.setImageResource(item.isExpanded() ? R.drawable.ic_folder_open : R.drawable.icon_folder_round);
            holder.imageExpandIcon.setVisibility(View.VISIBLE);
            holder.imageExpandIcon.setImageResource(item.isExpanded() ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        } else {
            holder.imageFileIcon.setImageResource(getFileIconResource(item.getName()));
            holder.imageExpandIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (item.isDirectory()) {
                item.toggleExpanded();
                editorActivity.loadFileTree(); // Call through EditorActivity
            } else {
                editorActivity.openFile(item.getFile()); // Call through EditorActivity
                // No drawer to close in new side panel design
            }
        });

        holder.imageExpandIcon.setOnClickListener(v -> {
            if (item.isDirectory()) {
                item.toggleExpanded();
                editorActivity.loadFileTree(); // Call through EditorActivity
            }
        });
        // Remove long click for context menu
        // Add vert icon click for context menu
        ImageView moreVert = holder.itemView.findViewById(R.id.image_more_vert);
        if (moreVert != null) {
            moreVert.setOnClickListener(v -> showFileContextMenu(moreVert, item));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    private int getFileIconResource(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return R.drawable.ic_html;
        if (fileName.endsWith(".css")) return R.drawable.ic_css;
        if (fileName.endsWith(".js")) return R.drawable.ic_javascript;
        if (fileName.endsWith(".json")) return R.drawable.ic_json;
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif") || fileName.endsWith(".webp")) return R.drawable.ic_image;
        if (fileName.endsWith(".svg")) return R.drawable.ic_svg;
        return R.drawable.ic_file;
    }

    private void showFileContextMenu(View anchor, final FileItem item) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_file_options, popup.getMenu());

        MenuItem newFileMenuItem = popup.getMenu().findItem(R.id.action_new_file);
        MenuItem newFolderMenuItem = popup.getMenu().findItem(R.id.action_new_folder);

        if (item.isDirectory()) {
            if (newFileMenuItem != null) newFileMenuItem.setVisible(true);
            if (newFolderMenuItem != null) newFolderMenuItem.setVisible(true);
        } else {
            if (newFileMenuItem != null) newFileMenuItem.setVisible(false);
            if (newFolderMenuItem != null) newFolderMenuItem.setVisible(false);
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.action_rename) {
                showRenameDialog(item);
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteDialog(item);
                return true;
            } else if (id == R.id.action_new_file) {
                if (item.isDirectory()) editorActivity.showNewFileDialog(item.getFile()); // Call through EditorActivity
                return true;
            } else if (id == R.id.action_new_folder) {
                if (item.isDirectory()) editorActivity.showNewFolderDialog(item.getFile()); // Call through EditorActivity
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showRenameDialog(final FileItem item) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename_file, null);
        TextInputEditText editTextNewName = dialogView.findViewById(R.id.edittext_new_name);
        editTextNewName.setText(item.getName());
        editTextNewName.requestFocus();

        new MaterialAlertDialogBuilder(context)
        .setTitle(item.isDirectory() ? context.getString(R.string.rename_folder) : context.getString(R.string.rename_file))
        .setView(dialogView)
        .setPositiveButton(context.getString(R.string.rename), (dialog, which) -> {
            String newName = editTextNewName.getText().toString().trim();
            if (newName.isEmpty() || newName.equals(item.getName())) return;
            if (autoInvalidFileNameChars.matcher(newName).find()) {
                Toast.makeText(context, context.getString(R.string.invalid_characters_in_name), Toast.LENGTH_SHORT).show();
                return;
            }

            File oldFile = item.getFile();
            File newFile = new File(oldFile.getParentFile(), newName);

            if (newFile.exists()) {
                Toast.makeText(context, context.getString(R.string.name_already_exists), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                editorActivity.renameFileOrDir(oldFile, newFile); // Call through EditorActivity

                // The logic to update open tabs is now handled by editorActivity.renameFileOrDir
                // and its internal call to refreshOpenTabsAfterFileOperation.
                // So, remove the direct tab update logic here.
                // editorActivity.loadFileTree() and editorActivity.refreshFileTabLayoutInFragment() are called by renameFileOrDir.

            } catch (IOException e) {
                Log.e(TAG, "Error renaming from adapter", e);
                Toast.makeText(context, context.getString(R.string.rename_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        })
        .setNegativeButton(context.getString(R.string.cancel), null)
        .show();
    }

    private void showDeleteDialog(final FileItem item) {
        new MaterialAlertDialogBuilder(context)
        .setTitle(item.isDirectory() ? context.getString(R.string.delete_folder) : context.getString(R.string.delete_file))
        .setMessage(context.getString(R.string.are_you_sure_you_want_to_delete, item.getName()))
        .setPositiveButton(context.getString(R.string.delete), (dialog, which) -> {
            try {
                editorActivity.deleteFileByPath(item.getFile()); // Call through EditorActivity

                // The logic to close open tabs is now handled by editorActivity.deleteFileByPath
                // and its internal call to refreshOpenTabsAfterFileOperation.
                // So, remove the direct tab close logic here.
                // editorActivity.loadFileTree() is called by deleteFileByPath.

            } catch (IOException e) {
                Log.e(TAG, "Error deleting from adapter", e);
                Toast.makeText(context, context.getString(R.string.delete_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        })
        .setNegativeButton(context.getString(R.string.cancel), null)
        .show();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageFileIcon, imageExpandIcon;
        TextView textFileName;
        ViewHolder(View itemView) {
            super(itemView);
            imageFileIcon = itemView.findViewById(R.id.image_file_icon);
            imageExpandIcon = itemView.findViewById(R.id.image_expand_icon);
            textFileName = itemView.findViewById(R.id.text_file_name);
        }
    }
}