package com.mcochin.stockstreaks.fragments;
import android.content.Intent;
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

public class ListManipulatorFragment extends Fragment {
    public static final String TAG = ListManipulatorFragment.class.getSimpleName();

    private ListManipulator mListManipulator;
    private EventListener mEventListener;

    public interface EventListener{
        void onLoadNextFewFinished();
        void onLoadAllFromDbFinished();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    @Override
    public void onDetach() {
        mEventListener = null;
        super.onDetach();
    }

    public void initLoadAllFromDb(){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = null;
                try {
                    // Query db for all data.
                    cursor = getContext().getContentResolver().query(
                            StockContract.StockEntry.CONTENT_URI,
                            ListManipulator.STOCK_PROJECTION,
                            null,
                            null,
                            StockProvider.ORDER_BY_ID_DESC);

                    // Extract Stock data from cursor
                    if(cursor != null ){
                        ArrayList<Stock> shownList = new ArrayList<>();
                        while(cursor.moveToNext()) {
                            Stock stock = Utility.getStockFromCursor(cursor);
                            stock.setId(mListManipulator.generateUniqueId());
                            shownList.add(stock);
                        }

                        mListManipulator.setShownList(shownList);

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
                Cursor cursor = null;
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
                        String[] loadList = new String[cursor.getCount()];
                        int i = 0;
                        while(cursor.moveToNext()){
                            loadList[i] = cursor.getString(indexSymbol);
                            i++;
                        }

                        // Give list to ListManipulator
                        mListManipulator.setLoadList(loadList);
                        loadAFew();
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

    public void loadAFew() {
        // Start service to load a few
        Intent serviceIntent = new Intent(getContext(), NetworkService.class);
        serviceIntent.setAction(NetworkService.ACTION_LOAD_A_FEW);
        serviceIntent.putExtra(NetworkService.KEY_LOAD_A_FEW_QUERY, mListManipulator.getAFewToLoad());

        getContext().startService(serviceIntent);
    }

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }

    public void setEventListener(EventListener eventListener){
        mEventListener = eventListener;
    }

    //TODO BroadcastReceiver, notify to query the few and then notify event listener
    //TODO also need to find a away to update updateDate
}
