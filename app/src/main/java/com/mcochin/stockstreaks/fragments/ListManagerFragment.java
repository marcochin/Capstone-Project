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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.NetworkService;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ListManagerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = ListManagerFragment.class.getSimpleName();
    public static final String UPDATE_BROADCAST_ACTION = StockContract.CONTENT_AUTHORITY;
    public static final String ERROR_BROADCAST_ACTION = StockContract.CONTENT_AUTHORITY + ".error";
    public static final int ID_LOADER_STOCK_WITH_SYMBOL = 2;

    private ListManipulator mListManipulator;
    private EventListener mEventListener;
    private UpdateReceiver mUpdateReceiver;
    private ErrorReceiver mErrorReceiver;

    /**
     * This is true if the list is updating to the latest values
     */
    private volatile boolean mRefreshing;

    /**
     * This is true if the list is loading a few. Latest values or not.
     */
    private boolean mLoadingAFew;

    public interface EventListener{
        void onLoadError(int errorCode);
        void onLoadAFewFinished();
        void onLoadAllFromDbFinished();
        void onLoadStockWithSymbolFinished();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Register receiver
        if(mUpdateReceiver == null){
            mUpdateReceiver = new UpdateReceiver();
            mErrorReceiver = new ErrorReceiver();
        }
        context.registerReceiver(mUpdateReceiver, new IntentFilter(UPDATE_BROADCAST_ACTION));
        context.registerReceiver(mErrorReceiver, new IntentFilter(ERROR_BROADCAST_ACTION));
        recalibrateLoader();
    }

    /**
     * When we rotate the device when a item is loading, Android somehow loses its reference to
     * loader, and so we need to boot it up again if there exists one. This is a workaround.
     * http://stackoverflow.com/questions/11618576/why-is-my-loader-destroyed
     */
    private void recalibrateLoader(){
        LoaderManager loaderManager = ((AppCompatActivity)getContext()).getSupportLoaderManager();
        Loader<Cursor> loader = loaderManager.getLoader(
                ListManagerFragment.ID_LOADER_STOCK_WITH_SYMBOL);

        if(loader != null){
            loaderManager.initLoader(
                    ListManagerFragment.ID_LOADER_STOCK_WITH_SYMBOL,
                    null,
                    this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    @Override
    public void onStop() {
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (this) {
                    ContentResolver cr = getContext().getContentResolver();
                    mListManipulator.permanentlyDeleteLastRemoveItem(cr);
                    if (mListManipulator.isListUpdated()) {
                        mListManipulator.saveBookmarkAndListPositions(cr);
                    }
                    return null;
                }
            }
        }.execute();
        super.onStop();
    }

    @Override
    public void onDetach() {
        // Release references to activity or might cause memory leak;
        mEventListener = null;
        // Unregister Receiver
        getContext().unregisterReceiver(mUpdateReceiver);
        getContext().unregisterReceiver(mErrorReceiver);
        super.onDetach();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        switch (id) {
            case ID_LOADER_STOCK_WITH_SYMBOL:
                String symbol = args.getString(NetworkService.KEY_LOAD_SYMBOL_QUERY);

                if(symbol != null) {
                    loader = new CursorLoader(
                            getContext(),
                            StockEntry.buildUri(symbol.toUpperCase(Locale.US)),
                            ListManipulator.STOCK_PROJECTION,
                            null,
                            null,
                            null);
                }
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null || data.getCount() == 0){
            return;
        }
        int id = loader.getId();

        switch (id) {
            case ID_LOADER_STOCK_WITH_SYMBOL:
                if (data.moveToFirst()) {
                    mListManipulator.addItemToTop(Utility.getStockFromCursor(data));
                }
                if (mEventListener != null) {
                    mEventListener.onLoadStockWithSymbolFinished();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
                    Log.d(TAG, "shown bookmrk: " + shownPositionBookmark);

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
                        mEventListener.onLoadAFewFinished();
                    }
                }
            }
        }.execute();
    }

    /**
     * Loads the next few symbols in the user's list.
     * @return true is request can be sent to {@link NetworkService}, false otherwise.
     */
    public boolean loadAFew() {
        String[] aFewToLoad = mListManipulator.getAFewToLoad();

        if (aFewToLoad != null) {
            mLoadingAFew = true;

            // Start service to load a few
            Intent serviceIntent = new Intent(getContext(), NetworkService.class);

            serviceIntent.setAction(NetworkService.ACTION_LOAD_A_FEW);
            serviceIntent.putExtra(NetworkService.KEY_LOAD_A_FEW_QUERY, aFewToLoad);
            getContext().startService(serviceIntent);

            return true;
        }

        mLoadingAFew = false;
        return false;
    }

    public void loadSymbol(String symbol){
        // Start service to retrieve stock info
        Intent serviceIntent = new Intent(getContext(), NetworkService.class);
        serviceIntent.putExtra(NetworkService.KEY_LOAD_SYMBOL_QUERY, symbol);
        serviceIntent.setAction(NetworkService.ACTION_LOAD_SYMBOL);

        getContext().startService(serviceIntent);

        //Start cursor loader to load the newly added stock
        Bundle args = new Bundle();
        args.putString(NetworkService.KEY_LOAD_SYMBOL_QUERY, symbol);

        ((AppCompatActivity)getContext()).getSupportLoaderManager()
                .restartLoader(ID_LOADER_STOCK_WITH_SYMBOL, args, this);
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
    public class UpdateReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, final Intent intent) {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        synchronized (this){
                            ArrayList<ContentProviderOperation> ops = intent
                                    .getParcelableArrayListExtra(StockProvider.KEY_OPERATIONS);
                            Cursor cursor = null;

                            if(mRefreshing){
                                mListManipulator.setShownListCursor(null);
                            }else{
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

                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if (mEventListener != null) {
                            mEventListener.onLoadAFewFinished();
                        }
                        mRefreshing = false;
                        mLoadingAFew = false;
                    }
                }.execute();
        }
    }

    public class ErrorReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            int errorCode = intent.getIntExtra(NetworkService.KEY_LOAD_ERROR, 0);

            if (mEventListener != null) {
                mEventListener.onLoadError(errorCode);
            }
            if(errorCode == NetworkService.LOAD_A_FEW_ERROR) {
                mRefreshing = false;
                mLoadingAFew = false;
            }
        }
    }
}
