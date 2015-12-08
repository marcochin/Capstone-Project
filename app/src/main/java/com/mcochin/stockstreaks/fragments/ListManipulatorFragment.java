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

import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.services.NetworkService;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;
import java.util.Calendar;

public class ListManipulatorFragment extends Fragment {
    public static final String TAG = ListManipulatorFragment.class.getSimpleName();
    public static final String BROADCAST_ACTION = StockContract.CONTENT_AUTHORITY;

    private ListManipulator mListManipulator;
    private EventListener mEventListener;
    private UpdateReceiver mUpdateReceiver;

    public interface EventListener{
        void onLoadNextFewFinished();
        void onLoadAllFromDbFinished();
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

                    // Query db for all data with the same updateDate as the first entry.
                    cursor = cr.query(
                            StockContract.StockEntry.CONTENT_URI,
                            ListManipulator.STOCK_PROJECTION,
                            StockProvider.UPDATE_DATE_SELECTION,
                            new String[]{Long.toString(lastUpdateTime.getTimeInMillis())},
                            StockProvider.ORDER_BY_ID_DESC);

                    // Extract Stock data from cursor
                    if(cursor != null ){
                        mListManipulator.setShownListCursor(cursor);
                        mListManipulator.setLoadList(getLoadListFromDb());
                        mListManipulator.syncLoadListBookmarkToShownList();

                        if(mEventListener != null){
                            mEventListener.onLoadAllFromDbFinished();
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

    public void initLoadAFew(){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                // Give list to ListManipulator
                mListManipulator.setLoadList(getLoadListFromDb());
                loadAFew();

                return null;
            }
        }.execute();
    }

    public void loadAFew() {
        // Start service to load a few
        Intent serviceIntent = new Intent(getContext(), NetworkService.class);
        serviceIntent.setAction(NetworkService.ACTION_LOAD_A_FEW);
        serviceIntent.putExtra(NetworkService.KEY_LOAD_A_FEW_QUERY, mListManipulator.getAFewToLoad());

        getContext().startService(serviceIntent);
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

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }

    public void setEventListener(EventListener eventListener){
        mEventListener = eventListener;
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

            if (mEventListener != null) {
                mEventListener.onLoadNextFewFinished();
            }
        }
    }
}
