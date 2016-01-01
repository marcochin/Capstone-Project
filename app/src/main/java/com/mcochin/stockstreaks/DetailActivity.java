package com.mcochin.stockstreaks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mcochin.stockstreaks.fragments.DetailFragment;

/**
 * Activity for the phone to show the details of a stock.
 */
public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if(savedInstanceState == null) {
            DetailFragment detailFragment = new DetailFragment();
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.KEY_DETAIL_URI, getIntent().getData());
            detailFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, detailFragment, DetailFragment.TAG)
                    .commit();
        }
    }
}
