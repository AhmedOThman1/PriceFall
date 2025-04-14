package com.pricefall.models;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.PagerSnapHelper;

public class LinePagerIndicatorDecoration extends RecyclerView.ItemDecoration {

    private final int indicatorHeight = (int) (16 * Resources.getSystem().getDisplayMetrics().density);

    private final float indicatorStrokeWidth;
    private final float indicatorItemLength;
    private final float indicatorItemPadding;
    private final float indicatorItemSelectedLength;
    private final float indicatorRadius;

    private final Paint paintInactive = new Paint();
    private final Paint paintActive = new Paint();
    private final boolean isOnBottom;

    public LinePagerIndicatorDecoration(int activeColor, int inactiveColor,
                                        float radiusDp, float normalLengthDp, float selectedLengthDp, float paddingDp, boolean isOnBottom) {

        indicatorStrokeWidth = dpToPx(2);
        indicatorRadius = dpToPx(radiusDp);
        indicatorItemLength = dpToPx(normalLengthDp);
        indicatorItemSelectedLength = dpToPx(selectedLengthDp);
        indicatorItemPadding = dpToPx(paddingDp);

        paintInactive.setStrokeCap(Paint.Cap.ROUND);
        paintInactive.setStrokeWidth(indicatorStrokeWidth);
        paintInactive.setStyle(Paint.Style.FILL);
        paintInactive.setAntiAlias(true);
        paintInactive.setColor(inactiveColor);

        paintActive.setStrokeCap(Paint.Cap.ROUND);
        paintActive.setStrokeWidth(indicatorStrokeWidth);
        paintActive.setStyle(Paint.Style.FILL);
        paintActive.setAntiAlias(true);
        paintActive.setColor(activeColor);

        this.isOnBottom = isOnBottom;
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int itemCount = parent.getAdapter().getItemCount();
        if (itemCount <= 1) return;

        float totalLength = (indicatorItemLength * (itemCount - 1)) + indicatorItemSelectedLength + (indicatorItemPadding * (itemCount - 1));
        float startX = (parent.getWidth() - totalLength) / 2f;
        float posY = isOnBottom ? parent.getHeight() - indicatorHeight / 2f : indicatorHeight / 2f ;

        LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
        int activePosition = layoutManager.findFirstVisibleItemPosition();

        for (int i = 0; i < itemCount; i++) {
            float length = (i == activePosition) ? indicatorItemSelectedLength : indicatorItemLength;
            float cx = startX + (length / 2);
            RectF rect = new RectF(startX, posY - indicatorRadius, startX + length, posY + indicatorRadius);
            canvas.drawRoundRect(rect, indicatorRadius, indicatorRadius, (i == activePosition) ? paintActive : paintInactive);
            startX += length + indicatorItemPadding;
        }
    }

    private float dpToPx(float dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }
}
