package com.codex.apk.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.R;

/**
 * Draws continuous vertical indentation lines for the file tree.
 * This relies on left padding applied per row: base + step * level.
 */
public class IndentDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;
    private final int basePx;
    private final int stepPx;
    private final float lineWidthPx;

    public IndentDecoration(@NonNull Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(context.getColor(R.color.file_tree_indent_color));
        lineWidthPx = dp(context, 1f);
        paint.setStrokeWidth(lineWidthPx);
        basePx = (int) dp(context, 12f);
        stepPx = (int) dp(context, 14f);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        final int childCount = parent.getChildCount();
        if (childCount == 0) return;

        // Determine max level visible to draw columns continuously
        int maxLevel = 0;
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int level = levelFromPadding(child.getPaddingLeft());
            if (level > maxLevel) maxLevel = level;
        }

        // Draw columns for each level from top to bottom of the list to avoid line breaks
        final int top = parent.getPaddingTop();
        final int bottom = parent.getHeight() - parent.getPaddingBottom();
        for (int level = 1; level <= maxLevel; level++) {
            float x = basePx + (float) stepPx * (level - 1);
            c.drawLine(x, top, x, bottom, paint);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // No additional spacing needed; lines are drawn in item area
        outRect.set(0, 0, 0, 0);
    }

    private static float dp(Context ctx, float dp) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        return dp * dm.density;
    }

    private int levelFromPadding(int paddingLeft) {
        int pl = Math.max(0, paddingLeft - basePx);
        return Math.max(0, Math.round((float) pl / (float) stepPx));
    }
}
