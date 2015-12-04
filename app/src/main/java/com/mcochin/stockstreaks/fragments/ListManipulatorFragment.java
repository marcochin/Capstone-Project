package com.mcochin.stockstreaks.fragments;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;

public class ListManipulatorFragment extends Fragment {
    public static final String TAG = ListManipulatorFragment.class.getSimpleName();

    private ListManipulator mListManipulator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);  // keep the fragment instance
        mListManipulator = new ListManipulator();
    }

    public void initLoadList(){
        // Get load list of symbols to query
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = null;
                try {
                    final String[] projection = new String[]{StockContract.StockEntry.COLUMN_SYMBOL};
                    final int indexSymbol = 0;

                    // We want last db item to be on top of the list
                    final String orderByDesc = "ORDER BY " + StockContract.StockEntry._ID + " DESC";

                    // Query db for symbols.
                    cursor = getContext().getContentResolver().query(
                            StockContract.StockEntry.CONTENT_URI,
                            projection,
                            null,
                            null,
                            orderByDesc);

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
                        loadNextFew();
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

    public void loadNextFew(){
        //TODO start service to load next 20

    }

    public ListManipulator getListManipulator() {
        return mListManipulator;
    }

}
