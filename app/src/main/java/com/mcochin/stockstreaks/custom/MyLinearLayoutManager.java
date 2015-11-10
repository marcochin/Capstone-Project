package com.mcochin.stockstreaks.custom;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Linear Layout Manager that implements a custom scroller for the RecyclerView.
 */
public class MyLinearLayoutManager extends LinearLayoutManager {
    private static final float MILLISECONDS_PER_INCH = 25f;
    private Context mContext;

    public MyLinearLayoutManager(Context context) {
        super(context);
        mContext = context;
    }

    public MyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(mContext) {

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
            }

            //This controls the direction in which smoothScroll looks for your view
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                //What is PointF? A class that just holds two float coordinates.
                //accepts a (x , y), use -1 for up direction, 1 for down direction
                return MyLinearLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                final int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
                final int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
                final int distance = (int) Math.sqrt(dx * dx + dy * dy);
                final int time = calculateTimeForDeceleration(distance);
                if (time > 0) {
                    action.update(-dx, -dy, time, new LinearInterpolator()); //I changed Interpolator to Linear instead of Decelerate
                }
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}