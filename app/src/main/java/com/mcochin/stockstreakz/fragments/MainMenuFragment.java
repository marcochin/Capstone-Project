package com.mcochin.stockstreakz.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.mcochin.stockstreakz.R;
import com.mcochin.stockstreakz.services.NetworkService;

/**
 * Fragment that controls the menu in the <code>MainActivity</code>.
 */
public class MainMenuFragment extends Fragment {
    public static final String TAG = MainMenuFragment.class.getSimpleName();
    private static final String SEARCH_VIEW_ICONIFY = "searchViewIconify";
    private static final String SEARCH_VIEW_FOCUSED = "searchViewFocused";
    public static final String SEARCH_VIEW_QUERY = "searchViewQuery";

    private SearchView mSearchView;
    private Bundle mSavedInstancedState;
    private boolean mSearchViewFocused;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if(savedInstanceState != null){
            mSavedInstancedState = savedInstanceState;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        mSearchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        configureSearchView(mSearchView);
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

    private void configureSearchView(SearchView searchView) {
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // If searchView is expanded on rotation then restore the state.
        if(mSavedInstancedState != null){
            boolean iconify = mSavedInstancedState.getBoolean(SEARCH_VIEW_ICONIFY);
            if(!iconify){
                searchView.setIconified(false);
                searchView.setQuery(mSavedInstancedState.getCharSequence(SEARCH_VIEW_QUERY, ""), false);
                if(!mSavedInstancedState.getBoolean(SEARCH_VIEW_FOCUSED)){
                    mSearchView.clearFocus();
                }
            }
        }

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mSearchViewFocused = hasFocus;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent serviceIntent = new Intent(getContext(), NetworkService.class);
                serviceIntent.putExtra(SEARCH_VIEW_QUERY, query);
                getActivity().startService(serviceIntent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save if search view is expanded
        if(mSearchView != null) {
            outState.putBoolean(SEARCH_VIEW_ICONIFY, mSearchView.isIconified());
            outState.putBoolean(SEARCH_VIEW_FOCUSED, mSearchViewFocused);
            outState.putCharSequence(SEARCH_VIEW_QUERY, mSearchView.getQuery());
        }
    }

    public void clearSearchViewFocus(){
        mSearchView.clearFocus();
    }
}