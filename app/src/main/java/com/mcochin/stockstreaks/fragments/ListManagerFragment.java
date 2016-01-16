package com.mcochin.stockstreaks.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.mcochin.stockstreaks.custom.MyApplication;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.events.AppRefreshFinishedEvent;
import com.mcochin.stockstreaks.events.LoadMoreFinishedEvent;
import com.mcochin.stockstreaks.events.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.events.WidgetRefreshDelegateEvent;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.MainService;
import com.mcochin.stockstreaks.utils.Utility;
import com.mcochin.stockstreaks.widget.StockWidgetProvider;

public class ListManagerFragment extends Fragment{
    public static final String TAG = ListManagerFragment.class.getSimpleName();

    private ListManipulator mListManipulator;
    private EventListener mEventListener;

    /**
     * This is true if the list is loading a few on the bottom
     */
    private static volatile boolean mLoadingMore = false;
    private int testSymbol = 0;

    public interface EventListener{
        void onLoadFromDbFinished();
        void onLoadMoreFinished(LoadMoreFinishedEvent event);
        void onLoadSymbolFinished(LoadSymbolFinishedEvent event);
        void onRefreshFinished(AppRefreshFinishedEvent event);
        void onWidgetRefreshDelegate(WidgetRefreshDelegateEvent event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "pause");
        if (mListManipulator.isListUpdated()) {
            new AsyncTask<Context, Void, Void>() {
                @Override
                protected Void doInBackground(Context... params) {
                    ContentResolver cr = params[0].getContentResolver();
                    mListManipulator.permanentlyDeleteLastRemoveItem(cr);
                    mListManipulator.saveShownListState(cr);
                    //Update widget to reflect changes
                    params[0].sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATED));

                    return null;
                }
            }.execute(getActivity().getApplicationContext());
        }

        super.onPause();
    }

    @Override
    public void onDetach() {
        // Release references to activity or might cause memory leak;
        mEventListener = null;

        super.onDetach();
    }

    public void initFromDb(){
        // Get load list of symbols to query
        new AsyncTask<Context, Void, Void>(){
            @Override
            protected Void doInBackground(Context... params) {
                Cursor cursor = null;
                try {
                    ContentResolver cr = params[0].getContentResolver();
                    int shownPositionBookmark  = Utility.getShownPositionBookmark(cr);

                    // Query db for data up to the list position bookmark;
                    cursor = cr.query(
                            StockEntry.CONTENT_URI,
                            ListManipulator.STOCK_PROJECTION,
                            StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION,
                            new String[]{Integer.toString(shownPositionBookmark)},
                            StockProvider.ORDER_BY_LIST_POSITION_ASC_ID_DESC);

                    // Extract Stock data from cursor
                    if (cursor != null) {
                        int cursorCount = cursor.getCount();
                        if(cursorCount > 0) {
                            mListManipulator.setShownListCursor(cursor);
                            mListManipulator.setLoadList(getLoadListFromDb());
                            mListManipulator.addToLoadListPositionBookmark(cursorCount);
                        }
                    }
                }finally {
                    if(cursor != null){
                        cursor.close();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onLoadFromDbFinished();
                }
            }
        }.execute(getActivity().getApplicationContext());
    }

    /**
     * Refreshes the list.
     * @param attachSymbol An option to query a symbol once the list has done refreshing.
     */
    public void initFromRefresh(final String attachSymbol){
        MyApplication.getInstance().setRefreshing(true);
        // Get load list of symbols to query
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... params) {
                    refreshList(params[0]);

                    if (attachSymbol != null) {
                        loadSymbol(attachSymbol);
                    }
                    return null;
                }
        }.execute(getActivity().getApplicationContext());
    }

    /**
     * Refreshes the list. Should be called from a background thread.
     * @param context
     */
    private void refreshList(Context context){
        if (mListManipulator.isListUpdated()) {
            mListManipulator.saveShownListState(context.getContentResolver());
        }
        // Start service to refresh app
        Intent serviceIntent = new Intent(getContext(), MainService.class);
        serviceIntent.setAction(MainService.ACTION_APP_REFRESH);
        getContext().startService(serviceIntent);
    }

    public void loadSymbol(String symbol){
        // Start service to retrieve stock info
        Intent serviceIntent = new Intent(getContext(), MainService.class);
        serviceIntent.putExtra(MainService.KEY_LOAD_SYMBOL_QUERY, symbol);
        serviceIntent.setAction(MainService.ACTION_LOAD_SYMBOL);
        getContext().startService(serviceIntent);
    }

    /**
     * Loads the next few symbols in the user's list.
     */
    public void loadMore() {
        String[] moreSymbols = mListManipulator.getMoreToLoad();

        if (moreSymbols != null) {
            mLoadingMore = true;

            // Start service to load a few
            Intent serviceIntent = new Intent(getContext(), MainService.class);
            serviceIntent.setAction(MainService.ACTION_LOAD_MORE);
            serviceIntent.putExtra(MainService.KEY_LOAD_MORE_QUERY, moreSymbols);
            getContext().startService(serviceIntent);

        }else{
            mLoadingMore = false;
        }
    }
    public void testLoadSymbol(){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Stock stock = new Stock();
                stock.setSymbol("Add");
                stock.setChangeDollar(3.33f);
                stock.setChangePercent(3.33f);
                stock.setFullName("test inc");
                stock.setRecentClose(3.33f);
                stock.setStreak(-33);
                mListManipulator.addItemToTop(stock);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onLoadSymbolFinished(new LoadSymbolFinishedEvent(
                            MyApplication.getInstance().getSessionId(),
                            null,
                            true));
                }
            }
        }.execute();
    }

    public void testRefresh() {
        MyApplication.getInstance().setRefreshing(true);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mListManipulator.setShownListCursor(null);
                testSymbol = 0;
                for (int i = 0; i < ListManipulator.MORE; i++){
                    Stock stock = new Stock();
                    stock.setSymbol(Integer.toString(testSymbol++));
                    stock.setChangeDollar(3.33f);
                    stock.setChangePercent(3.33f);
                    stock.setFullName("test inc");
                    stock.setRecentClose(3.33f);
                    stock.setStreak(-33);

                    mListManipulator.addItemToBottom(stock);
                }
                MyApplication.getInstance().setRefreshing(false);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onRefreshFinished(new AppRefreshFinishedEvent(
                            MyApplication.getInstance().getSessionId(),
                            null,
                            true));
                }
            }
        }.execute();
    }

    public void testLoadMore() {
        mLoadingMore = true;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < ListManipulator.MORE; i++){
                    Stock stock = new Stock();
                    stock.setSymbol(Integer.toString(testSymbol++));
                    stock.setChangeDollar(3.33f);
                    stock.setChangePercent(3.33f);
                    stock.setFullName("test inc");
                    stock.setRecentClose(3.33f);
                    stock.setStreak(-33);

                    mListManipulator.addItemToPosition(mListManipulator.getCount() - 1, stock);

                }
                // Remove loading item if it exists
                mListManipulator.removeLoadingItem();
                mLoadingMore = false;
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onLoadMoreFinished(new LoadMoreFinishedEvent(
                            MyApplication.getInstance().getSessionId(),
                            null,
                            true));
                }
            }
        }.execute();
    }

    public boolean testCanLoadMore(){
        return testSymbol < 50;
    }

    /**
     * Should be called from a background thread.
     * @return a list of ALL the symbols from the db. This will serve as our load list.
     */
    private String[] getLoadListFromDb(){
        Cursor cursor = null;
        String[] loadList = null;
        try {
            final String[] projection = new String[]{StockEntry.COLUMN_SYMBOL};
            final int indexSymbol = 0;

            // Query db for just symbols.
            cursor = getContext().getContentResolver().query(
                    StockEntry.CONTENT_URI,
                    projection,
                    null,
                    null,
                    StockProvider.ORDER_BY_LIST_POSITION_ASC_ID_DESC);

            // Grab symbols from cursor and put them in array
            if(cursor != null){
                int cursorCount = cursor.getCount();
                loadList = new String[cursorCount];

                for(int i = 0; i < cursorCount; i ++){
                    cursor.moveToPosition(i);
                    loadList[i] = cursor.getString(indexSymbol);
                }
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return loadList;
    }

    public void onEventMainThread(final LoadSymbolFinishedEvent event){
        // We use async task for the benefit of them executing sequentially in a single
        // background thread. And in order to prevent using the synchronized keyword in the main
        // thread which may block it.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (event.isSuccessful()) {
                    mListManipulator.addItemToTop(event.getStock());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onLoadSymbolFinished(event);
                }
            }
        }.execute();
    }

    public void onEventMainThread(final LoadMoreFinishedEvent event){
        if(!event.getSessionId().equals(MyApplication.getInstance().getSessionId())){
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (event.isSuccessful()) {
                    //Add on bottom but before the loading item
                    for (Stock stock : event.getStockList()) {
                        mListManipulator.addItemToPosition(mListManipulator.getCount() - 1, stock);
                    }
                    // Remove loading item if it exists
                    mListManipulator.removeLoadingItem();
                }
                mLoadingMore = false;
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onLoadMoreFinished(event);
                }
            }
        }.execute();
    }

    public void onEventMainThread(final AppRefreshFinishedEvent event){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (event.isSuccessful()) {
                    mListManipulator.setShownListCursor(null);
                    // Give list to ListManipulator
                    mListManipulator.setLoadList(getLoadListFromDb());

                    for (Stock stock : event.getStockList()) {
                        mListManipulator.addItemToBottom(stock);
                    }
                }
                MyApplication.getInstance().setRefreshing(false);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onRefreshFinished(event);
                }
            }
        }.execute();
    }

    public void onEventMainThread(final WidgetRefreshDelegateEvent event){
        new AsyncTask<Context, Void, Void>(){
            @Override
            protected Void doInBackground(Context... params) {
                refreshList(params[0]);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mEventListener != null) {
                    mEventListener.onWidgetRefreshDelegate(event);
                }
            }
        }.execute(getActivity().getApplicationContext());
    }

    public void setEventListener(EventListener eventListener){
        mEventListener = eventListener;
    }

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }

    public boolean isLoadingMore() {
        return mLoadingMore;
    }

    public void setLoadingMore(boolean loadingAFew) {
        mLoadingMore = loadingAFew;
    }
}
