package com.mcochin.stockstreaks;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
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
import com.mcochin.stockstreaks.fragments.dialogs.AboutDialog;
import com.mcochin.stockstreaks.fragments.dialogs.FaqDialog;
import com.mcochin.stockstreaks.fragments.dialogs.SortDialog;
import com.mcochin.stockstreaks.pojos.events.AppRefreshFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.InitLoadFromDbFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.LoadMoreFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.pojos.events.WidgetRefreshDelegateEvent;
import com.mcochin.stockstreaks.services.MainService;
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
    private static final String KEY_DYNAMIC_SCROLL_ENABLED = "dynamicScrollEnabled";
    private static final String KEY_DYNAMIC_LOAD_ANOTHER = "dynamicLoadAnother";
    private static final String KEY_ITEM_CLICKS_FOR_INTERSTITIAL = "ItemClicksForInterstitial";

    private static final int CLICKS_UNTIL_INTERSTITIAL = 10;

    private RecyclerView mRecyclerView;
    private MyLinearLayoutManager mLayoutManager;
    private MainAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;
    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerViewTouchActionGuardManager mTouchActionGuardManager;

    private DrawerLayout mDrawerLayout;
    private EditText mSearchEditText;
    private TextView mSearchLogo;
    private SearchBox mToolbar;
    private SwipeRefreshLayout mSwipeToRefresh;
    private Snackbar mSnackbar;
    private View mDetailContainer;
    private View mCoordinatorLayout;
    private View mProgressWheel;
    private View mEmptyMsg;
    private ListManagerFragment mListFragment;

    private View mOverflowMenuButton;
    private PopupWindow mOverflowMenuPopUp;
    private View.OnClickListener mOverflowItemListener;

    private boolean mFirstOpen;
    private boolean mStartedFromWidget;
    private boolean mDragClickPreventionEnabled;
    private boolean mDynamicScrollLoadEnabled;
    private boolean mDynamicScrollLoadAnother;
    private int mNumberOfLaunchItems;

    private InterstitialAd mInterstitialAd;
    private int mItemClicksForInterstitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find our views from the xml layout
        mEmptyMsg = findViewById(R.id.text_empty_list);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDetailContainer = findViewById(R.id.detail_container);
        mSearchLogo = (TextView) findViewById(R.id.search_box_logo);
        mOverflowMenuButton = findViewById(R.id.overflow);
        mProgressWheel = findViewById(R.id.progress_wheel);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mSearchEditText = (EditText) findViewById(R.id.edit_text_search);
        mSwipeToRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_to_refresh);
        mToolbar = (SearchBox) findViewById(R.id.search_box);

        // Init savedInstanceState first as many components rely on it
        initSavedInstanceState(savedInstanceState);
        initNavigationMenu();
        initOverflowMenu();
        initRecyclerView();
//        initInterstitialAd();

        mListFragment.setEventListener(this);
        mSwipeToRefresh.setOnRefreshListener(this);
        mToolbar.setSearchListener(this);

        mNumberOfLaunchItems = getResources().getInteger(R.integer.numberOfLaunchItems);
    }

    /**
     * Initializes variables depending on the state of savedInstanceState
     *
     * @param savedInstanceState
     */
    private void initSavedInstanceState(Bundle savedInstanceState) {
        // When Android kills this app because of low memory check if sessionId is empty. If it is,
        // start the app as if it were the first time.
        if(MyApplication.getInstance().getSessionId().isEmpty()){
            savedInstanceState = null;
        }
        mListFragment = ((ListManagerFragment) getSupportFragmentManager()
                .findFragmentByTag(ListManagerFragment.TAG));

        if (savedInstanceState == null) {
            mFirstOpen = true;
            // We have to generate a new session so network calls from previous sessions
            // have a chance to cancel themselves
            MyApplication.startNewSession();

            // Initialize the fragment that stores the list
            if(mListFragment == null) {
                mListFragment = new ListManagerFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(mListFragment, ListManagerFragment.TAG).commit();
            }

            // Execute pending transaction to immediately add the ListManagerFragment because
            // the RecyclerView Adapter is dependent on it.
            getSupportFragmentManager().executePendingTransactions();

            // Checks to see if app is opened from widget
            Uri detailUri = getIntent().getData();
            if (detailUri != null) {
                mStartedFromWidget = true;
                String symbol = StockContract.getSymbolFromUri(detailUri);

                if (mDetailContainer != null) {
                    insertFragmentIntoDetailContainer(symbol);
                }
            }
            if (!mStartedFromWidget && mDetailContainer != null) {
                showDetailEmptyFragment();
            }
        }

        // saveInstanceState != null
        else {
            // If editText was focused, return that focus on orientation change
            if (savedInstanceState.getBoolean(KEY_SEARCH_FOCUSED)) {
                mToolbar.toggleSearch();

            } else if (!savedInstanceState.getBoolean(KEY_LOGO_VISIBLE)) {
                // Else if not focused, but logo is invisible, open search w/o the focus
                mToolbar.toggleSearch();
                mSearchEditText.clearFocus();
            }

            if (savedInstanceState.getBoolean(KEY_PROGRESS_WHEEL_VISIBLE)) {
                mProgressWheel.setVisibility(View.VISIBLE);

            } else if (savedInstanceState.getBoolean(KEY_EMPTY_MSG_VISIBLE)) {
                mEmptyMsg.setVisibility(View.VISIBLE);
            }

            mDynamicScrollLoadEnabled = savedInstanceState.getBoolean(KEY_DYNAMIC_SCROLL_ENABLED);
            mDynamicScrollLoadAnother = savedInstanceState.getBoolean(KEY_DYNAMIC_LOAD_ANOTHER);
            mItemClicksForInterstitial = savedInstanceState.getInt(KEY_ITEM_CLICKS_FOR_INTERSTITIAL);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Checks to see if app is opened from widget
        // On Phone --> DetailActivity, onCreate null, no onNewIntent. Click back to MainActivity, onCreate null, onNewIntent called.
        // On Phone & Tablet --> MainActivity, onNewIntent called only if already opened. If not onCreate null only.
        //
        // If the parent activity has launch mode <singleTop>, or the up intent contains
        // FLAG_ACTIVITY_CLEAR_TOP, the parent activity is brought to the top of the stack, and
        // receives the intent through its onNewIntent() method.
        Uri detailUri = intent.getData();
        if (detailUri != null) {
            String symbol = StockContract.getSymbolFromUri(detailUri);

            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(symbol);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initDynamicScrollListener();

        EventBus eventBus = EventBus.getDefault();
        eventBus.registerSticky(mListFragment);

        // Fetch the stock list
        fetchStockList();
    }

    /**
     * Grabs the stock list from the db when app is first opened and updates it if possible.
     */
    private void fetchStockList() {
        ListEventQueue listEventQueue = ListEventQueue.getInstance();

        if (mFirstOpen) {
            mDynamicScrollLoadAnother = false;
            showProgressWheel();
            listEventQueue.clearQueue();
            mListFragment.initFromDb();

        } else {
            listEventQueue.postAllFromQueue();
            refreshList(null, false);
        }
        mFirstOpen = false;
    }

    /**
     * Sends a request to refresh the list.
     *
     * @param attachSymbol The symbol to load after the list has been refreshed. This is to ensure
     *                     already loaded symbols will be in sync with the new symbol.
     * @param showUpToDateToast set to true if you would like a toast to notify if you're already up to
     *                          date.
     * @return true if list can be refreshed, false otherwise.
     */
    private boolean refreshList(String attachSymbol, boolean showUpToDateToast) {
        if (MyApplication.getInstance().isRefreshing()) {
            showProgressWheel();

        }else if(Utility.canUpdateList(getContentResolver())){
            // We set Dynamic scroll to false here as early as possible as a precaution to prevent
            // load as we don't setRefreshing to true until initFromRefresh();
            mDynamicScrollLoadEnabled = false;
            mDynamicScrollLoadAnother = false;
            showProgressWheel();

            // Dismiss Snack-bar to prevent undo removal because the old data will not be in sync with
            // new data when list is refreshed.
            if (mSnackbar != null && mSnackbar.isShown()) {
                mSnackbar.dismiss();
            }
            mListFragment.initFromRefresh(attachSymbol);
            return true;

        }else if(showUpToDateToast){
            Toast.makeText(this, R.string.toast_list_is_up_to_date, Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    @Override // SwipeRefreshLayout.OnRefreshListener
    public void onRefresh() {
        mSwipeToRefresh.setRefreshing(false);
        refreshList(null, true);
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
        if (!getListManipulator().isListLimitReached(MainActivity.this)) {
            query = query.toUpperCase(Locale.US);

            if (TextUtils.isEmpty(query)) {
                Toast.makeText(this, R.string.toast_empty_search, Toast.LENGTH_SHORT).show();

            } else if(!refreshList(query, false)){
                // Refresh the shownList BEFORE fetching a new stock. This is to prevent
                // fetching the new stock twice when it becomes apart of that list.
                showProgressWheel();
                mListFragment.loadSymbol(query);
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadFromDbFinished(InitLoadFromDbFinishedEvent event) {
        hideProgressWheel();
        mAdapter.notifyDataSetChanged();

        if (getListManipulator().getCount() == 0) {
            showEmptyMessage();

        } else if (mDetailContainer != null && !mStartedFromWidget) {
            // Load the first item into the container, when db finishes loading
            insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
        }

        if (!refreshList(null, false) && getListManipulator().getCount() < mNumberOfLaunchItems) {
            dynamicLoadMore();
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadSymbolFinished(LoadSymbolFinishedEvent event) {
        hideProgressWheel();

        if (event.isSuccessful()) {
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                // Can't use notifyDataInserted(...) because we are suppose to update adapter size
                // AND notifyItem...() in the main thread and same call stack. We happen to update
                // adapter size in a bg thread. Also another problem is when we rotate and we post
                // all from queue, we will call notifyItemInserted w/o inserting an item.
                // But we can use notifyDataSetChanged() to work around that.
                // http://stackoverflow.com/questions/30220771/recyclerview-inconsistency-detected-invalid-item-position
                mAdapter.notifyDataSetChanged();
                mRecyclerView.smoothScrollToPosition(0);
            }
            mSearchEditText.setText("");

            // If tablet, insert fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
            }

            // Send to analytics what symbols were added.
//            MyApplication.getInstance().getDefaultTracker().send(new HitBuilders.EventBuilder()
//                    .setCategory(getString(R.string.analytics_category))
//                    .setAction(getString(R.string.analytics_action_add))
//                    .setLabel(getString(R.string.analytics_label_add_placeholder,
//                            event.getStock().getSymbol()))
//                    .build());
        } else {
            if (getListManipulator().getCount() == 0) {
                showEmptyWidgets();
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadMoreFinished(LoadMoreFinishedEvent event) {
        if (event.isSuccessful()) {
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                mAdapter.notifyDataSetChanged();
            }

            // Load more only if number of launch items are not reached or if we have the signal to
            // load another
            if (getListManipulator().getCount() < mNumberOfLaunchItems || mDynamicScrollLoadAnother) {
                dynamicLoadMore();
                mDynamicScrollLoadAnother = false;
            } else {
                mDynamicScrollLoadEnabled = true;
            }

        } else {
            //Show retry button
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                mAdapter.notifyItemChanged(getListManipulator().getCount() - 1);
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onRefreshFinished(AppRefreshFinishedEvent event) {
        hideProgressWheel();
        if (event.isSuccessful()) {
            mAdapter.notifyDataSetChanged();
            dynamicLoadMore();

            // If tablet, insert first item's fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
            }

        } else {
            if (getListManipulator().getCount() == 0) {
                showEmptyWidgets();
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onWidgetRefreshDelegate(WidgetRefreshDelegateEvent event) {
        showProgressWheel();
    }

    @Override // MainAdapter.EventListener
    public void onStockItemClick(MainAdapter.MainViewHolder holder) {
        if (!mDragClickPreventionEnabled) {
            int position = holder.getAdapterPosition();

            if (position != RecyclerView.NO_POSITION) {
                String symbol = getListManipulator().getItem(position).getSymbol();

                //If tablet insert fragment into container
                if (mDetailContainer != null) {
                    String detailSymbol = ((DetailFragment) getSupportFragmentManager()
                            .findFragmentByTag(DetailFragment.TAG)).getSymbol();

                    // If the symbol's detail fragment is already loaded, don't load again if
                    // clicked again.
                    if(!detailSymbol.equals(symbol)) {
                        insertFragmentIntoDetailContainer(symbol);
                    }
                } else {
                    insertFragmentIntoDetailActivity(symbol);
                }

                // Check if it is time to show interstitial ad
//                if (mItemClicksForInterstitial >= CLICKS_UNTIL_INTERSTITIAL
//                        && mInterstitialAd.isLoaded()) {
//                    mInterstitialAd.show();
//                }
//                mItemClicksForInterstitial++;
            }
        }
    }

    @Override // MainAdapter.EventListener
    public void onStockItemRemoved(MainAdapter.MainViewHolder holder) {
        int position = holder.getAdapterPosition();

        if (position != RecyclerView.NO_POSITION) {
            final ListManipulator listManipulator = getListManipulator();
            String removeSymbol = listManipulator.getItem(position).getSymbol();
            // Delete the lastRemovedItem before, removing another item. Can't rely on Snackbar's
            // onDismissed() callback because it only gets called AFTER this method is finished.
            listManipulator.permanentlyDeleteLastRemoveItem(MainActivity.this);
            listManipulator.removeItem(position);

            if (listManipulator.getCount() == 0) {
                showEmptyWidgets();

            } else if (mDetailContainer != null) {
                // Show the first item in the detail container if the one being removed is
                // currently in the detail container.
                String detailSymbol = ((DetailFragment) getSupportFragmentManager()
                        .findFragmentByTag(DetailFragment.TAG)).getSymbol();

                if (detailSymbol.equals(removeSymbol)) {
                    insertFragmentIntoDetailContainer(listManipulator.getItem(0).getSymbol());
                }
            }

            // Snack-bar to give the option to undo the removal of an item or else it will
            // permanently delete from database once it is dismissed.
            // Note: Snack-bar onDismiss callback follow weird order of execution. It doesn't get
            // called when you expect it to.
            mSnackbar = Snackbar.make(
                    mCoordinatorLayout,
                    getString(R.string.placeholder_snackbar_main_text, removeSymbol), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action_text, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!getListManipulator().isListLimitReached(MainActivity.this)) {
                                hideEmptyMessage();

                                int undoPosition = listManipulator.undoLastRemoveItem();
                                mAdapter.notifyItemInserted(undoPosition);
                                mRecyclerView.smoothScrollToPosition(undoPosition);

                                if (mDetailContainer != null) {
                                    insertFragmentIntoDetailContainer(
                                            listManipulator.getItem(undoPosition).getSymbol());
                                }
                            }
                        }
                    }).setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION
                                    && event != Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) {
                                listManipulator.permanentlyDeleteLastRemoveItem(MainActivity.this);
                            }
                        }
                    });
            mSnackbar.show();

        }
    }

    @Override // MainAdapter.EventListener
    public void onStockItemMoved(int fromPosition, int toPosition) {
        getListManipulator().moveItem(fromPosition, toPosition);
    }

    @Override // MainAdapter.EventListener
    public void onStockItemTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDragClickPreventionEnabled = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mDragDropManager != null && !mDragDropManager.isDragging()) {
                    mDragDropManager.cancelDrag();
                }
                break;
        }
    }

    @Override // MainAdapter.EventListener
    public void onLoadItemRetryClick(MainAdapter.LoadViewHolder holder) {
        if (Utility.canUpdateList(getContentResolver())) {
            Toast.makeText(this, R.string.toast_error_refresh_list, Toast.LENGTH_SHORT).show();

        } else if (!MyApplication.getInstance().isRefreshing()) {
            mListFragment.loadMore();
            mAdapter.notifyItemChanged(getListManipulator().getCount() - 1);
        }
    }

    /**
     * Initializes the navigation menu.
     */
    private void initNavigationMenu() {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);

        mToolbar.setMenuListener(new SearchBox.MenuListener() {
            @Override
            public void onMenuClick() {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Setup navigation menu clicks here
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.navigation_faq:
                        new FaqDialog().show(getSupportFragmentManager(), FaqDialog.TAG);
                        break;

                    case R.id.navigation_about:
                        new AboutDialog().show(getSupportFragmentManager(), AboutDialog.TAG);
                        break;
                }
                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    /**
     * Initializes the overflow menu.
     */
    private void initOverflowMenu() {
        mToolbar.setOverflowMenuClickLister(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater layoutInflater = (LayoutInflater) getBaseContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                View overflowLayout = layoutInflater.inflate(R.layout.overflow_menu_custom, null);

                mOverflowMenuPopUp = new PopupWindow(overflowLayout,
                        getResources().getDimensionPixelSize(R.dimen.overflow_menu_width),
                        ViewGroup.LayoutParams.WRAP_CONTENT, true);

                // A workaround to dismiss PopUpWindow to be dismissed when touched outside.
                mOverflowMenuPopUp.setBackgroundDrawable(new ColorDrawable());

                int cardViewCompatPaddingVerticalOffset =
                        getResources().getDimensionPixelSize(R.dimen.overflow_menu_compat_padding);
                // Horizontal Offset is 0 because no matter how much we offset it will not offset any
                // more to the right since we set compat padding to true on the
                // menu card view. That extra padding will not go off screen since it is part of the
                // menu now and Android wants to fit the entire view on screen.
                mOverflowMenuPopUp.showAsDropDown(mOverflowMenuButton, 0, -cardViewCompatPaddingVerticalOffset);

                overflowLayout.findViewById(R.id.overflow_refresh)
                        .setOnClickListener(mOverflowItemListener);
                overflowLayout.findViewById(R.id.overflow_sort)
                        .setOnClickListener(mOverflowItemListener);
            }
        });

        // Setup overflow menu clicks here
        mOverflowItemListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.overflow_refresh:
                        refreshList(null, true);
                        break;

                    case R.id.overflow_sort:
                        SortDialog.newInstance(new SortDialog.OnSortFinishedListener() {
                            @Override
                            public void onSortFinished() {
                                mAdapter.notifyDataSetChanged();
                            }
                        }).show(getSupportFragmentManager(), SortDialog.TAG);
                        break;
                }

                mOverflowMenuPopUp.dismiss();
            }
        };
    }

    /**
     * Initializes the {@link RecyclerView}.
     */
    private void initRecyclerView() {
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
        mDragDropManager.setOnItemDragEventListener(new RecyclerViewDragDropManager.OnItemDragEventListener() {
            @Override
            public void onItemDragStarted(int position) {
                mDragClickPreventionEnabled = true;
            }

            @Override
            public void onItemDragPositionChanged(int fromPosition, int toPosition) {
            }

            @Override
            public void onItemDragFinished(int fromPosition, int toPosition, boolean result) {
                mAdapter.notifyDataSetChanged();
            }
        });

        // Swipe manager
        mSwipeManager = new RecyclerViewSwipeManager();
        mSwipeManager.setOnItemSwipeEventListener(new RecyclerViewSwipeManager.OnItemSwipeEventListener() {
            @Override
            public void onItemSwipeStarted(int position) {
            }

            @Override
            public void onItemSwipeFinished(int position, int result, int afterSwipeReaction) {
                mAdapter.notifyDataSetChanged();
            }
        });

        //Create adapter
        final MainAdapter mainAdapter = new MainAdapter(this, this, mListFragment);

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

    /**
     * Initializes the RecyclerView's Scroll Listener that changes elevation of the Toolbar
     * depending on the scroll offset. Also it will attempt to load more data when the last item
     * if the list is shown.
     */
    private void initDynamicScrollListener() {
        // When recyclerView is scrolled all the way to the top, appbar elevation will disappear.
        // When you start scrolling down elevation will reappear.

        // This gets called on instantiation, on item add, and on scroll
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mDetailContainer == null) {
                    if (recyclerView.computeVerticalScrollOffset() <= 2) { //0 or 1 not reliable
                        mToolbar.setElevation(0);
                    } else {
                        mToolbar.setElevation(
                                getResources().getDimension(R.dimen.appbar_elevation));
                    }
                }
                if (mDynamicScrollLoadEnabled
                        && mLayoutManager.findLastVisibleItemPosition() == getListManipulator().getCount() - 1) {
                    dynamicLoadMore();
                }
            }
        });
    }

    /**
     * Loads more data to the bottom of your list.
     */
    private void dynamicLoadMore() {
        ListManipulator listManipulator = getListManipulator();

        if (!listManipulator.isLoadingItemPresent()
                && !MyApplication.getInstance().isRefreshing()
                && listManipulator.canLoadMore()) {
            // Insert loading item
            listManipulator.addLoadingItem();

            if (!Utility.canUpdateList(getContentResolver())) {
                mListFragment.loadMore();
            } else {
                Toast.makeText(this, R.string.toast_error_refresh_list, Toast.LENGTH_SHORT).show();
            }

            // Must notifyItemInserted AFTER loadMore for mIsLoadingAFew to be updated
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                mAdapter.notifyItemInserted(listManipulator.getCount() - 1);
            }

            // When dynamic scroll is enabled and it load we will load twice. We immediately disable
            // dynamic scroll as a precaution to prevent overlapping of loads. We will re-enable
            // after callback is called.
            if (mDynamicScrollLoadEnabled) {
                mDynamicScrollLoadAnother = true;
                mDynamicScrollLoadEnabled = false;
            }
        }
    }

    /**
     * Inserts a {@link DetailFragment} containing the symbol's details into the Detail Container.
     *
     * @param symbol The symbol to insert the fragment for.
     */
    private void insertFragmentIntoDetailContainer(@NonNull String symbol) {
        Uri detailUri = StockEntry.buildUri(symbol);
        Bundle args = new Bundle();
        args.putParcelable(DetailFragment.KEY_DETAIL_URI, detailUri);

        DetailFragment detailFragment = new DetailFragment();
        detailFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, detailFragment, DetailFragment.TAG)
                .commit();
    }

    /**
     * Inserts a {@link DetailFragment} containing the symbol's details into the
     * {@link DetailActivity}.
     *
     * @param symbol The symbol to insert the fragment for.
     */
    private void insertFragmentIntoDetailActivity(@NonNull String symbol) {
        mSearchEditText.clearFocus();
        Uri detailUri = StockEntry.buildUri(symbol);
        //If phone open activity
        Intent openDetail = new Intent(MainActivity.this, DetailActivity.class);
        openDetail.setData(detailUri);

        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle();
        startActivity(openDetail, bundle);
    }

    public ListManipulator getListManipulator() {
        if (mListFragment != null) {
            return mListFragment.getListManipulator();
        }
        return null;
    }

    private void showEmptyWidgets() {
        showEmptyMessage();
        if (mDetailContainer != null) {
            showDetailEmptyFragment();
        }
    }

    private void showDetailEmptyFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, new DetailEmptyFragment(), DetailFragment.TAG)
                .commit();
    }

    private void showEmptyMessage() {
        if (mProgressWheel.getVisibility() == View.INVISIBLE) {
            mEmptyMsg.setVisibility(View.VISIBLE);
        }
    }

    private void showProgressWheel() {
        mEmptyMsg.setVisibility(View.INVISIBLE);
        mProgressWheel.setVisibility(View.VISIBLE);
    }

    private void hideEmptyMessage() {
        mEmptyMsg.setVisibility(View.INVISIBLE);
    }

    private void hideProgressWheel() {
        if (!Utility.isServiceRunning((ActivityManager) getSystemService(ACTIVITY_SERVICE),
                MainService.class.getName())) {
            mProgressWheel.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SEARCH_FOCUSED, mSearchEditText.isFocused());
        outState.putBoolean(KEY_LOGO_VISIBLE, mSearchLogo.getVisibility() == View.VISIBLE);
        outState.putBoolean(KEY_PROGRESS_WHEEL_VISIBLE, mProgressWheel.getVisibility() == View.VISIBLE);
        outState.putBoolean(KEY_EMPTY_MSG_VISIBLE, mEmptyMsg.getVisibility() == View.VISIBLE);
        outState.putBoolean(KEY_DYNAMIC_SCROLL_ENABLED, mDynamicScrollLoadEnabled);
        outState.putBoolean(KEY_DYNAMIC_LOAD_ANOTHER, mDynamicScrollLoadAnother);
        outState.putInt(KEY_ITEM_CLICKS_FOR_INTERSTITIAL, mItemClicksForInterstitial);
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
        if (mRecyclerView != null) {
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

    /**
     * Initializes the interstitial ad.
     */
    private void initInterstitialAd() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                mItemClicksForInterstitial = 0;
                requestNewInterstitialAd();
            }
        });

        requestNewInterstitialAd();
    }

    /**
     * Requests a new interstitial ad to be loaded.
     */
    private void requestNewInterstitialAd() {
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }
}
