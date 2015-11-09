package com.com.mcochin.custom;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

import com.quinny898.library.persistentsearch.SearchBox;

/**
 * EditText that fully closes the searchBox when the back button is pressed.
 */
public class BackPressEditText extends EditText {
    private SearchBox mSearchBox;

    public BackPressEditText(Context context) {
        super(context);
    }

    public BackPressEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackPressEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BackPressEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // Pressing back will fully close the search instead of originally just dismissing the
        // soft keyboard and leaving focus on the EditText

        if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP) {
            if(mSearchBox != null) {
                mSearchBox.toggleSearch();
            }
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    public void setSearchBox(SearchBox searchBox){
        //We need access to the SearchBox so the back press can close the search.
        mSearchBox = searchBox;
    }
}
