package com.codex.apk;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Split view diff adapter (GitHub-like side-by-side): pairs removed/added/context lines
 * with synchronized columns and intraline highlights.
 */
public class SplitDiffAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_PAIR = 0;
    private static final int TYPE_EXPANDER = 1;

    private static final int CONTEXT_COLLAPSE_THRESHOLD = 20;

    private final Context context;
    private final List<BaseRow> visibleRows = new ArrayList<>();
    private List<BaseRow> fullRows = new ArrayList<>();

    public SplitDiffAdapter(Context context, List<DiffUtils.DiffLine> unified) {
        this.context = context;
        setData(unified);
        setHasStableIds(false);
    }

    public void setData(List<DiffUtils.DiffLine> unified) {
        this.fullRows = buildRows(unified);
        this.visibleRows.clear();
        this.visibleRows.addAll(applyCollapse(this.fullRows));
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (visibleRows.get(position) instanceof ExpanderRow) ? TYPE_EXPANDER : TYPE_PAIR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_EXPANDER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diff_expander, parent, false);
            return new ExpanderVH(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diff_row_split, parent, false);
        return new PairVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BaseRow row = visibleRows.get(position);
        if (holder instanceof ExpanderVH) {
            ExpanderVH vh = (ExpanderVH) holder;
            vh.bind((ExpanderRow) row);
            vh.itemView.setOnClickListener(v -> expand((ExpanderRow) row));
            return;
        }
        PairVH vh = (PairVH) holder;
        PairRow pr = (PairRow) row;
        // Line numbers
        vh.tvOldLine.setText(pr.oldLineNumber > 0 ? String.valueOf(pr.oldLineNumber) : "");
        vh.tvNewLine.setText(pr.newLineNumber > 0 ? String.valueOf(pr.newLineNumber) : "");
        // Backgrounds
        if (pr.leftType == DiffUtils.LineType.REMOVED) {
            vh.leftGutter.setBackgroundColor(context.getColor(R.color.color_border_diff_deleted));
            vh.leftContainer.setBackgroundColor(context.getColor(R.color.color_diff_deleted_bg));
            vh.tvOld.setTextColor(context.getColor(R.color.color_border_diff_deleted));
        } else {
            vh.leftGutter.setBackgroundColor(context.getColor(R.color.outline_variant));
            vh.leftContainer.setBackgroundColor(context.getColor(R.color.surface));
            vh.tvOld.setTextColor(context.getColor(R.color.on_surface));
        }
        if (pr.rightType == DiffUtils.LineType.ADDED) {
            vh.rightGutter.setBackgroundColor(context.getColor(R.color.color_border_diff_added));
            vh.rightContainer.setBackgroundColor(context.getColor(R.color.color_diff_added_bg));
            vh.tvNew.setTextColor(context.getColor(R.color.color_border_diff_added));
        } else {
            vh.rightGutter.setBackgroundColor(context.getColor(R.color.outline_variant));
            vh.rightContainer.setBackgroundColor(context.getColor(R.color.surface));
            vh.tvNew.setTextColor(context.getColor(R.color.on_surface));
        }
        // Intraline highlights for changed lines
        if (pr.leftType == DiffUtils.LineType.REMOVED && pr.rightType == DiffUtils.LineType.ADDED) {
            String[] marked = DiffUtils.computeIntraline(pr.oldText, pr.newText);
            setIntraline(vh.tvOld, marked[0], context.getColor(R.color.color_border_diff_deleted));
            setIntraline(vh.tvNew, marked[1], context.getColor(R.color.color_border_diff_added));
        } else {
            vh.tvOld.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            vh.tvNew.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            vh.tvOld.setText(pr.oldText != null ? pr.oldText : "");
            vh.tvNew.setText(pr.newText != null ? pr.newText : "");
        }
    }

    @Override
    public int getItemCount() { return visibleRows.size(); }

    private void expand(ExpanderRow exp) {
        int idx = visibleRows.indexOf(exp);
        if (idx < 0) return;
        visibleRows.remove(idx);
        visibleRows.addAll(idx, exp.hidden);
        notifyItemRangeRemoved(idx, 1);
        notifyItemRangeInserted(idx, exp.hidden.size());
    }

    private void setIntraline(TextView tv, String marked, int color) {
        if (marked == null) { tv.setText(""); return; }
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int i = 0;
        while (i < marked.length()) {
            if (i + 2 <= marked.length() && marked.startsWith("<<", i)) {
                int j = marked.indexOf(">>", i + 2);
                if (j == -1) break;
                String chunk = marked.substring(i + 2, j);
                int start = ssb.length();
                ssb.append(chunk);
                ssb.setSpan(new BackgroundColorSpan(color), start, start + chunk.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i = j + 2;
                continue;
            }
            if (i + 2 <= marked.length() && marked.startsWith("[[", i)) {
                int j = marked.indexOf("]]", i + 2);
                if (j == -1) break;
                String chunk = marked.substring(i + 2, j);
                int start = ssb.length();
                ssb.append(chunk);
                ssb.setSpan(new BackgroundColorSpan(color), start, start + chunk.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i = j + 2;
                continue;
            }
            ssb.append(marked.charAt(i));
            i++;
        }
        tv.setText(ssb);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    }

    // Data models
    private static abstract class BaseRow {}

    private static class PairRow extends BaseRow {
        final int oldLineNumber;
        final int newLineNumber;
        final String oldText;
        final String newText;
        final DiffUtils.LineType leftType;
        final DiffUtils.LineType rightType;
        PairRow(int oldLn, int newLn, String oldText, String newText,
                DiffUtils.LineType leftType, DiffUtils.LineType rightType) {
            this.oldLineNumber = oldLn;
            this.newLineNumber = newLn;
            this.oldText = oldText;
            this.newText = newText;
            this.leftType = leftType;
            this.rightType = rightType;
        }
    }

    private static class ExpanderRow extends BaseRow {
        final int hiddenCount;
        final List<BaseRow> hidden;
        ExpanderRow(List<BaseRow> hidden) {
            this.hidden = hidden;
            this.hiddenCount = hidden != null ? hidden.size() : 0;
        }
    }

    private List<BaseRow> buildRows(List<DiffUtils.DiffLine> unified) {
        List<BaseRow> out = new ArrayList<>();
        if (unified == null) return out;
        List<DiffUtils.DiffLine> pendRem = new ArrayList<>();
        List<DiffUtils.DiffLine> pendAdd = new ArrayList<>();
        Runnable flush = () -> {
            int n = Math.max(pendRem.size(), pendAdd.size());
            for (int i = 0; i < n; i++) {
                DiffUtils.DiffLine r = i < pendRem.size() ? pendRem.get(i) : null;
                DiffUtils.DiffLine a = i < pendAdd.size() ? pendAdd.get(i) : null;
                int oldLn = r != null && r.oldLine != null ? r.oldLine : 0;
                int newLn = a != null && a.newLine != null ? a.newLine : 0;
                String oldTx = r != null ? r.text : "";
                String newTx = a != null ? a.text : "";
                DiffUtils.LineType lt = r != null ? DiffUtils.LineType.REMOVED : DiffUtils.LineType.CONTEXT;
                DiffUtils.LineType rt = a != null ? DiffUtils.LineType.ADDED : DiffUtils.LineType.CONTEXT;
                out.add(new PairRow(oldLn, newLn, oldTx, newTx, lt, rt));
            }
            pendRem.clear();
            pendAdd.clear();
        };

        for (DiffUtils.DiffLine d : unified) {
            switch (d.type) {
                case HEADER:
                    flush.run();
                    break;
                case REMOVED:
                    pendRem.add(d);
                    break;
                case ADDED:
                    pendAdd.add(d);
                    break;
                case CONTEXT:
                    flush.run();
                    out.add(new PairRow(d.oldLine != null ? d.oldLine : 0, d.newLine != null ? d.newLine : 0,
                            d.text, d.text, DiffUtils.LineType.CONTEXT, DiffUtils.LineType.CONTEXT));
                    break;
            }
        }
        flush.run();
        return out;
    }

    private List<BaseRow> applyCollapse(List<BaseRow> rows) {
        List<BaseRow> out = new ArrayList<>();
        List<BaseRow> contextBuffer = new ArrayList<>();
        for (BaseRow br : rows) {
            PairRow pr = (br instanceof PairRow) ? (PairRow) br : null;
            boolean isContext = pr != null && pr.leftType == DiffUtils.LineType.CONTEXT && pr.rightType == DiffUtils.LineType.CONTEXT;
            if (isContext) {
                contextBuffer.add(br);
                continue;
            }
            // flush buffer
            if (!contextBuffer.isEmpty()) {
                if (contextBuffer.size() > CONTEXT_COLLAPSE_THRESHOLD) {
                    out.add(new ExpanderRow(new ArrayList<>(contextBuffer)));
                } else {
                    out.addAll(contextBuffer);
                }
                contextBuffer.clear();
            }
            out.add(br);
        }
        if (!contextBuffer.isEmpty()) {
            if (contextBuffer.size() > CONTEXT_COLLAPSE_THRESHOLD) {
                out.add(new ExpanderRow(new ArrayList<>(contextBuffer)));
            } else {
                out.addAll(contextBuffer);
            }
        }
        return out;
    }

    static class PairVH extends RecyclerView.ViewHolder {
        View leftContainer, rightContainer, leftGutter, rightGutter;
        TextView tvOldLine, tvNewLine, tvOld, tvNew;
        PairVH(@NonNull View itemView) {
            super(itemView);
            leftContainer = itemView.findViewById(R.id.left_container);
            rightContainer = itemView.findViewById(R.id.right_container);
            leftGutter = itemView.findViewById(R.id.left_gutter);
            rightGutter = itemView.findViewById(R.id.right_gutter);
            tvOldLine = itemView.findViewById(R.id.tv_old_line);
            tvNewLine = itemView.findViewById(R.id.tv_new_line);
            tvOld = itemView.findViewById(R.id.tv_old_content);
            tvNew = itemView.findViewById(R.id.tv_new_content);
        }
    }

    static class ExpanderVH extends RecyclerView.ViewHolder {
        TextView tv;
        ExpanderVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_expander);
        }
        void bind(ExpanderRow row) {
            tv.setText("… " + row.hiddenCount + " unchanged lines. Tap to expand …");
        }
    }
}
