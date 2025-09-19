package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class RecentProjectsAdapter extends RecyclerView.Adapter<RecentProjectsAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<HashMap<String, Object>> recentProjects;
    private final MainActivity mainActivity;

    public RecentProjectsAdapter(Context context, ArrayList<HashMap<String, Object>> recentProjects, MainActivity mainActivity) {
        this.context = context;
        this.recentProjects = recentProjects;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, Object> project = recentProjects.get(position);
        String projectName = (String) project.get("name");
        String projectPath = (String) project.get("path");

        holder.projectName.setText(projectName);

        holder.itemView.setOnClickListener(v -> {
            if (mainActivity != null) {
                mainActivity.openProject(projectPath, projectName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentProjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView projectName;
        ImageView projectIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            projectName = itemView.findViewById(R.id.text_project_name);
            projectIcon = itemView.findViewById(R.id.image_project_icon);
        }
    }
}
