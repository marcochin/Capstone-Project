package com.mcochin.stockstreakz;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

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
import com.mcochin.stockstreakz.fragments.MainMenuFragment;

public class MainActivity extends AppCompatActivity implements MainAdapter.EventListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private MyLinearLayoutManager mLayoutManager;
    private MainAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;
    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerViewTouchActionGuardManager mTouchActionGuardManager;
    private ViewGroup mRootView;
    private AppBarLayout mAppBar;
    private View.OnClickListener mSnackBarActionListener;

    private boolean mTwoPane;
    /**
     * When AppBar is expanded it pushes the RecyclerView down. However, for some reason
     * Android still sees the hidden portion of the RecyclerView to be visible.
     * So, when we call findLastCompletelyVisibleItemPosition() from mSnackBarActionListener
     * it will return the position that is hidden from our view(pushed down by appBar). Therefore,
     * to find the ACTUAL lastCompletelyVisibleItem we need to offset the returned position by the
     * number of FAKE completelyVisibleItems.
     */
    private int mListItemsOffsettedByExpandedAppBar = -1;

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

        if (savedInstanceState == null) {
            //Initialize the menu with the SearchView
            getSupportFragmentManager().beginTransaction()
                    .add(new MainMenuFragment(), MainMenuFragment.TAG)
                    .commit();

            //Initialize the fragment that stores the list
            getSupportFragmentManager().beginTransaction()
                    .add(new ListManipulatorFragment(), ListManipulatorFragment.TAG)
                            .commit();

            getSupportFragmentManager().executePendingTransactions();
        }

        mTwoPane = findViewById(R.id.detail_container) != null;

        mRootView = (ViewGroup)findViewById(R.id.rootView);
        mAppBar = (AppBarLayout)findViewById(R.id.appBar);
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mLayoutManager = new MyLinearLayoutManager(this);

        mSnackBarActionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getListManipulator().undoLastRemoveItem();
                mAdapter.notifyItemInserted(position);
                if(mAppBar != null) {
                    mLayoutManager.findLastCompletelyVisibleItemPosition();

                    if(position >= mLayoutManager.findLastCompletelyVisibleItemPosition()
                             - mListItemsOffsettedByExpandedAppBar) {
                        mAppBar.setExpanded(false);
                    }
                }
                mRecyclerView.smoothScrollToPosition(position);
            }
        };

        configureDragNDropAndSwipe();
        configureAppBarDynamicElevation();
    }

    @Override
    public void onItemClick(MainAdapter.MainViewHolder holder) {
        if(mTwoPane){
            //If tablet insert fragment into container
        }else{
            //Need to clear focus to hide the soft keyboard or else fragments come up blank
            MainMenuFragment menuFragment = (MainMenuFragment)getSupportFragmentManager()
                            .findFragmentByTag(MainMenuFragment.TAG);
            menuFragment.clearSearchViewFocus();

            //If phone open activity
            Intent openDetail = new Intent(this, DetailActivity.class);
            startActivity(openDetail);
        }
    }

    @Override
    public void onItemRemoved(MainAdapter.MainViewHolder holder) {
        if(mListItemsOffsettedByExpandedAppBar == -1) {
            int itemPosition = mRecyclerView.getChildLayoutPosition(holder.itemView);
            if(itemPosition != 0){
                mListItemsOffsettedByExpandedAppBar = mAppBar.getHeight() /
                        holder.itemView.getHeight();
            }
        }
        Snackbar.make(mRootView, getString(R.string.snackbar_main_text, holder.getSymbol()), Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_action_text, mSnackBarActionListener).show();
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

    private void configureDragNDropAndSwipe(){
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
                            mAppBar.setElevation(mAppBar.getTargetElevation());
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
