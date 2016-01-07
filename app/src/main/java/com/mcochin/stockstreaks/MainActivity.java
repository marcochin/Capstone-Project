package com.mcochin.stockstreaks;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.mcochin.stockstreaks.adapters.MainAdapter;
import com.mcochin.stockstreaks.custom.MyApplication;
import com.mcochin.stockstreaks.custom.MyLinearLayoutManager;
import com.mcochin.stockstreaks.data.ListEventQueue;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.events.OnWidgetRefreshEvent;
import com.mcochin.stockstreaks.fragments.DetailFragment;
import com.mcochin.stockstreaks.fragments.ListManagerFragment;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.utils.Utility;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.util.Locale;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity implements SearchBox.SearchListener,
        MainAdapter.EventListener, SwipeRefreshLayout.OnRefreshListener,
        ListManagerFragment.EventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_SEARCH_FOCUSED = "searchFocused";
    private static final String KEY_LOGO_VISIBLE = "logoVisible";

    private RecyclerView mRecyclerView;
    private MyLinearLayoutManager mLayoutManager;
    private MainAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;
    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerViewTouchActionGuardManager mTouchActionGuardManager;

    private EditText mSearchEditText;
    private TextView mLogo;
    private SearchBox mAppBar;
    private SwipeRefreshLayout mSwipeToRefresh;
    private View mRootView;
    private Snackbar mSnackbar;
    private View mProgressWheel;
    private View mEmptyMsg;

    private ListManagerFragment mListFragment;

    private boolean mTwoPane;
    private boolean mFirstOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find our views from xml layouts
        mTwoPane = findViewById(R.id.detail_container) != null;
        mRootView = findViewById(R.id.rootView);
        mProgressWheel = findViewById(R.id.progress_wheel);
        mEmptyMsg = findViewById(R.id.text_empty_list);
        mAppBar = (SearchBox)findViewById(R.id.appBar);
        mLogo = (TextView)findViewById(R.id.logo);
        mSearchEditText = (EditText)findViewById(R.id.search);
        mSwipeToRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_to_refresh);
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);

        if (savedInstanceState == null) {
            mFirstOpen = true;

            // Initialize the fragment that stores the list
            mListFragment = new ListManagerFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(mListFragment, ListManagerFragment.TAG).commit();
            getSupportFragmentManager().executePendingTransactions();

            // We have to generate a new session id, so network calls from previous sessions
            // don't get returned to new sessions
            MyApplication.generateSessionId();

        } else{
            mListFragment = ((ListManagerFragment) getSupportFragmentManager()
                    .findFragmentByTag(ListManagerFragment.TAG));

            // If editText was focused, return that focus on orientation change
            if (savedInstanceState.getBoolean(KEY_SEARCH_FOCUSED)) {
                mAppBar.toggleSearch();
                // Else if not focused, but logo is invisible, open search w/o the focus
            } else if(!savedInstanceState.getBoolean(KEY_LOGO_VISIBLE)){
                mAppBar.toggleSearch();
                mSearchEditText.clearFocus();
            }
        }

        mListFragment.setEventListener(this);
        mSwipeToRefresh.setOnRefreshListener(this);
        mAppBar.setSearchListener(this);

        configureOverflowMenu();
        configureRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        configureDynamicScrollingListener();

        EventBus eventBus = EventBus.getDefault();
        eventBus.registerSticky(mListFragment);
        eventBus.removeAllStickyEvents();

        // Fetch the stock list
        fetchStockList();
    }

    private void fetchStockList(){
        ListEventQueue listEventQueue = ListEventQueue.getInstance();

        if(mFirstOpen) {
            showProgressWheel();
            if (!(listEventQueue.peek() instanceof OnWidgetRefreshEvent)) {
                listEventQueue.clearQueue();
            }
            mListFragment.initFromDb();
        }else{
            listEventQueue.postAllFromQueue();
        }

        if(Utility.canUpdateList(getContentResolver())) {
            if (mFirstOpen && listEventQueue.peek() instanceof OnWidgetRefreshEvent) {
                mListFragment.initFromWidgetRefresh();

            } else if (!MyApplication.getInstance().isRefreshing()) {
                // Make sure it is not refreshing so we don't refresh twice
                refreshShownList(null);
            }
        }
        mFirstOpen = false;
    }

    private void refreshShownList(String attachSymbol){
        // Dismiss Snack-bar to prevent undo removal because the old data will not be in sync with
        // new data when list is refreshed.
        showProgressWheel();

        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
        }
        mListFragment.initFromRefresh(attachSymbol);
    }

    @Override // SwipeRefreshLayout.OnRefreshListener
    public void onRefresh() {
        mSwipeToRefresh.setRefreshing(false);
        //TODO uncomment bottom line. it is only there for testing
        //if(!mListFragment.isRefreshing() && Utility.canUpdateList(getContentResolver())) {
            refreshShownList(null);
        //}
    }

    @Override // ListManipulatorFragment.EventListener
    public void onRefreshFinished(boolean success) {
        if(success) {
            mAdapter.notifyDataSetChanged();
        }
        showEmptyMessageIfPossible();
    }

    @Override // ListManipulatorFragment.EventListener
    public void onWidgetRefresh() {
        showProgressWheel();
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadFromDbFinished() {
        mAdapter.notifyDataSetChanged();
        showEmptyMessageIfPossible();
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadSymbolFinished(boolean success) {
        if(success) {
            mAdapter.notifyItemInserted(0);
            mRecyclerView.smoothScrollToPosition(0);
            mSearchEditText.setText("");
        }
        showEmptyMessageIfPossible();
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadAFewFinished(boolean success) {
        if(success) {
            mAdapter.notifyDataSetChanged();

        }else{
            //Show retry button if there is loading item
            int lastPosition = getListManipulator().getCount() - 1;
            if(lastPosition > -1) {
                Stock lastStock = getListManipulator().getItem(lastPosition);
                if (lastStock.getSymbol().equals(ListManipulator.LOADING_ITEM)) {
                    mAdapter.notifyItemChanged(lastPosition);
                }
            }
        }
        showEmptyMessageIfPossible();
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
            Toast.makeText(this, R.string.toast_empty_search, Toast.LENGTH_SHORT).show();
            return;
        }

        // Refresh the shownList BEFORE fetching a new stock. This is to prevent
        // fetching the new stock twice when it becomes apart of that list.
        if(!MyApplication.getInstance().isRefreshing() && Utility.canUpdateList(getContentResolver())) {
            refreshShownList(query);
        }else{
            mListFragment.loadSymbol(query);
        }

        showProgressWheel();
    }

    @Override // MainAdapter.EventListener
    public void onItemClick(MainAdapter.MainViewHolder holder) {
        int position = holder.getAdapterPosition();

        if(position != RecyclerView.NO_POSITION) {
            Stock stock = getListManipulator().getItem(position);
            Uri detailUri = StockEntry.buildUri(stock.getSymbol());
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.KEY_DETAIL_URI, detailUri);

            if (mTwoPane) {
                //If tablet insert fragment into container
                DetailFragment detailFragment = new DetailFragment();
                detailFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container, detailFragment, DetailFragment.TAG)
                        .commit();

            } else {
                mSearchEditText.clearFocus();
                //If phone open activity
                Intent openDetail = new Intent(MainActivity.this, DetailActivity.class);
                openDetail.setData(detailUri);
                startActivity(openDetail);
            }
        }
    }

    @Override // MainAdapter.EventListener
    public void onItemRetryClick(MainAdapter.MainViewHolder holder) {
        if(Utility.canUpdateList(getContentResolver())){
            Toast.makeText(this, R.string.toast_error_refresh_list, Toast.LENGTH_SHORT).show();

        }else if(!MyApplication.getInstance().isRefreshing()) {
            mListFragment.loadAFew();
            mAdapter.notifyItemChanged(getListManipulator().getCount() - 1);
        }
    }

    @Override // MainAdapter.EventListener
    public void onItemRemoved(MainAdapter.MainViewHolder holder) {
        int position = holder.getAdapterPosition();

        if(position != RecyclerView.NO_POSITION) {
            getListManipulator().removeItem(position);
            mAdapter.notifyItemRemoved(position);

            mSnackbar = Snackbar.make(
                    mRootView,
                    getString(R.string.placeholder_snackbar_main_text, holder.getSymbol()), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action_text, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int position = getListManipulator().undoLastRemoveItem();
                            mAdapter.notifyItemInserted(position);
                            mRecyclerView.smoothScrollToPosition(position);
                        }
                    })
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);
                            new AsyncTask<Void, Void, Void>(){
                                @Override
                                protected Void doInBackground(Void... params) {
                                    getListManipulator().permanentlyDeleteLastRemoveItem(
                                            getContentResolver());
                                    return null;
                                }
                            }.execute();
                        }
                    });
            mSnackbar.show();
            showEmptyMessageIfPossible();
        }
    }

    @Override // MainAdapter.EventListener
    public void onItemMoved(int fromPosition, int toPosition) {
        getListManipulator().moveItem(fromPosition, toPosition);
        mAdapter.notifyItemMoved(fromPosition, toPosition);
    }

    public void configureOverflowMenu(){
        mAppBar.setOverflowMenu(R.menu.menu_main);
        mAppBar.setOverflowMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                switch (id) {
                    case R.id.action_refresh:
                        if (!MyApplication.getInstance().isRefreshing()
                                && Utility.canUpdateList(getContentResolver())) {
                            refreshShownList(null);
                        }
                        break;
                    case R.id.action_sort:
                        break;
                }
                return false;
            }
        });
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

    private void configureDynamicScrollingListener(){
        // When recyclerView is scrolled all the way to the top, appbar elevation will disappear.
        // When you start scrolling down elevation will reappear.
        if (!mTwoPane) {
            ViewCompat.setElevation(mAppBar, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                // This gets called on instantiation, on item add, and on scroll
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (recyclerView.computeVerticalScrollOffset() <= 2) { //0 or 1 not reliable
                            mAppBar.setElevation(0);
                        } else {
                            mAppBar.setElevation(
                                    getResources().getDimension(R.dimen.appbar_elevation));
                        }
                        dynamicLoadAFew();
                    }
                });
            }
        }
    }

    private void dynamicLoadAFew(){
        ListManipulator listManipulator = getListManipulator();

        if (mLayoutManager.findLastVisibleItemPosition() == listManipulator.getCount() - 1
                && !listManipulator.isLoadingItemPresent()
                && !MyApplication.getInstance().isRefreshing()
                && listManipulator.canLoadAFew()) {
            // Insert dummy item
            listManipulator.addLoadingItem();

            if(!MyApplication.getInstance().isLoadingAFew()
                    && !Utility.canUpdateList(getContentResolver())) {
                mListFragment.loadAFew();

            }else{
                Toast.makeText(this, R.string.toast_error_refresh_list, Toast.LENGTH_SHORT).show();
            }
            // Must notifyItemInserted AFTER loadAFew for mIsLoadingAFew to be updated
            mAdapter.notifyItemInserted(listManipulator.getCount() - 1);
        }
    }

    private void showEmptyMessageIfPossible(){
        // because showEmptyMessageIfPossible relies on progress wheel to be invisible, try
        // to hide the progress wheel before calling this method if it is necessary
        hideProgressWheelIfPossible();
        if(getListManipulator().getCount() == 0 && mProgressWheel.getVisibility() == View.INVISIBLE){
            mEmptyMsg.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressWheelIfPossible(){
        if(!Utility.isMainServiceRunning((ActivityManager) getSystemService(ACTIVITY_SERVICE))){
            mProgressWheel.setVisibility(View.INVISIBLE);
        }
    }

    private void hideEmptyMessage(){
        mEmptyMsg.setVisibility(View.INVISIBLE);
    }

    private void showProgressWheel(){
        hideEmptyMessage();
        mProgressWheel.setVisibility(View.VISIBLE);
    }

    public ListManipulator getListManipulator() {
        return mListFragment.getListManipulator();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SEARCH_FOCUSED, mSearchEditText.isFocused());
        outState.putBoolean(KEY_LOGO_VISIBLE, mLogo.getVisibility() == View.VISIBLE);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        mDragDropManager.cancelDrag();

        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if(mRecyclerView != null){
            mRecyclerView.clearOnScrollListeners();
        }
        EventBus.getDefault().unregister(mListFragment);

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
        mListFragment = null;

        super.onDestroy();
    }
}
