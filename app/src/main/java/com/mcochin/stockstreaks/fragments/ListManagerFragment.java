package com.mcochin.stockstreaks.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.mcochin.stockstreaks.data.ListEventQueue;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.events.LoadAFewFinishedEvent;
import com.mcochin.stockstreaks.events.LoadFromDbFinishedEvent;
import com.mcochin.stockstreaks.events.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.events.WidgetRefreshEvent;
import com.mcochin.stockstreaks.services.MainService;
import com.mcochin.stockstreaks.utils.Utility;
import com.mcochin.stockstreaks.widget.StockWidgetProvider;

public class ListManagerFragment extends Fragment{
    public static final String TAG = ListManagerFragment.class.getSimpleName();

    private ListManipulator mListManipulator;
    private EventListener mEventListener;

    /**
     * This is true if the list is updating to the latest values.(Swipe to Refresh/ Refresh Menu Btn)
     */
    private static volatile boolean mRefreshing;

    /**
     * This is true if the list is loading a few. Latest values or not.
     */
    private static volatile boolean mLoadingAFew;

    public interface EventListener{
        void onLoadFromDbFinished();
        void onLoadAFewFinished(boolean success);
        void onLoadSymbolFinished(boolean success);
        void onWidgetRefresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    @Override
    public void onPause() {
        if (mListManipulator.isListUpdated()) {
            new AsyncTask<Context, Void, Void>() {
                @Override
                protected Void doInBackground(Context... params) {
                    synchronized (this) {
                        ContentResolver cr = params[0].getContentResolver();
                        mListManipulator.permanentlyDeleteLastRemoveItem(cr);
                        mListManipulator.saveShownListState(cr);
                        //Update widget to reflect changes
                        params[0].sendBroadcast(new Intent(StockWidgetProvider.ACTION_DATA_UPDATED));
                    }
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
                            StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION_LE,
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

                ListEventQueue.getInstance().post(new LoadFromDbFinishedEvent());
                return null;
            }
        }.execute(getActivity().getApplicationContext());
    }

    /**
     * Refreshes the list.
     * @param attachSymbol An option to query a symbol once the list has done refreshing.
     */
    public void initRefresh(final String attachSymbol){
        mRefreshing = true;
        // Get load list of symbols to query
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... params) {
                    if (mListManipulator.isListUpdated()) {
                        mListManipulator.saveShownListState(params[0].getContentResolver());
                    }
                    // Start service to refresh app
                    Intent serviceIntent = new Intent(getContext(), MainService.class);
                    serviceIntent.setAction(MainService.ACTION_APP_REFRESH);
                    getContext().startService(serviceIntent);

                    if (attachSymbol != null) {
                        loadSymbol(attachSymbol);
                    }
                    return null;
                }
        }.execute(getActivity().getApplicationContext());
    }

    /**
     * We load our stock list from the queue only if the widget has updated the app for us
     * and it's events are in the queue.
     */
    public void initFromWidgetRefresh(){
        new AsyncTask<Context, Void, Void>(){
            @Override
            protected Void doInBackground(Context... params) {
                mListManipulator.setLoadList(getLoadListFromDb());
                ListEventQueue.getInstance().postAllFromQueue();
                return null;
            }
        }.execute();
    }

    /**
     * Loads the next few symbols in the user's list.
     */
    public void loadAFew() {
        String[] aFewToLoad = mListManipulator.getAFewToLoad();

        if (aFewToLoad != null) {
            mLoadingAFew = true;

            // Start service to load a few
            Intent serviceIntent = new Intent(getContext(), MainService.class);
            serviceIntent.setAction(MainService.ACTION_LOAD_A_FEW);
            serviceIntent.putExtra(MainService.KEY_LOAD_A_FEW_QUERY, aFewToLoad);
            getContext().startService(serviceIntent);

        }else{
            mLoadingAFew = false;
        }
    }

    public void loadSymbol(String symbol){
        // Start service to retrieve stock info
        Intent serviceIntent = new Intent(getContext(), MainService.class);
        serviceIntent.putExtra(MainService.KEY_LOAD_SYMBOL_QUERY, symbol);
        serviceIntent.setAction(MainService.ACTION_LOAD_SYMBOL);
        getContext().startService(serviceIntent);
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

    public void onEventMainThread(LoadFromDbFinishedEvent event){
        if (mEventListener != null) {
            mEventListener.onLoadFromDbFinished();
        }
    }

    public void onEventMainThread(final LoadSymbolFinishedEvent event){
        // We use async task for the benefit of them executing sequentially in a single
        // background thread. And in order to prevent using the synchronized keyword in the main
        // thread which may block it.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (event.isSuccessful()) {
                    mListManipulator.addItemToTop(event.getStock());
                }

                if (mEventListener != null) {
                    mEventListener.onLoadSymbolFinished(event.isSuccessful());
                }
            }
        }.execute();
    }

    public void onEventMainThread(final LoadAFewFinishedEvent event){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                if (event.isSuccessful()) {
                    if (mRefreshing) {
                        mListManipulator.setShownListCursor(null);
                        // Give list to ListManipulator
                        mListManipulator.setLoadList(getLoadListFromDb());
                        mRefreshing = false;
                    } else {
                        // Remove loading item if it exists
                        mListManipulator.removeLoadingItem();
                    }

                    for (Stock stock : event.getStockList()) {
                        mListManipulator.addItemToBottom(stock);
                    }
                }
                mLoadingAFew = false;
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {

                if (mEventListener != null) {
                    mEventListener.onLoadAFewFinished(event.isSuccessful());
                }
            }
        }.execute();
    }

    public void onEventMainThread(WidgetRefreshEvent event){
        if(event.isRefreshThroughWidget()) {
            mRefreshing = true;
            mLoadingAFew = true;

        }else{
            initRefresh(null);
        }

        if (mEventListener != null) {
            mEventListener.onWidgetRefresh();
        }
    }

    public static boolean isRefreshing(){
        return mRefreshing;
    }
    public static boolean isLoadingAFew(){
        return mLoadingAFew;
    }
    public static void setRefreshing(boolean refreshing){
        mRefreshing = refreshing;
    }
    public static void setLoadingAfew(boolean loadingAFew){
        mLoadingAFew = loadingAFew;
    }


    public void setEventListener(EventListener eventListener){
        mEventListener = eventListener;
    }

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }
}
