package com.mcochin.stockstreakz.utils;

/**
 * Utility class for holding methods that act on drawables.
 */
import android.graphics.drawable.Drawable;

public class DrawableUtils {
    private static final int[] EMPTY_STATE = new int[] {};

    /**
     * Clear a drawable's state e.g. pressed state or activated state.
     * @param drawable The drawable those state needs to be cleared.
     */
    public static void clearState(Drawable drawable) {
        if (drawable != null) {
            drawable.setState(EMPTY_STATE);
        }
    }
}