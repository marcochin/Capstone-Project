package com.mcochin.stockstreaks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mcochin.stockstreaks.fragments.DetailFragment;

/**
 * Created by Marco on 10/31/2015.
 */
public class DetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if(savedInstanceState == null) {
            DetailFragment detailFragment = new DetailFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, detailFragment, DetailFragment.TAG)
                    .commit();
        }

//        final View rootView = findViewById(R.id.detail_container);
//        rootView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                rootView.invalidate();
//            }
//        }, 100);
    }
}
