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
import com.mcochin.stockstreaks.pojos.LoadAFewFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadFromDbFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.MainService;
import com.mcochin.stockstreaks.utils.Utility;

public class ListManagerFragment extends Fragment{
    public static final String TAG = ListManagerFragment.class.getSimpleName();

    private ListManipulator mListManipulator;
    private EventListener mEventListener;

    /**
     * This is true if the list is updating to the latest values.(Swipe to Refresh/ Refresh Menu Btn)
     */
    private volatile boolean mRefreshing;

    /**
     * This is true if the list is loading a few. Latest values or not.
     */
    private boolean mLoadingAFew;

    public interface EventListener{
        void onLoadAllFromDbFinished();
        void onLoadAFewFinished(boolean success);
        void onLoadSymbolFinished(boolean success);
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
                        if (mListManipulator.isListUpdated()) {
                            mListManipulator.saveShownListState(cr);
                        }
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

    public void initLoadFromDb(){
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

                ListEventQueue.getInstance().post(new LoadFromDbFinishedEvent());
                return null;
            }
        }.execute(getActivity().getApplicationContext());
    }

    /**
     * Refreshes the list.
     * @param attachSymbol An option to query a symbol once the list has done refreshing.
     */
    public void initLoadAFew(final String attachSymbol){
        // Get load list of symbols to query
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... params) {
                synchronized (this) {
                    // Give list to ListManipulator
                    if (mListManipulator.isListUpdated()) {
                        mListManipulator.saveShownListState(params[0].getContentResolver());
                    }
                    mListManipulator.setLoadList(getLoadListFromDb());
                    mRefreshing = loadAFew();

                    if (attachSymbol != null) {
                        loadSymbol(attachSymbol);

                    }else if(!mRefreshing){
                        ListEventQueue.getInstance().post(new LoadAFewFinishedEvent(null, false));
                    }
                    return null;
                }
            }
        }.execute(getActivity().getApplicationContext());
    }

    /**
     * Loads the next few symbols in the user's list.
     * @return true is request can be sent to {@link MainService}, false otherwise.
     */
    public boolean loadAFew() {
        String[] aFewToLoad = mListManipulator.getAFewToLoad();

        if (aFewToLoad != null) {
            mLoadingAFew = true;

            // Start service to load a few
            Intent serviceIntent = new Intent(getContext(), MainService.class);

            serviceIntent.setAction(MainService.ACTION_LOAD_A_FEW);
            serviceIntent.putExtra(MainService.KEY_LOAD_A_FEW_QUERY, aFewToLoad);
            getContext().startService(serviceIntent);

            return true;
        }
        mLoadingAFew = false;
        return false;
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
                loadList = new String[cursor.getCount()];
                int i = 0;
                while(cursor.moveToNext()){
                    loadList[i] = cursor.getString(indexSymbol);
                    i++;
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
            mEventListener.onLoadAllFromDbFinished();
        }
    }

    public void onEventMainThread(LoadSymbolFinishedEvent event){
        synchronized (this) {
            if (event.isSuccessful()) {
                mListManipulator.addItemToTop(event.getStock());
            }

            if (mEventListener != null) {
                mEventListener.onLoadSymbolFinished(event.isSuccessful());
            }
        }
    }

    public void onEventMainThread(LoadAFewFinishedEvent event){

        synchronized (this) {
            if (event.isSuccessful()) {
                if (mRefreshing) {
                    mListManipulator.setShownListCursor(null);
                } else {
                    // Remove loading item if it exists
                    mListManipulator.removeLoadingItem();
                }

                for (Stock stock : event.getStockList()) {
                    mListManipulator.addItemToBottom(stock);
                }
            }
            mRefreshing = false;
            mLoadingAFew = false;

            if (mEventListener != null) {
                mEventListener.onLoadAFewFinished(event.isSuccessful());
            }
        }
    }

    public boolean isRefreshing(){
        return mRefreshing;
    }
    public boolean isLoadingAFew(){
        return mLoadingAFew;
    }

    public void setEventListener(EventListener eventListener){
        mEventListener = eventListener;
    }

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }
}
