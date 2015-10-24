package com.mcochin.stockstreakz;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;

import yahoofinance.YahooFinance;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getSimpleName();
    private static String SEARCH_VIEW_ICONIFY = "searchViewIconify";
    private static String SEARCH_VIEW_QUERY = "searchViewQuery";

    private SearchView mSearchView;
    private Bundle mSavedInstancedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            // Disable the default toolbar title because we have our own custom one.
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        if(savedInstanceState != null){
            mSavedInstancedState = savedInstanceState;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mSearchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        mSearchView.setQueryHint(getString(R.string.action_search_hint));

        // If searchView is expanded on rotation then restore the state.
        if(mSavedInstancedState != null){
           boolean iconify = mSavedInstancedState.getBoolean(SEARCH_VIEW_ICONIFY);
            if(!iconify){
                mSearchView.setIconified(false);
                mSearchView.setQuery(mSavedInstancedState.getCharSequence(SEARCH_VIEW_QUERY), false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save if search view is expanded
        outState.putBoolean(SEARCH_VIEW_ICONIFY, mSearchView.isIconified());
        outState.putCharSequence(SEARCH_VIEW_QUERY, mSearchView.getQuery());
    }
}
