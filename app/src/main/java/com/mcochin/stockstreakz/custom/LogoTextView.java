package com.mcochin.stockstreakz.custom;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mcochin.stockstreakz.R;

/**
 * Custom TextView for the app logo that takes in a custom attribute for font.
 */
public class LogoTextView extends TextView {
    private static final String FONTS_PATH = "fonts/";

    public LogoTextView(Context context) {
        super(context);
        init(null);
    }

    public LogoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LogoTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LogoTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs!=null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LogoTextView);
            String fontName = a.getString(R.styleable.LogoTextView_fontName);
            if (fontName!=null) {
                Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), FONTS_PATH + fontName);
                setTypeface(myTypeface);
            }
            a.recycle();
        }
    }
}
