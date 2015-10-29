package com.mcochin.stockstreakz;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import com.mcochin.stockstreakz.adapters.MainAdapter;
import com.mcochin.stockstreakz.services.NetworkService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SEARCH_VIEW_ICONIFY = "searchViewIconify";
    public static final String SEARCH_VIEW_QUERY = "searchViewQuery";

    private SearchView mSearchView;
    private Bundle mSavedInstancedState;

    static final String[] FRUITS = new String[] { "Apple", "Avocado", "Banana",
            "Blueberry", "Coconut", "Durian", "Guava", "Kiwi", "Jackfruit", "Mango",
            "Olive", "Pear", "Sugar-apple", "Orange", "Strawberry", "Pineapple",
            "Watermelon", "Grape", "PassionFruit", "DragonFruit", "Honey-dew",
            "Cantaloupe", "Papaya"};

    private RecyclerView mRecyclerView;

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

        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new MainAdapter(FRUITS));

        // When recyclerView is scrolled all the way to the top elevation will disappear
        // When you start scrolling down elevation will reappear
        final AppBarLayout appbarView = (AppBarLayout)findViewById(R.id.appBar);
        if (null != appbarView) {
            ViewCompat.setElevation(appbarView, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (recyclerView.computeVerticalScrollOffset() <= 2 ) {
                            appbarView.setElevation(0);
                        } else {
                            appbarView.setElevation(appbarView.getTargetElevation());
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mSearchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        configureSearchView(mSearchView);

        return true;
    }

    private void configureSearchView(SearchView searchView) {
        searchView.setQueryHint(getString(R.string.action_search_hint));
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // If searchView is expanded on rotation then restore the state.
        if(mSavedInstancedState != null){
            boolean iconify = mSavedInstancedState.getBoolean(SEARCH_VIEW_ICONIFY);
            if(!iconify){
                searchView.setIconified(false);
                searchView.setQuery(mSavedInstancedState.getCharSequence(SEARCH_VIEW_QUERY, ""), false);
            }
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent serviceIntent = new Intent(MainActivity.this, NetworkService.class);
                serviceIntent.putExtra(SEARCH_VIEW_QUERY, query);
                startService(serviceIntent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
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
        if(mSearchView != null) {
            outState.putBoolean(SEARCH_VIEW_ICONIFY, mSearchView.isIconified());
            outState.putCharSequence(SEARCH_VIEW_QUERY, mSearchView.getQuery());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mRecyclerView) {
            mRecyclerView.clearOnScrollListeners();
        }
    }
}
