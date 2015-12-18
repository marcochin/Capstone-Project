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

import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.NetworkService;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;
import java.util.Locale;

public class ListManipulatorFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = ListManipulatorFragment.class.getSimpleName();
    public static final String BROADCAST_ACTION = StockContract.CONTENT_AUTHORITY;
    public static final int ID_LOADER_STOCK_WITH_SYMBOL = 2;

    private ListManipulator mListManipulator;
    private EventListener mEventListener;
    private UpdateReceiver mUpdateReceiver;

    /**
     * This is true if the list is updating to the latest values
     */
    private volatile boolean mIsRefreshing;

    /**
     * This is true if the list is loading a few. Latest values or not.
     */
    private boolean mIsLoadingAFew;

    public interface EventListener{
        void onLoadAFewFinished(boolean isSuccess);
        void onLoadAllFromDbFinished();
        void onLoadStockWithSymbolFinished(Loader<Cursor> loader, Cursor data);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Register receiver
        if(mUpdateReceiver == null){
            mUpdateReceiver = new UpdateReceiver();
        }
        getContext().registerReceiver(mUpdateReceiver, new IntentFilter(BROADCAST_ACTION));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    @Override
    public void onStop() {
        ContentResolver cr  = getContext().getContentResolver();
        mListManipulator.permanentlyDeleteLastRemoveItem(cr);
        if(mListManipulator.isListUpdated()){
            mListManipulator.saveBookmarkAndListPositions(cr);
        }
        super.onStop();
    }

    @Override
    public void onDetach() {
        // Release references to activity or might cause memory leak;
        mEventListener = null;
        // Unregister Receiver
        getContext().unregisterReceiver(mUpdateReceiver);
        super.onDetach();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        switch (id) {
            case ID_LOADER_STOCK_WITH_SYMBOL:
                String symbol = args.getString(NetworkService.KEY_SEARCH_QUERY);

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

                if (mEventListener != null) {
                    mEventListener.onLoadStockWithSymbolFinished(loader, data);
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

                    if(shownPositionBookmark >= 0) {
                        // Query db for all data with the same updateDate as the first entry.
                        cursor = cr.query(
                                StockEntry.CONTENT_URI,
                                ListManipulator.STOCK_PROJECTION,
                                StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION,
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
                // Give list to ListManipulator
                mListManipulator.setLoadList(getLoadListFromDb());
                mIsRefreshing = loadAFew();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                //Loader must be run on main thread or crash
                if (attachSymbol != null) {
                    loadStockWithSymbol(attachSymbol);
                }
                if(!mIsRefreshing && mEventListener != null){
                    mEventListener.onLoadAFewFinished(false);
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
            mIsLoadingAFew = true;

            // Start service to load a few
            Intent serviceIntent = new Intent(getContext(), NetworkService.class);
            serviceIntent.setAction(NetworkService.ACTION_LOAD_A_FEW);

            if (mListManipulator.getLoadListPositionBookmark() == 0) {
                serviceIntent.putExtra(NetworkService.KEY_LIST_REFRESH, true);
            }
            serviceIntent.putExtra(NetworkService.KEY_LOAD_A_FEW_QUERY, aFewToLoad);
            getContext().startService(serviceIntent);

            return true;
        }

        mIsLoadingAFew = false;
        return false;
    }

    public void loadStockWithSymbol(String symbol){
        // Start service to retrieve stock info
        Intent serviceIntent = new Intent(getContext(), NetworkService.class);
        serviceIntent.putExtra(NetworkService.KEY_SEARCH_QUERY, symbol);
        serviceIntent.setAction(NetworkService.ACTION_STOCK_WITH_SYMBOL);

        getContext().startService(serviceIntent);

        //Start cursor loader to load the newly added stock
        Bundle args = new Bundle();
        args.putString(NetworkService.KEY_SEARCH_QUERY, symbol);

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
        return mIsRefreshing;
    }
    public boolean isLoadingAFew(){
        return mIsLoadingAFew;
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
            final int errorCode = intent.getIntExtra(NetworkService.KEY_ERROR, 0);

                new AsyncTask<Void, Void, Boolean>() {
                    boolean isSuccess = false;

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        if(errorCode != NetworkService.NETWORK_ERROR) {
                            isSuccess = true;

                            ArrayList<ContentProviderOperation> ops = intent
                                    .getParcelableArrayListExtra(StockProvider.KEY_OPERATIONS);
                            Cursor cursor = null;

                            // Remove loading item if it exists
                            mListManipulator.removeLoadingItem();

                            //Loop through results
                            for (ContentProviderOperation op : ops) {
                                try {
                                    // Filter out save state Uri
                                    if (!StockContract.isSaveStateUri(op.getUri())){
                                        // Query the stocks from db
                                        cursor = getContext().getContentResolver().query(
                                                op.getUri(),
                                                ListManipulator.STOCK_PROJECTION,
                                                null,
                                                null,
                                                null);

                                        if(mIsRefreshing){
                                            mListManipulator.setShownListCursor(cursor);
                                        }else {
                                            // Insert stock in shownList
                                            if (cursor != null && cursor.moveToFirst()) {
                                                Stock stock = Utility.getStockFromCursor(cursor);
                                                mListManipulator.addItemToBottom(stock);
                                            }
                                        }
                                    }
                                } finally {
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                }
                            }
                        }
                        return isSuccess;
                    }

                    @Override
                    protected void onPostExecute(Boolean isSuccess) {
                        if (mEventListener != null) {
                            mEventListener.onLoadAFewFinished(isSuccess);
                        }
                        mIsRefreshing = false;
                        mIsLoadingAFew = false;
                    }
                }.execute();
        }
    }
}
