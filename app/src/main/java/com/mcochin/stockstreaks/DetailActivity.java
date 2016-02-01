package com.mcochin.stockstreaks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mcochin.stockstreaks.data.StockContract;
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
            // Unlike Main Activity, the onNewIntent will not be called is launched from widget.
            // That is why we don't have onNewIntent and is why why can add fragment instead of
            // replace. Launching DetailActivity from widget will always have a null
            // savedInstanceState.
            Uri detailUri = getIntent().getData();

            if(detailUri != null){
                insertFragmentIntoDetailContainer(detailUri);
            }else{
                finish();
            }
        }
    }

    private void insertFragmentIntoDetailContainer(Uri detailUri){
        DetailFragment detailFragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(DetailFragment.KEY_DETAIL_URI, detailUri);
        detailFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.detail_container, detailFragment, DetailFragment.TAG)
                .commit();
    }
}
