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

        // Reset defaults
        holder.itemView.setVisibility(View.VISIBLE);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(lp);
        }
        holder.tvOld.setText("");
        holder.tvNew.setText("");
        holder.tvContent.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        holder.itemView.setBackgroundColor(0x00000000); // transparent

        switch (line.type) {
            case HEADER:
                // Headers (e.g., @@ -a,+b @@ or ---/+++) are excluded via filterDisplayable();
                // This branch should rarely be hit, but render as invisible safety net.
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                holder.itemView.setVisibility(View.GONE);
                break;
            case ADDED:
                holder.viewGutter.setBackgroundColor(context.getColor(R.color.color_border_diff_added));
                holder.itemView.setBackgroundColor(context.getColor(R.color.color_diff_added_bg));
                holder.tvOld.setText("");
                holder.tvNew.setText(line.newLine != null ? String.valueOf(line.newLine) : "");
                holder.tvContent.setTextColor(context.getColor(R.color.color_border_diff_added));
                holder.tvContent.setText("+ " + line.text);
                break;
            case REMOVED:
                holder.viewGutter.setBackgroundColor(context.getColor(R.color.color_border_diff_deleted));
                holder.itemView.setBackgroundColor(context.getColor(R.color.color_diff_deleted_bg));
                holder.tvOld.setText(line.oldLine != null ? String.valueOf(line.oldLine) : "");
                holder.tvNew.setText("");
                holder.tvContent.setTextColor(context.getColor(R.color.color_border_diff_deleted));
                holder.tvContent.setText("- " + line.text);
                break;
            case CONTEXT:
                holder.viewGutter.setBackgroundColor(context.getColor(R.color.outline_variant));
                holder.itemView.setBackgroundColor(context.getColor(R.color.surface));
                holder.tvOld.setText(line.oldLine != null ? String.valueOf(line.oldLine) : "");
                holder.tvNew.setText(line.newLine != null ? String.valueOf(line.newLine) : "");
                holder.tvContent.setTextColor(context.getColor(R.color.on_surface));
                holder.tvContent.setText("  " + line.text);
                break;
        }
    }

    @Override
    public int getItemCount() { return lines.size(); }

    static class DiffViewHolder extends RecyclerView.ViewHolder {
        View viewGutter;
        TextView tvOld;
        TextView tvNew;
        TextView tvContent;
        DiffViewHolder(@NonNull View itemView) {
            super(itemView);
            viewGutter = itemView.findViewById(R.id.view_gutter);
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
