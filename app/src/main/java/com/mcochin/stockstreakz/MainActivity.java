package com.mcochin.stockstreakz;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.mcochin.stockstreakz.adapters.MainAdapter;
import com.mcochin.stockstreakz.custom.MyLinearLayoutManager;
import com.mcochin.stockstreakz.data.ListManipulator;
import com.mcochin.stockstreakz.fragments.ListManipulatorFragment;
import com.quinny898.library.persistentsearch.SearchBox;

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
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_test);

        mTwoPane = findViewById(R.id.detail_container) != null;

        mRootView = findViewById(R.id.rootView);
        mAppBar = (SearchBox)findViewById(R.id.appBar);
        mSearchEditText = (EditText)findViewById(R.id.search);
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mLayoutManager = new MyLinearLayoutManager(this);

        if (savedInstanceState == null) {
            //Initialize the fragment that stores the list
            getSupportFragmentManager().beginTransaction()
                    .add(new ListManipulatorFragment(), ListManipulatorFragment.TAG).commit();

            getSupportFragmentManager().executePendingTransactions();
        } else{
            if (savedInstanceState.getBoolean(KEY_SEARCH_FOCUSED)) {
                mAppBar.openSearch(true);
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

        configureAppBar();
        configureDragDropAndSwipe();
        configureAppBarDynamicElevation();
    }

    @Override
    public void onItemClick(MainAdapter.MainViewHolder holder) {
        if(mTwoPane){
            //If tablet insert fragment into container
        }else{
            hideKeyboard();

            //If phone open activity
            Intent openDetail = new Intent(this, DetailActivity.class);
            startActivity(openDetail);
        }
    }

    @Override
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
    protected void onStop() {
        hideKeyboard();
        super.onStop();
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

    private void configureDragDropAndSwipe(){
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
                new MainAdapter(this, mDragDropManager, getListManipulator());
        mAdapter = mainAdapter;

        mWrappedAdapter = mDragDropManager.createWrappedAdapter(mainAdapter);      // Wrap for dragging
        mWrappedAdapter = mSwipeManager.createWrappedAdapter(mWrappedAdapter);      // Wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

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

    private void configureAppBar(){
        mAppBar.setHint(getString(R.string.search_hint));
        mAppBar.setLogoText(getString(R.string.app_name));
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

    private void hideKeyboard(){
        View view = getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
