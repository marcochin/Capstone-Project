package com.mcochin.stockstreaks;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.mcochin.stockstreaks.adapters.MainAdapter;
import com.mcochin.stockstreaks.custom.MyLinearLayoutManager;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.fragments.ListManipulatorFragment;
import com.mcochin.stockstreaks.services.NetworkService;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

public class MainActivity extends AppCompatActivity implements MainAdapter.EventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_SEARCH_FOCUSED = "searchFocused";

    private RecyclerView mRecyclerView;
    private MyLinearLayoutManager mLayoutManager;
    private MainAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;
    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerViewTouchActionGuardManager mTouchActionGuardManager;
    private SearchBox mAppBar;
    private EditText mSearchEditText;
    private View mRootView;
    private View.OnClickListener mSnackBarActionListener;

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTwoPane = findViewById(R.id.detail_container) != null;

        mRootView = findViewById(R.id.rootView);
        mAppBar = (SearchBox)findViewById(R.id.appBar);
        mSearchEditText = (EditText)findViewById(R.id.search);
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);

        if (savedInstanceState == null) {
            //Initialize the fragment that stores the list
            getSupportFragmentManager().beginTransaction()
                    .add(new ListManipulatorFragment(), ListManipulatorFragment.TAG).commit();

            getSupportFragmentManager().executePendingTransactions();
        } else{
            if (savedInstanceState.getBoolean(KEY_SEARCH_FOCUSED)) {
                mAppBar.toggleSearch();
            }
        }

        mSnackBarActionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getListManipulator().undoLastRemoveItem();
                mAdapter.notifyItemInserted(position);
                mRecyclerView.smoothScrollToPosition(position);
            }
        };

        mAppBar.setOverflowMenu(R.menu.menu_main);
        mAppBar.setSearchListener(new SearchBox.SearchListener() {
            @Override
            public void onSearchOpened() {

            }

            @Override
            public void onSearchCleared() {

            }

            @Override
            public void onSearchClosed() {

            }

            @Override
            public void onSearchTermChanged(String term) {

            }

            @Override
            public void onSearch(String query) {
                // TODO start service, start cursor loader when service fetches data and uses
                // TODO content provider to put in the db.
                // TODO Cursor loader will detect it and then update ui.
                if(TextUtils.isEmpty(query)){
                    Toast.makeText(getBaseContext(), R.string.toast_empty_search, Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent serviceIntent = new Intent(getBaseContext(), NetworkService.class);
                serviceIntent.putExtra(NetworkService.KEY_SEARCH_QUERY, query);
                startService(serviceIntent);
            }

            @Override
            public void onResultClick(SearchResult result) {

            }
        });

        configureRecyclerView();
        configureAppBarDynamicElevation();
    }

    @Override // MainAdapter.EventListener
    public void onItemClick(MainAdapter.MainViewHolder holder) {
        if(mTwoPane){
            //If tablet insert fragment into container
        }else{
            //If phone open activity
            Intent openDetail = new Intent(this, DetailActivity.class);
            startActivity(openDetail);
        }
    }

    @Override // MainAdapter.EventListener
    public void onItemRemoved(MainAdapter.MainViewHolder holder) {
        Snackbar.make(mRootView, getString(R.string.snackbar_main_text, holder.getSymbol()), Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_action_text, mSnackBarActionListener).show();
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
        mAdapter.removeListManipulator();
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

        final MainAdapter mainAdapter =
                new MainAdapter(this, this, mDragDropManager, getListManipulator());
        mAdapter = mainAdapter;

        mWrappedAdapter = mDragDropManager.createWrappedAdapter(mainAdapter);      // Wrap for dragging
        mWrappedAdapter = mSwipeManager.createWrappedAdapter(mWrappedAdapter);      // Wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mLayoutManager = new MyLinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);
        mRecyclerView.setItemAnimator(animator);

        // Additional decorations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.material_shadow_z1)));
        }
        mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(this, R.drawable.list_divider_h), true));

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
        if (mAppBar != null) {
            ViewCompat.setElevation(mAppBar, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (recyclerView.computeVerticalScrollOffset() <= 2) {
                            mAppBar.setElevation(0);
                        } else {
                            mAppBar.setElevation(getResources()
                                    .getDimension(R.dimen.appbar_elevation));
                        }
                    }
                });
            }
        }
    }

    public ListManipulator getListManipulator() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(ListManipulatorFragment.TAG);
        return ((ListManipulatorFragment) fragment).getListManipulator();
    }
}
