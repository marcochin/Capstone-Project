package com.mcochin.stockstreaks.fragments;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.mcochin.stockstreaks.data.ListManipulator;

public class ListManipulatorFragment extends Fragment {
    public static final String TAG = ListManipulatorFragment.class.getSimpleName();

    private ListManipulator mListManipulator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }

}
