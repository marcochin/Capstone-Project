package com.mcochin.stockstreaks.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract;

/**
 * Fragment that contains more details of the list items in the main list.
 */
public class DetailFragment extends Fragment {
    public static final String TAG = DetailFragment.class.getSimpleName();

    public static final String[] DETAIL_PROJECTION = new String[]{
            StockEntry.COLUMN_PREV_STREAK,
            StockEntry.COLUMN_PREV_STREAK_END_PRICE,
            StockEntry.COLUMN_STREAK_YEAR_HIGH,
            StockEntry.COLUMN_STREAK_YEAR_LOW
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_ref, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity)getActivity();
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
