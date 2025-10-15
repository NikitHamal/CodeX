package com.codex.apk;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to render inline unified diff lines using DiffUtils parser.
 */
public class InlineDiffAdapter extends RecyclerView.Adapter<InlineDiffAdapter.DiffViewHolder> {

    private final Context context;
    private final List<DiffUtils.DiffLine> lines = new ArrayList<>();

    public InlineDiffAdapter(Context context, List<DiffUtils.DiffLine> items) {
        this.context = context;
        if (items != null) this.lines.addAll(filterDisplayable(items));
        setHasStableIds(false);
    }

    public void updateLines(List<DiffUtils.DiffLine> newLines) {
        this.lines.clear();
        if (newLines != null) this.lines.addAll(filterDisplayable(newLines));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diff_line, parent, false);
        return new DiffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DiffViewHolder holder, int position) {
        DiffUtils.DiffLine line = lines.get(position);

        holder.tvOld.setText("");
        holder.tvNew.setText("");
        holder.tvContent.setText(line.text);
        holder.itemView.setBackgroundColor(context.getColor(android.R.color.transparent));

        switch (line.type) {
            case ADDED:
                holder.itemView.setBackgroundColor(context.getColor(R.color.color_diff_added_bg));
                holder.tvNew.setText(line.newLine != null ? String.valueOf(line.newLine) : "");
                holder.tvContent.setText("+ " + line.text);
                break;
            case REMOVED:
                holder.itemView.setBackgroundColor(context.getColor(R.color.color_diff_deleted_bg));
                holder.tvOld.setText(line.oldLine != null ? String.valueOf(line.oldLine) : "");
                holder.tvContent.setText("- " + line.text);
                break;
            case CONTEXT:
                holder.tvOld.setText(line.oldLine != null ? String.valueOf(line.oldLine) : "");
                holder.tvNew.setText(line.newLine != null ? String.valueOf(line.newLine) : "");
                break;
            case HEADER:
                 holder.itemView.setVisibility(View.GONE);
                 holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                 break;
        }
    }

    @Override
    public int getItemCount() { return lines.size(); }

    static class DiffViewHolder extends RecyclerView.ViewHolder {
        TextView tvOld;
        TextView tvNew;
        TextView tvContent;
        DiffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOld = itemView.findViewById(R.id.tv_old_line);
            tvNew = itemView.findViewById(R.id.tv_new_line);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }

    // Remove header lines like @@ hunk headers and ---/+++ file markers from display
    private static List<DiffUtils.DiffLine> filterDisplayable(List<DiffUtils.DiffLine> src) {
        List<DiffUtils.DiffLine> out = new ArrayList<>();
        if (src == null) return out;
        for (DiffUtils.DiffLine d : src) {
            if (d == null) continue;
            if (d.type == DiffUtils.LineType.HEADER) continue; // hide
            out.add(d);
        }
        return out;
    }
}
