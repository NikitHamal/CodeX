package com.codex.apk;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.ViewHolder> {
    private static final String TAG = "FileTreeAdapter";
    private final Context context;
    private final List<FileItem> items;
    private final DrawerLayout drawerLayout;
    private final EditorActivity editorActivity;
    private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");
    
    // Enhanced visual states
    private int selectedPosition = -1;
    private boolean isSelectionMode = false;

    public FileTreeAdapter(Context context, List<FileItem> items, DrawerLayout drawerLayout, EditorActivity editorActivity) {
        this.context = context;
        this.items = items;
        this.drawerLayout = drawerLayout;
        this.editorActivity = editorActivity;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file_tree_enhanced, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = items.get(position);
        
        // Set indentation based on level
        int paddingStartPx = (int) (16 * context.getResources().getDisplayMetrics().density * item.getLevel());
        holder.itemView.setPadding(paddingStartPx, holder.itemView.getPaddingTop(),
                holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());

        // Set file name with enhanced typography
        holder.textFileName.setText(item.getName());
        
        // Set file path for tooltip
        holder.itemView.setContentDescription(item.getFile().getAbsolutePath());

        // Enhanced icon handling
        if (item.isDirectory()) {
            holder.imageFileIcon.setImageResource(item.isExpanded() ? R.drawable.ic_folder_open : R.drawable.icon_folder_round);
            
            // Check if directory is empty and hide arrow if it is
            boolean isEmpty = isDirectoryEmpty(item.getFile());
            holder.imageExpandIcon.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            
            if (!isEmpty) {
                holder.imageExpandIcon.setImageResource(item.isExpanded() ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
                
                // Animate expand icon rotation
                float rotation = item.isExpanded() ? 180f : 0f;
                holder.imageExpandIcon.setRotation(rotation);
            }
            
            // Enhanced directory styling
            holder.cardContainer.setStrokeColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardContainer.setStrokeWidth(item.isExpanded() ? 2 : 0);
        } else {
            holder.imageFileIcon.setImageResource(getFileIconResource(item.getName()));
            holder.imageExpandIcon.setVisibility(View.GONE);
            
            // File styling
            holder.cardContainer.setStrokeWidth(0);
        }

        // Enhanced click handling with animations
        holder.itemView.setOnClickListener(v -> {
            animateClick(holder.itemView);
            if (item.isDirectory()) {
                animateExpandCollapse(holder.imageExpandIcon, item.isExpanded());
                item.toggleExpanded();
                editorActivity.loadFileTree();
            } else {
                setSelectedPosition(position);
                editorActivity.openFile(item.getFile());
            }
        });

        // Enhanced expand icon click
        holder.imageExpandIcon.setOnClickListener(v -> {
            if (item.isDirectory()) {
                animateExpandCollapse(holder.imageExpandIcon, item.isExpanded());
                item.toggleExpanded();
                editorActivity.loadFileTree();
            }
        });

        // Enhanced context menu
        holder.imageMoreVert.setOnClickListener(v -> showFileContextMenu(holder.imageMoreVert, item));
        
        // Selection state
        updateSelectionState(holder, position);
        
        // Enhanced visual feedback
        holder.itemView.setOnLongClickListener(v -> {
            setSelectionMode(true);
            setSelectedPosition(position);
            return true;
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private void animateClick(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    private void animateExpandCollapse(ImageView icon, boolean isExpanded) {
        float targetRotation = isExpanded ? 0f : 180f;
        ObjectAnimator rotation = ObjectAnimator.ofFloat(icon, "rotation", icon.getRotation(), targetRotation);
        rotation.setDuration(200);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();
    }

    private void updateSelectionState(ViewHolder holder, int position) {
        if (position == selectedPosition && isSelectionMode) {
            holder.cardContainer.setStrokeColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardContainer.setStrokeWidth(3);
            holder.cardContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
        } else {
            holder.cardContainer.setStrokeWidth(0);
            holder.cardContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
        }
    }

    private void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (position != -1) {
            notifyItemChanged(position);
        }
    }

    private void setSelectionMode(boolean mode) {
        isSelectionMode = mode;
        if (!mode) {
            setSelectedPosition(-1);
        }
    }

    private int getFileIconResource(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return R.drawable.ic_html;
        if (fileName.endsWith(".css")) return R.drawable.ic_css;
        if (fileName.endsWith(".js")) return R.drawable.ic_javascript;
        if (fileName.endsWith(".json")) return R.drawable.ic_json;
        if (fileName.endsWith(".xml")) return R.drawable.ic_xml;
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) return R.drawable.ic_markdown;
        if (fileName.endsWith(".txt")) return R.drawable.ic_text;
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
            fileName.endsWith(".gif") || fileName.endsWith(".svg") || fileName.endsWith(".webp")) return R.drawable.ic_image;
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov") || 
            fileName.endsWith(".mkv") || fileName.endsWith(".webm")) return R.drawable.ic_video;
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".ogg") || 
            fileName.endsWith(".flac")) return R.drawable.ic_audio;
        if (fileName.endsWith(".pdf")) return R.drawable.ic_pdf;
        if (fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".7z") || 
            fileName.endsWith(".tar") || fileName.endsWith(".gz")) return R.drawable.ic_archive;
        return R.drawable.ic_file;
    }

    private void showFileContextMenu(View anchor, final FileItem item) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.inflate(R.menu.file_context_menu);
        
        // Show/hide menu items based on file type
        MenuItem renameItem = popup.getMenu().findItem(R.id.action_rename);
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete);
        MenuItem newFileItem = popup.getMenu().findItem(R.id.action_new_file);
        MenuItem newFolderItem = popup.getMenu().findItem(R.id.action_new_folder);
        
        if (item.isDirectory()) {
            // Directory options
            renameItem.setVisible(true);
            deleteItem.setVisible(true);
            newFileItem.setVisible(true);
            newFolderItem.setVisible(true);
        } else {
            // File options
            renameItem.setVisible(true);
            deleteItem.setVisible(true);
            newFileItem.setVisible(false);
            newFolderItem.setVisible(false);
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
                editorActivity.showNewFileDialog(item.getFile());
                return true;
            } else if (id == R.id.action_new_folder) {
                editorActivity.showNewFolderDialog(item.getFile());
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void showRenameDialog(final FileItem item) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename_file, null);
        TextInputEditText editText = dialogView.findViewById(R.id.edit_text_new_name);
        editText.setText(item.getName());
        editText.selectAll();

        new MaterialAlertDialogBuilder(context)
                .setTitle("Rename " + (item.isDirectory() ? "Folder" : "File"))
                .setView(dialogView)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (autoInvalidFileNameChars.matcher(newName).find()) {
                        Toast.makeText(context, "Invalid characters in filename", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    File newFile = new File(item.getFile().getParentFile(), newName);
                    if (newFile.exists()) {
                        Toast.makeText(context, "File already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    editorActivity.renameFileOrDir(item.getFile(), newFile);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(final FileItem item) {
        String message = "Are you sure you want to delete " + 
                        (item.isDirectory() ? "the folder" : "the file") + 
                        " \"" + item.getName() + "\"?";
        
        new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm Delete")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    editorActivity.deleteFileByPath(item.getFile());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardContainer;
        ImageView imageFileIcon, imageExpandIcon, imageMoreVert;
        MaterialTextView textFileName;

        ViewHolder(View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.card_container);
            imageFileIcon = itemView.findViewById(R.id.image_file_icon);
            imageExpandIcon = itemView.findViewById(R.id.image_expand_icon);
            imageMoreVert = itemView.findViewById(R.id.image_more_vert);
            textFileName = itemView.findViewById(R.id.text_file_name);
        }
    }
    
    private boolean isDirectoryEmpty(File directory) {
        if (!directory.isDirectory()) return true;
        File[] files = directory.listFiles();
        return files == null || files.length == 0;
    }
}