package com.mcochin.stockstreaks.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.mcochin.stockstreaks.data.StockContract;

import java.util.Calendar;

/**
 * Utility class containing general helper methods for this application
 */
public class Utility {
    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c Context used to get the ConnectivityManager
     * @return true if the network is available
     */
    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static Calendar calendarTimeReset(Calendar calendar){
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    public static boolean isEntryExist(String symbol, ContentResolver cr){
        Cursor cursor = null;
        try{
            cursor = cr.query(StockContract.StockEntry.buildUri(symbol), null, null, null, null);
            if(cursor != null){
                return cursor.moveToFirst();
            }

        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return false;
    }
}