package com.mcochin.stockstreaks.fragments;
import android.content.BroadcastReceiver;
import android.content.ContentProviderResult;
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

import com.mcochin.stockstreaks.MainActivity;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.NetworkService;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ListManipulatorFragment extends Fragment {
    public static final String TAG = ListManipulatorFragment.class.getSimpleName();
    public static final String BROADCAST_ACTION = StockContract.CONTENT_AUTHORITY;
    public static final int ID_LOADER_STOCK_WITH_SYMBOL = 2;

    private ListManipulator mListManipulator;
    private EventListener mEventListener;
    private UpdateReceiver mUpdateReceiver;

    public interface EventListener{
        void onLoadNextFewFinished();
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
    public void onDetach() {
        // Release references to activity or might cause memory leak;
        mEventListener = null;
        // Unregister Receiver
        getContext().unregisterReceiver(mUpdateReceiver);
        super.onDetach();
    }

    public void initLoadAllFromDb(){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = null;
                try {
                    ContentResolver cr = getContext().getContentResolver();
                    Calendar lastUpdateTime = Utility.getLastUpdateTime(cr);

                    if(lastUpdateTime != null) {
                        // Query db for all data with the same updateDate as the first entry.
                        cursor = cr.query(
                                StockContract.StockEntry.CONTENT_URI,
                                ListManipulator.STOCK_PROJECTION,
                                StockProvider.UPDATE_DATE_SELECTION,
                                new String[]{Long.toString(lastUpdateTime.getTimeInMillis())},
                                StockProvider.ORDER_BY_ID_DESC);

                        // Extract Stock data from cursor
                        if (cursor != null) {
                            mListManipulator.setShownListCursor(cursor);
                            mListManipulator.setLoadList(getLoadListFromDb());
                            mListManipulator.addToLoadListPositionBookmark(cursor.getCount());

                            if (mEventListener != null) {
                                mEventListener.onLoadAllFromDbFinished();
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
        }.execute();
    }

    /**
     * Refreshes the list.
     * @param attachSymbol An option to query a symbol once the list has done refreshing.
     */
    public void initLoadAFew(final String attachSymbol){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                // Give list to ListManipulator
                mListManipulator.setLoadList(getLoadListFromDb());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                loadAFew();

                if(attachSymbol != null){
                    loadStockWithSymbol(attachSymbol);
                }
            }
        }.execute();
    }

    public void loadAFew() {
        String[] aFewToLoad = mListManipulator.getAFewToLoad();

        if(aFewToLoad != null) {
            // Start service to load a few
            Intent serviceIntent = new Intent(getContext(), NetworkService.class);
            serviceIntent.setAction(NetworkService.ACTION_LOAD_A_FEW);

            if (mListManipulator.getLoadListPositionBookmark() == 0) {
                serviceIntent.putExtra(NetworkService.KEY_UPDATE_UPDATE_DATE, true);
            }
            serviceIntent.putExtra(NetworkService.KEY_LOAD_A_FEW_QUERY, aFewToLoad);

            getContext().startService(serviceIntent);
        }

        //TODO else nothing to load
    }

    public void loadStockWithSymbol(final String symbol){

        // Start service to retrieve stock info
        Intent serviceIntent = new Intent(getContext(), NetworkService.class);
        serviceIntent.putExtra(NetworkService.KEY_SEARCH_QUERY, symbol);
        serviceIntent.setAction(NetworkService.ACTION_STOCK_WITH_SYMBOL);

        if(Utility.canUpdateList(getContext().getContentResolver())){
            serviceIntent.putExtra(NetworkService.KEY_UPDATE_UPDATE_DATE, true);
        }
        getContext().startService(serviceIntent);

        //Start cursor loader to load the newly added stock
        ((AppCompatActivity)getContext()).getSupportLoaderManager().restartLoader(
                ID_LOADER_STOCK_WITH_SYMBOL,
                null,
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        CursorLoader loader = null;
                        switch (id) {
                            case ID_LOADER_STOCK_WITH_SYMBOL:
                                loader = new CursorLoader(
                                        getContext(),
                                        StockContract.StockEntry.buildUri(
                                                symbol.toUpperCase(Locale.US)),
                                        ListManipulator.STOCK_PROJECTION,
                                        null,
                                        null,
                                        null);
                                break;
                        }
                        return loader;
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                        if (mEventListener != null) {
                            mEventListener.onLoadStockWithSymbolFinished(loader, data);
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {

                    }
                });
    }

    private String[] getLoadListFromDb(){
        Cursor cursor = null;
        String[] loadList = null;
        try {
            final String[] projection = new String[]{StockContract.StockEntry.COLUMN_SYMBOL};
            final int indexSymbol = 0;

            // Query db for just symbols.
            cursor = getContext().getContentResolver().query(
                    StockContract.StockEntry.CONTENT_URI,
                    projection,
                    null,
                    null,
                    StockProvider.ORDER_BY_ID_DESC);

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
        public void onReceive(Context context, Intent intent) {
            ContentProviderResult[] results = (ContentProviderResult[])
                    intent.getParcelableArrayExtra(StockProvider.KEY_UPDATE_RESULTS);
            //Loop through results
            for (ContentProviderResult result : results) {
                // Query the stocks from db
                Cursor cursor = getContext().getContentResolver().query(result.uri,
                        ListManipulator.STOCK_PROJECTION, null, null, null);

                // Insert stock in shownList
                if(cursor != null && cursor.moveToFirst()) {
                    Stock stock = Utility.getStockFromCursor(cursor);
                    mListManipulator.addItem(stock);
                    cursor.close();
                }
            }

            // add to bookmark the result size
            mListManipulator.addToLoadListPositionBookmark(results.length);

            if (mEventListener != null) {
                mEventListener.onLoadNextFewFinished();
            }
        }
    }
}
