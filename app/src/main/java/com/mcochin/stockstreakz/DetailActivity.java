package com.mcochin.stockstreakz;

import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mcochin.stockstreakz.fragments.DetailFragment;

/**
 * Created by Marco on 10/31/2015.
 */
public class DetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        DetailFragment detailFragment = new DetailFragment();
        getSupportFragmentManager().beginTransaction()
            .add(R.id.detail_container, detailFragment, DetailFragment.TAG)
            .commit();
    }
}
