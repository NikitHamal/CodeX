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
 * Adapter to render split (side-by-side) diffs using parsed unified diff lines.
 * Left column shows original; right shows modified. Lines are paired to align insertions/deletions.
 */
public class SplitDiffAdapter extends RecyclerView.Adapter<SplitDiffAdapter.SplitViewHolder> {

    public static class SplitRow {
        public DiffUtils.LineType oldType; // type for left column
        public DiffUtils.LineType newType; // type for right column
        public Integer oldLineNum;
        public Integer newLineNum;
        public String oldText;
        public String newText;
    }

    private final Context context;
    private final List<SplitRow> rows = new ArrayList<>();

    public SplitDiffAdapter(Context context, List<DiffUtils.DiffLine> src) {
        this.context = context;
        updateFromDiff(src);
        setHasStableIds(false);
    }

    public void updateFromDiff(List<DiffUtils.DiffLine> src) {
        rows.clear();
        if (src == null) return;
        // Pair algorithm: walk lines and align REMOVE with ADD when contiguous.
        List<DiffUtils.DiffLine> buffer = new ArrayList<>();
        for (DiffUtils.DiffLine d : src) {
            if (d.type == DiffUtils.LineType.HEADER) continue;
            // When context line, flush any pending buffer as separate pairs
            if (d.type == DiffUtils.LineType.CONTEXT) {
                flushBufferAsPairs(buffer);
                SplitRow row = new SplitRow();
                row.oldType = DiffUtils.LineType.CONTEXT;
                row.newType = DiffUtils.LineType.CONTEXT;
                row.oldLineNum = d.oldLine;
                row.newLineNum = d.newLine;
                row.oldText = d.text;
                row.newText = d.text;
                rows.add(row);
                continue;
            }
            // For changes, accumulate then pair later
            buffer.add(d);
        }
        flushBufferAsPairs(buffer);
        notifyDataSetChanged();
    }

    private void flushBufferAsPairs(List<DiffUtils.DiffLine> buffer) {
        if (buffer.isEmpty()) return;
        // Simple greedy pairing: walk and pair removed with added by order
        int i = 0;
        while (i < buffer.size()) {
            DiffUtils.DiffLine a = buffer.get(i);
            if (a.type == DiffUtils.LineType.REMOVED) {
                DiffUtils.DiffLine b = (i + 1 < buffer.size() && buffer.get(i + 1).type == DiffUtils.LineType.ADDED)
                        ? buffer.get(i + 1) : null;
                SplitRow row = new SplitRow();
                row.oldType = DiffUtils.LineType.REMOVED;
                row.newType = b != null ? DiffUtils.LineType.ADDED : DiffUtils.LineType.CONTEXT;
                row.oldLineNum = a.oldLine;
                row.newLineNum = b != null ? b.newLine : null;
                row.oldText = a.text;
                row.newText = b != null ? b.text : "";
                rows.add(row);
                i += (b != null) ? 2 : 1;
            } else if (a.type == DiffUtils.LineType.ADDED) {
                // An addition not preceded by a deletion
                SplitRow row = new SplitRow();
                row.oldType = DiffUtils.LineType.CONTEXT; // blank on left
                row.newType = DiffUtils.LineType.ADDED;
                row.oldLineNum = null;
                row.newLineNum = a.newLine;
                row.oldText = "";
                row.newText = a.text;
                rows.add(row);
                i++;
            } else { // context shouldn't be here, but handle defensively
                SplitRow row = new SplitRow();
                row.oldType = DiffUtils.LineType.CONTEXT;
                row.newType = DiffUtils.LineType.CONTEXT;
                row.oldLineNum = a.oldLine;
                row.newLineNum = a.newLine;
                row.oldText = a.text;
                row.newText = a.text;
                rows.add(row);
                i++;
            }
        }
        buffer.clear();
    }

    @NonNull
    @Override
    public SplitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diff_line_split, parent, false);
        return new SplitViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SplitViewHolder holder, int position) {
        SplitRow row = rows.get(position);
        // Defaults
        holder.tvOldLine.setText("");
        holder.tvNewLine.setText("");
        holder.tvOldContent.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        holder.tvNewContent.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);

        // Left
        if (row.oldType == DiffUtils.LineType.REMOVED) {
            holder.gutterOld.setBackgroundColor(context.getColor(R.color.color_border_diff_deleted));
            holder.itemView.setBackgroundColor(0x00000000);
            holder.tvOldLine.setText(row.oldLineNum != null ? String.valueOf(row.oldLineNum) : "");
            holder.tvOldContent.setTextColor(context.getColor(R.color.color_border_diff_deleted));
            holder.tvOldContent.setText("- " + row.oldText);
        } else if (row.oldType == DiffUtils.LineType.CONTEXT) {
            holder.gutterOld.setBackgroundColor(context.getColor(R.color.outline_variant));
            holder.tvOldLine.setText(row.oldLineNum != null ? String.valueOf(row.oldLineNum) : "");
            holder.tvOldContent.setTextColor(context.getColor(R.color.on_surface));
            holder.tvOldContent.setText("  " + row.oldText);
        } else { // ADDED shouldn't appear on left, treat as blank
            holder.gutterOld.setBackgroundColor(context.getColor(R.color.outline_variant));
            holder.tvOldLine.setText("");
            holder.tvOldContent.setText("");
        }

        // Right
        if (row.newType == DiffUtils.LineType.ADDED) {
            holder.gutterNew.setBackgroundColor(context.getColor(R.color.color_border_diff_added));
            holder.tvNewLine.setText(row.newLineNum != null ? String.valueOf(row.newLineNum) : "");
            holder.tvNewContent.setTextColor(context.getColor(R.color.color_border_diff_added));
            holder.tvNewContent.setText("+ " + row.newText);
        } else if (row.newType == DiffUtils.LineType.CONTEXT) {
            holder.gutterNew.setBackgroundColor(context.getColor(R.color.outline_variant));
            holder.tvNewLine.setText(row.newLineNum != null ? String.valueOf(row.newLineNum) : "");
            holder.tvNewContent.setTextColor(context.getColor(R.color.on_surface));
            holder.tvNewContent.setText("  " + row.newText);
        } else { // removed shouldn't appear on right; show blank
            holder.gutterNew.setBackgroundColor(context.getColor(R.color.outline_variant));
            holder.tvNewLine.setText("");
            holder.tvNewContent.setText("");
        }
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class SplitViewHolder extends RecyclerView.ViewHolder {
        View gutterOld, gutterNew;
        TextView tvOldLine, tvNewLine;
        TextView tvOldContent, tvNewContent;
        SplitViewHolder(@NonNull View itemView) {
            super(itemView);
            gutterOld = itemView.findViewById(R.id.view_gutter_old);
            gutterNew = itemView.findViewById(R.id.view_gutter_new);
            tvOldLine = itemView.findViewById(R.id.tv_old_line_split);
            tvNewLine = itemView.findViewById(R.id.tv_new_line_split);
            tvOldContent = itemView.findViewById(R.id.tv_old_content);
            tvNewContent = itemView.findViewById(R.id.tv_new_content);
        }
    }
}
