package com.pricefall.models;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CenterZoomItemDecoration extends RecyclerView.ItemDecoration {
    private final float mScale;

    public CenterZoomItemDecoration(float scale) {
        mScale = scale;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        float itemWidth = parent.getWidth() / mScale;
        float itemHeight = parent.getHeight() / mScale;

        int itemHorizontalMargin = (int) 3;//((parent.getWidth() - itemWidth) / 2);
        int itemVerticalMargin = (int) 6;//((parent.getHeight() - itemHeight) / 2);

        outRect.set(itemHorizontalMargin, itemVerticalMargin, itemHorizontalMargin, itemVerticalMargin);
    }
}
