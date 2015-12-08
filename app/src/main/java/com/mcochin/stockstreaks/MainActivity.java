package com.mcochin.stockstreaks;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.mcochin.stockstreaks.adapters.MainAdapter;
import com.mcochin.stockstreaks.custom.MyLinearLayoutManager;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.fragments.ListManipulatorFragment;
import com.mcochin.stockstreaks.services.NetworkService;
import com.mcochin.stockstreaks.utils.Utility;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchBox.SearchListener, MainAdapter.EventListener, SwipeRefreshLayout.OnRefreshListener,
        ListManipulatorFragment.EventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_SEARCH_FOCUSED = "searchFocused";
    private static final int ID_LOADER_STOCK_WITH_SYMBOL = 2;

    private RecyclerView mRecyclerView;
    private MyLinearLayoutManager mLayoutManager;
    private MainAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;
    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerViewTouchActionGuardManager mTouchActionGuardManager;


    private EditText mSearchEditText;
    private SearchBox mAppBar;
    private SwipeRefreshLayout mSwipeToRefresh;
    private View mRootView;

    private View.OnClickListener mSnackBarActionListener;

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find our views from xml layouts
        mTwoPane = findViewById(R.id.detail_container) != null;
        mRootView = findViewById(R.id.rootView);
        mAppBar = (SearchBox)findViewById(R.id.appBar);
        mSearchEditText = (EditText)findViewById(R.id.search);
        mSwipeToRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_to_refresh);
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);

        if (savedInstanceState == null) {
            // Initialize the fragment that stores the list
            getSupportFragmentManager().beginTransaction()
                    .add(new ListManipulatorFragment(), ListManipulatorFragment.TAG).commit();

            getSupportFragmentManager().executePendingTransactions();
        } else{
            // If editText was focused, return that focus on orientation change
            if (savedInstanceState.getBoolean(KEY_SEARCH_FOCUSED)) {
                mAppBar.toggleSearch();
            }
        }

        ((ListManipulatorFragment) getSupportFragmentManager()
                .findFragmentByTag(ListManipulatorFragment.TAG)).setEventListener(this);

        // Initialize our snack bar action button listener
        mSnackBarActionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getListManipulator().undoLastRemoveItem();
                mAdapter.notifyItemInserted(position);
                mRecyclerView.smoothScrollToPosition(position);
            }
        };

        // Set our refresh listener for swiping down to refresh
        mSwipeToRefresh.setOnRefreshListener(this);

        // Initialize our searchBox overflow menu and search callbacks
        mAppBar.setOverflowMenu(R.menu.menu_main);
        mAppBar.setSearchListener(this);

        configureRecyclerView();
        configureAppBarDynamicElevation();

        // Fetch the stock list
        fetchStockList();
    }

    private void fetchStockList(){
        ListManipulatorFragment listManipulatorFragment =
                ((ListManipulatorFragment) getSupportFragmentManager()
                        .findFragmentByTag(ListManipulatorFragment.TAG));

        if(!Utility.canUpdateList(getContentResolver()) || !Utility.isNetworkAvailable(this)){
            listManipulatorFragment.initLoadAllFromDb();
        }else{
            listManipulatorFragment.initLoadAFew();
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadNextFewFinished() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoadAllFromDbFinished() {// ListManipulatorFragment.EventListener
        mAdapter.notifyDataSetChanged();
    }

    @Override // SearchBox.SearchListener
    public void onSearchOpened() {

    }

    @Override // SearchBox.SearchListener
    public void onSearchCleared() {

    }

    @Override // SearchBox.SearchListener
    public void onSearchClosed() {

    }

    @Override // SearchBox.SearchListener
    public void onSearchTermChanged(String term) {

    }

    @Override // SearchBox.SearchListener
    public void onResultClick(SearchResult result) {

    }

    @Override // SearchBox.SearchListener
    public void onSearch(String query) {
        query = query.toUpperCase(Locale.US);

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(MainActivity.this,
                    R.string.toast_empty_search, Toast.LENGTH_SHORT).show();
            return;
        }

        //Start service to retrieve stock info
        Intent serviceIntent = new Intent(MainActivity.this, NetworkService.class);
        serviceIntent.putExtra(NetworkService.KEY_SEARCH_QUERY, query);
        serviceIntent.setAction(NetworkService.ACTION_STOCK_WITH_SYMBOL);
        startService(serviceIntent);

        //Start cursor loader to load the newly added stock
        getSupportLoaderManager().restartLoader(ID_LOADER_STOCK_WITH_SYMBOL,
                null, MainActivity.this);
    }

    @Override // SwipeRefreshLayout.OnRefreshListener
    public void onRefresh() {
        if(Utility.canUpdateList(getContentResolver())) {
            ((ListManipulatorFragment) getSupportFragmentManager()
                    .findFragmentByTag(ListManipulatorFragment.TAG)).initLoadAFew();
        }
    }

    @Override // LoaderManager.LoaderCallbacks<Cursor>
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        switch(id){
            case ID_LOADER_STOCK_WITH_SYMBOL:
                loader = new CursorLoader(
                        MainActivity.this,
                        StockEntry.buildUri(mSearchEditText.getText().toString()
                                .toUpperCase(Locale.US)),
                        ListManipulator.STOCK_PROJECTION,
                        null,
                        null,
                        null);
                break;
        }
        return loader;
    }

    @Override // LoaderManager.LoaderCallbacks<Cursor>
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null || data.getCount() == 0){
            return;
        }

        int id = loader.getId();

        switch (id) {
            case ID_LOADER_STOCK_WITH_SYMBOL:
                Log.d(TAG, "loader stock_with_symbol");
                ListManipulator listManipulator = getListManipulator();

                if(data.moveToFirst()){
                    listManipulator.addItem(Utility.getStockFromCursor(data));
                }
                int position = listManipulator.getCount() - 1;
                mAdapter.notifyItemInserted(position);
                mRecyclerView.smoothScrollToPosition(position);

                break;
        }
    }

    @Override // LoaderManager.LoaderCallbacks<Cursor>
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override // MainAdapter.EventListener
    public void onItemClick(MainAdapter.MainViewHolder holder) {
        if(mTwoPane){
            //If tablet insert fragment into container

        }else{
            //If phone open activity
            Intent openDetail = new Intent(MainActivity.this, DetailActivity.class);
            startActivity(openDetail);
        }
    }

    @Override // MainAdapter.EventListener
    public void onItemRemoved(MainAdapter.MainViewHolder holder) {
        Snackbar.make(
                mRootView,
                getString(R.string.placeholder_snackbar_main_text, holder.getSymbol()),
                    Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_action_text, mSnackBarActionListener)
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SEARCH_FOCUSED, mSearchEditText.isFocused());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        mDragDropManager.cancelDrag();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mDragDropManager != null) {
            mDragDropManager.release();
            mDragDropManager = null;
        }

        if (mSwipeManager != null) {
            mSwipeManager.release();
            mSwipeManager = null;
        }

        if (mTouchActionGuardManager != null) {
            mTouchActionGuardManager.release();
            mTouchActionGuardManager = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
        mAdapter.release();
        mAdapter = null;
        mLayoutManager = null;

        super.onDestroy();
    }

    private void configureRecyclerView(){

        // Touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mTouchActionGuardManager.setEnabled(true);

        // Drag & drop manager
        mDragDropManager = new RecyclerViewDragDropManager();
        mDragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.material_shadow_z3));
        // Start dragging after long press
        mDragDropManager.setInitiateOnLongPress(true);
        mDragDropManager.setInitiateOnMove(false);

        // Swipe manager
        mSwipeManager = new RecyclerViewSwipeManager();

        //Create adapter
        final MainAdapter mainAdapter = new MainAdapter(
                this,
                mDragDropManager,
                getListManipulator(),
                this);

        mAdapter = mainAdapter;
        mWrappedAdapter = mDragDropManager.createWrappedAdapter(mainAdapter);  // Wrap for dragging
        mWrappedAdapter = mSwipeManager.createWrappedAdapter(mWrappedAdapter); // Wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22. Disable the
        // change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mLayoutManager = new MyLinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);
        mRecyclerView.setItemAnimator(animator);

//        // Additional decorations
//        // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            mRecyclerView.addItemDecoration(new ItemShadowDecorator(
//                    (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.material_shadow_z1)));
//        }
        mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(
                ContextCompat.getDrawable(this, R.drawable.list_divider_h), true));

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mSwipeManager.attachRecyclerView(mRecyclerView);
        mDragDropManager.attachRecyclerView(mRecyclerView);
    }

    private void configureAppBarDynamicElevation(){
        // When recyclerView is scrolled all the way to the top, appbar elevation will disappear.
        // When you start scrolling down elevation will reappear.
        if (!mTwoPane) {
            ViewCompat.setElevation(mAppBar, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (recyclerView.computeVerticalScrollOffset() <= 2) {
                            mAppBar.setElevation(0);
                        } else {
                            mAppBar.setElevation(
                                    getResources().getDimension(R.dimen.appbar_elevation));
                        }
                    }
                });
            }
        }
    }

    public ListManipulator getListManipulator() {
        return ((ListManipulatorFragment) getSupportFragmentManager()
                .findFragmentByTag(ListManipulatorFragment.TAG)).getListManipulator();
    }
}
