package com.mcochin.stockstreaks;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
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
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.fragments.DetailEmptyFragment;
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
    private static final String KEY_PROGRESS_WHEEL_VISIBLE = "progressWheelVisible";
    private static final String KEY_EMPTY_MSG_VISIBLE = "emptyMsgVisible";
    private static final String KEY_DETAIL_CONTAINER_VISIBLE = "cardViewVisible";

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
    private Snackbar mSnackbar;
    private View mDetailContainer;
    private View mRootView;
    private View mProgressWheel;
    private View mEmptyMsg;
    private ListManagerFragment mListFragment;

    private boolean mFirstOpen;
    private boolean mStartedFromWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find our views from xml layouts
        mDetailContainer = findViewById(R.id.detail_container);
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

            // Execute pending transaction to immediately add the ListManagerFragment because
            // the RecyclerView Adapter is dependent on it.
            getSupportFragmentManager().executePendingTransactions();

            // Checks to see if app is opened from widget
            Uri detailUri = getIntent().getData();
            if(detailUri != null){
                mStartedFromWidget = true;
                String symbol = StockContract.getSymbolFromUri(detailUri);

                if (mDetailContainer != null) {
                    insertFragmentIntoDetailContainer(symbol);
                } else {
                    insertFragmentIntoDetailActivity(symbol);
                }
            }

            //TODO yelllow
//            // Initialize Activity with empty views
            // This will overrider the start from widget card view sooooo
//            showEmptyWidgets();

            // We have to generate a new session so network calls from previous sessions
            // have a chance to cancel themselves
            MyApplication.startNewSession();

        } else{
            mListFragment = ((ListManagerFragment) getSupportFragmentManager()
                    .findFragmentByTag(ListManagerFragment.TAG));

            // If editText was focused, return that focus on orientation change
            if (savedInstanceState.getBoolean(KEY_SEARCH_FOCUSED)) {
                mAppBar.toggleSearch();

            } else if(!savedInstanceState.getBoolean(KEY_LOGO_VISIBLE)){
                // Else if not focused, but logo is invisible, open search w/o the focus
                mAppBar.toggleSearch();
                mSearchEditText.clearFocus();
            }

            if(savedInstanceState.getBoolean(KEY_PROGRESS_WHEEL_VISIBLE)){
                mProgressWheel.setVisibility(View.VISIBLE);

            } else if (savedInstanceState.getBoolean(KEY_EMPTY_MSG_VISIBLE)){
                mEmptyMsg.setVisibility(View.VISIBLE);
            }

            if(savedInstanceState.getBoolean(KEY_DETAIL_CONTAINER_VISIBLE)){
                mDetailContainer.setVisibility(View.VISIBLE);
            }
        }

        mListFragment.setEventListener(this);
        mSwipeToRefresh.setOnRefreshListener(this);
        mAppBar.setSearchListener(this);

        configureOverflowMenu();
        configureRecyclerView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Checks to see if app is opened from widget
        Uri detailUri = intent.getData();
        if(detailUri != null){
            String symbol = StockContract.getSymbolFromUri(detailUri);

            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(symbol);
            } else {
                insertFragmentIntoDetailActivity(symbol);
            }
        }
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
            listEventQueue.clearQueue();
            mListFragment.initFromDb();
        }else{
            listEventQueue.postAllFromQueue();
        }

        if(Utility.canUpdateList(getContentResolver())) {
            if (MyApplication.getInstance().isRefreshing()) {
                showProgressWheel();
            } else{
                // Make sure it is not refreshing so we don't refresh twice
                refreshList(null);
            }
        }
        mFirstOpen = false;
    }

    private void refreshList(String attachSymbol){
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
            refreshList(null);
        //}
    }

    @Override // ListManipulatorFragment.EventListener
    public void onWidgetRefresh() {
        showProgressWheel();
        EventBus.getDefault().removeAllStickyEvents();
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadFromDbFinished() {
        hideProgressWheel();
        mAdapter.notifyDataSetChanged();

        if(getListManipulator().getCount() == 0 ){
            showEmptyWidgets();
        }else if(mDetailContainer != null && !mStartedFromWidget){
            // Load the first item into the container, when db finishes loading
            insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onRefreshFinished(boolean success) {
        hideProgressWheel();
        if(success) {
            mAdapter.notifyDataSetChanged();

            // If tablet, insert fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
            }
        } else{
            if(getListManipulator().getCount() == 0 ){
                showEmptyWidgets();
            }
        }
        EventBus.getDefault().removeAllStickyEvents();
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadSymbolFinished(boolean success) {
        hideProgressWheel();

        if(success) {
            mAdapter.notifyItemInserted(0);
            mRecyclerView.smoothScrollToPosition(0);
            mSearchEditText.setText("");

            // If tablet, insert fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
            }
        } else{
            if(getListManipulator().getCount() == 0 ){
                showEmptyWidgets();
            }
        }
        EventBus.getDefault().removeAllStickyEvents();
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
        EventBus.getDefault().removeAllStickyEvents();
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
            refreshList(query);
        }else{
            mListFragment.loadSymbol(query);
        }

        showProgressWheel();
    }

    @Override // MainAdapter.EventListener
    public void onItemClick(MainAdapter.MainViewHolder holder) {
        int position = holder.getAdapterPosition();

        if(position != RecyclerView.NO_POSITION) {
            String symbol = getListManipulator().getItem(position).getSymbol();

            //If tablet insert fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(symbol);
            } else {
                insertFragmentIntoDetailActivity(symbol);
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
            final ListManipulator listManipulator = getListManipulator();
            listManipulator.removeItem(position);
            mAdapter.notifyItemRemoved(position);

            mSnackbar = Snackbar.make(
                    mRootView,
                    getString(R.string.placeholder_snackbar_main_text, holder.getSymbol()), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action_text, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideEmptyMessage();
                            int position = listManipulator.undoLastRemoveItem();
                            mAdapter.notifyItemInserted(position);
                            mRecyclerView.smoothScrollToPosition(position);

                            if(mDetailContainer != null) {;
                                insertFragmentIntoDetailContainer(
                                        listManipulator.getItem(position).getSymbol());
                            }
                        }
                    })
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);
                            new AsyncTask<Void, Void, Void>(){
                                @Override
                                protected Void doInBackground(Void... params) {
                                    listManipulator.permanentlyDeleteLastRemoveItem(
                                            getContentResolver());
                                    return null;
                                }
                            }.execute();
                        }
                    });
            mSnackbar.show();

            if(listManipulator.getCount() == 0){
                showEmptyWidgets();
            }else if(mDetailContainer != null){
                insertFragmentIntoDetailContainer(listManipulator.getItem(0).getSymbol());
            }
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
                            refreshList(null);
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
                mListFragment,
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

        //TODO yelllow
//        ViewCompat.setElevation(mAppBar, 0); not sure if we need this comment out for now

        // This gets called on instantiation, on item add, and on scroll
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mDetailContainer == null) {
                    if (recyclerView.computeVerticalScrollOffset() <= 2) { //0 or 1 not reliable
                        mAppBar.setElevation(0);
                    } else {
                        mAppBar.setElevation(
                                getResources().getDimension(R.dimen.appbar_elevation));
                    }
                }
                dynamicLoadAFew();
            }
        });
    }

    private void dynamicLoadAFew(){
        ListManipulator listManipulator = getListManipulator();

        if (mLayoutManager.findLastVisibleItemPosition() == listManipulator.getCount() - 1
                && !listManipulator.isLoadingItemPresent()
                && !MyApplication.getInstance().isRefreshing()
                && listManipulator.canLoadAFew()) {
            // Insert dummy item
            listManipulator.addLoadingItem();

            if(!Utility.canUpdateList(getContentResolver())) {
                mListFragment.loadAFew();
            }else{
                Toast.makeText(this, R.string.toast_error_refresh_list, Toast.LENGTH_SHORT).show();
            }
            // Must notifyItemInserted AFTER loadAFew for mIsLoadingAFew to be updated
            mAdapter.notifyItemInserted(listManipulator.getCount() - 1);
        }
    }

    private void insertFragmentIntoDetailContainer(@NonNull String symbol){
        Uri detailUri = StockEntry.buildUri(symbol);
        Bundle args = new Bundle();
        args.putParcelable(DetailFragment.KEY_DETAIL_URI, detailUri);

        DetailFragment detailFragment = new DetailFragment();
        detailFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, detailFragment, DetailFragment.TAG)
                .commit();
    }

    private void insertFragmentIntoDetailActivity(@NonNull String symbol){
        mSearchEditText.clearFocus();
        Uri detailUri = StockEntry.buildUri(symbol);
        //If phone open activity
        Intent openDetail = new Intent(MainActivity.this, DetailActivity.class);
        openDetail.setData(detailUri);
        startActivity(openDetail);
    }

    private void showEmptyWidgets(){
        showEmptyMessage();
        if(mDetailContainer != null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new DetailEmptyFragment(), DetailFragment.TAG)
                    .commit();
        }
    }

    private void showEmptyMessage(){
        if(mProgressWheel.getVisibility() == View.INVISIBLE){
            mEmptyMsg.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyMessage(){
        mEmptyMsg.setVisibility(View.INVISIBLE);
    }

    private void showProgressWheel() {
        mEmptyMsg.setVisibility(View.INVISIBLE);
        mProgressWheel.setVisibility(View.VISIBLE);
    }

    private void hideProgressWheel(){
        if(!Utility.isMainServiceRunning((ActivityManager) getSystemService(ACTIVITY_SERVICE))){
            mProgressWheel.setVisibility(View.INVISIBLE);
        }
    }

    public ListManipulator getListManipulator() {
        if(mListFragment != null) {
            return mListFragment.getListManipulator();
        }

        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SEARCH_FOCUSED, mSearchEditText.isFocused());
        outState.putBoolean(KEY_LOGO_VISIBLE, mLogo.getVisibility() == View.VISIBLE);
        outState.putBoolean(KEY_PROGRESS_WHEEL_VISIBLE, mProgressWheel.getVisibility() == View.VISIBLE);
        outState.putBoolean(KEY_EMPTY_MSG_VISIBLE, mEmptyMsg.getVisibility() == View.VISIBLE);
        if(mDetailContainer != null) {
            outState.putBoolean(KEY_DETAIL_CONTAINER_VISIBLE,
                    mDetailContainer.getVisibility() == View.VISIBLE);
        }
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
