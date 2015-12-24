package com.mcochin.stockstreaks.fragments;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.MainService;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;

public class ListManagerFragment extends Fragment{
    public static final String TAG = ListManagerFragment.class.getSimpleName();
    public static final String BROADCAST_ACTION_LOAD_A_FEW = StockContract.CONTENT_AUTHORITY + ".few";
    public static final String BROADCAST_ACTION_LOAD_SYMBOL = StockContract.CONTENT_AUTHORITY + ".symbol";
    public static final int ID_LOADER_STOCK_WITH_SYMBOL = 1;

    private ListManipulator mListManipulator;
    private EventListener mEventListener;
    private LoadAFewReceiver mLoadAFewReceiver;
    private LoadSymbolReceiver mLoadSymbolReceiver;

    /**
     * This is true if the list is updating to the latest values
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
    public void onResume() {
        super.onResume();

        // Register receiver
        if(mLoadAFewReceiver == null){
            mLoadAFewReceiver = new LoadAFewReceiver();
            mLoadSymbolReceiver = new LoadSymbolReceiver();
        }

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.registerReceiver(
                mLoadAFewReceiver,
                new IntentFilter(BROADCAST_ACTION_LOAD_A_FEW));
        broadcastManager.registerReceiver(
                mLoadSymbolReceiver,
                new IntentFilter(BROADCAST_ACTION_LOAD_SYMBOL));

        getContext().getContentResolver().call(StockContract.BASE_CONTENT_URI,
                StockProvider.METHOD_GET_LOST_BROADCAST, null, null);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.unregisterReceiver(mLoadSymbolReceiver);
        broadcastManager.unregisterReceiver(mLoadAFewReceiver);

        new AsyncTask<Context, Void, Void>(){
            @Override
            protected Void doInBackground(Context... params) {
                synchronized (this) {
                    ContentResolver cr = params[0].getContentResolver();
                    mListManipulator.permanentlyDeleteLastRemoveItem(cr);
                    if (mListManipulator.isListUpdated()) {
                        mListManipulator.saveBookmarkAndListPositions(cr);
                    }
                }
                return null;
            }
        }.execute(getActivity().getApplicationContext());

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        // Release references to activity or might cause memory leak;
        mEventListener = null;

        super.onDetach();
    }

    public void initLoadFromDb(){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = null;
                try {
                    ContentResolver cr = getContext().getContentResolver();
                    int shownPositionBookmark  = Utility.getShownPositionBookmark(cr);

                    // Query db for all data with the same updateDate as the first entry.
                    cursor = cr.query(
                            StockEntry.CONTENT_URI,
                            ListManipulator.STOCK_PROJECTION,
                            shownPositionBookmark == 0 ?
                                    StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION_ZERO
                                    :StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION,
                            new String[]{Integer.toString(shownPositionBookmark)},
                            StockProvider.ORDER_BY_LIST_POSITION_ASC);

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
                    mEventListener.onLoadAllFromDbFinished();
                }
            }
        }.execute();
    }

    /**
     * Refreshes the list.
     * @param attachSymbol An option to query a symbol once the list has done refreshing.
     */
    public void initLoadAFew(final String attachSymbol){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (this) {
                    // Give list to ListManipulator
                    if (mListManipulator.isListUpdated()) {
                        mListManipulator.saveBookmarkAndListPositions(getContext().getContentResolver());
                    }
                    mListManipulator.setLoadList(getLoadListFromDb());
                    mRefreshing = loadAFew();

                    return null;
                }
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                //Loader must be run on main thread or crash
                if (attachSymbol != null) {
                    loadSymbol(attachSymbol);

                }else if(!mRefreshing){
                    if(mEventListener != null){
                        mEventListener.onLoadAFewFinished(false);
                    }
                }
            }
        }.execute();
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
                    StockProvider.ORDER_BY_LIST_POSITION_ASC);

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

    /**
     * Receiver that gets notified when there is a bulk update to the items in the db.
     */
    public class LoadAFewReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, final Intent intent) {
                new AsyncTask<Void, Void, Void>() {
                    boolean success = intent.getBooleanExtra(MainService.KEY_LOAD_SUCCESS, true);

                    @Override
                    protected Void doInBackground(Void... params) {
                        synchronized (this) {
                            if (success) {
                                ArrayList<ContentProviderOperation> ops =
                                        intent.getParcelableArrayListExtra(StockProvider.KEY_OPERATIONS);
                                Cursor cursor = null;

                                if (mRefreshing) {
                                    mListManipulator.setShownListCursor(null);
                                } else {
                                    // Remove loading item if it exists
                                    mListManipulator.removeLoadingItem();
                                }

                                //Loop through results
                                for (ContentProviderOperation op : ops) {
                                    try {
                                        // Filter out save state Uri
                                        if (!StockContract.isSaveStateUri(op.getUri())) {
                                            // Query the stocks from db
                                            cursor = getContext().getContentResolver().query(
                                                    op.getUri(),
                                                    ListManipulator.STOCK_PROJECTION,
                                                    null,
                                                    null,
                                                    null);

                                            // Insert stock in shownList
                                            if (cursor != null && cursor.moveToFirst()) {
                                                Stock stock = Utility.getStockFromCursor(cursor);
                                                mListManipulator.addItemToBottom(stock);
                                            }
                                        }
                                    } finally {
                                        if (cursor != null) {
                                            cursor.close();
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if (mEventListener != null) {
                            mEventListener.onLoadAFewFinished(success);
                        }
                        mRefreshing = false;
                        mLoadingAFew = false;
                    }
                }.execute();
        }
    }

    /**
     * Receiver that gets notified when there an item is inserted in the db.
     */
    public class LoadSymbolReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, final Intent intent) {
            new AsyncTask<Void, Void, Void>(){
                boolean success = intent.getBooleanExtra(MainService.KEY_LOAD_SUCCESS, true);

                @Override
                protected Void doInBackground(Void... params) {
                    synchronized (this) {
                        if (success) {
                            ArrayList<ContentProviderOperation> ops = intent
                                    .getParcelableArrayListExtra(StockProvider.KEY_OPERATIONS);
                            Cursor cursor = null;

                            //Loop through results
                            for (ContentProviderOperation op : ops) {
                                try {
                                    // Filter out save state Uri
                                    if (!StockContract.isSaveStateUri(op.getUri())) {
                                        // Query the stocks from db
                                        cursor = getContext().getContentResolver().query(
                                                op.getUri(),
                                                ListManipulator.STOCK_PROJECTION,
                                                null,
                                                null,
                                                null);

                                        // Insert stock in shownList
                                        if (cursor != null && cursor.moveToFirst()) {
                                            Stock stock = Utility.getStockFromCursor(cursor);
                                            mListManipulator.addItemToTop(stock);
                                        }
                                    }
                                } finally {
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                }
                            }
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    if (mEventListener != null) {
                        mEventListener.onLoadSymbolFinished(success);
                    }
                }
            }.execute();
        }
    }
}
