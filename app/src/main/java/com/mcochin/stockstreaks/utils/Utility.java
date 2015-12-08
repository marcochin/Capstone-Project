package com.mcochin.stockstreaks.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.pojos.Stock;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import yahoofinance.YahooFinance;

/**
 * Utility class containing general helper methods for this application
 */
public class Utility {
    public static final int STOCK_MARKET_UPDATE_HOUR = 16;
    public static final int STOCK_MARKET_UPDATE_MINUTE = 30;
    public static final int STOCK_MARKET_OPEN_HOUR = 9;
    public static final int STOCK_MARKET_OPEN_MINUTE = 30;

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

    /**
     * This is intended for threads to show toast messages.
     * @param context
     * @param toastMsg
     */
    public static void showToast(final Context context, final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static Calendar getNewYorkCalendarInstance(){
        return Calendar.getInstance(TimeZone.getTimeZone(YahooFinance.TIMEZONE));
    }

    public static Calendar calendarTimeReset(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    public static Calendar getCalendarQuickSetup(int hourOfDay, int minute, int milli) {
        Calendar calendar = getNewYorkCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.MILLISECOND, milli);

        return calendar;
    }

    public static Calendar getCalendarQuickSetup(int hourOfDay, int minute, int milli, int month,
                                           int dayOfMonth, int year) {
        // we are changing the month, day, and year so new york instance doesn't matter
        Calendar calendar = getNewYorkCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.MILLISECOND, milli);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        return calendar;
    }

    public static Stock getStockFromCursor(Cursor cursor){
        String symbol = cursor.getString(ListManipulator.INDEX_SYMBOL);
        String fullName = cursor.getString(ListManipulator.INDEX_FULL_NAME);
        float recentClose = cursor.getFloat(ListManipulator.INDEX_RECENT_CLOSE);
        float changeDollar = cursor.getFloat(ListManipulator.INDEX_CHANGE_DOLLAR);
        float changePercent = cursor.getFloat(ListManipulator.INDEX_CHANGE_PERCENT);
        int streak = cursor.getInt(ListManipulator.INDEX_STREAK);


        Stock stock = new Stock();
        stock.setSymbol(symbol);
        stock.setFullName(fullName);
        stock.setRecentClose(recentClose);
        stock.setChangeDollar(changeDollar);
        stock.setChangePercent(changePercent);
        stock.setStreak(streak);

        return stock;
    }

    /**
     * Used to determine is a symbol already exists in the database
     * @param symbol The symbol to look up
     * @param cr The ContentResolver to access your ContentProvider
     * @return true if exists, otherwise false
     */
    public static boolean isEntryExist(String symbol, ContentResolver cr){
        Cursor cursor = null;
        try{
            cursor = cr.query(StockEntry.buildUri(symbol), null, null, null, null);
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

    /**
     * Gets last update time from db
     * @param cr
     * @return returns the lastUpdateTime or null if not yet exist
     */
    public static Calendar getLastUpdateTime(ContentResolver cr) {
        Cursor cursor = null;
        Calendar lastUpdateTime = null;
        try {
            final String[] projection = {StockEntry.COLUMN_UPDATE_TIME_IN_MILLI};
            final int indexTimeInMilli = 0;

            cursor = cr.query(
                    StockEntry.CONTENT_URI,
                    projection,
                    null,
                    null,
                    StockProvider.ORDER_BY_ID_DESC_LIMIT_1);

            if (cursor != null && cursor.moveToFirst()) {
                lastUpdateTime = Utility.getNewYorkCalendarInstance();
                long updateTimeInMilli = cursor.getLong(indexTimeInMilli);
                lastUpdateTime.setTimeInMillis(updateTimeInMilli);
            }
        }finally {
            if(cursor!=null){
                cursor.close();
            }
        }
        return lastUpdateTime;
    }

    /**
     * Checks if the current time is between trading hours, regardless if stock market is closed or
     * not.
     * @return
     */
    public static boolean isDuringTradingHours(){
        //9:30am
        Calendar stockMarketOpen = Utility.getCalendarQuickSetup(
                Utility.STOCK_MARKET_OPEN_HOUR,
                Utility.STOCK_MARKET_OPEN_MINUTE,
                0);

        //4:30pm
        Calendar stockMarketClose = Utility.getCalendarQuickSetup(
                Utility.STOCK_MARKET_UPDATE_HOUR,
                Utility.STOCK_MARKET_UPDATE_MINUTE,
                0);

        Calendar nowTime = getNewYorkCalendarInstance();

        // If nowTime is between 9:30am EST and 4:30 pm EST
        // assume it is trading hours
        if(!nowTime.before(stockMarketOpen) && nowTime.before(stockMarketClose)){
            return true;
        }

        return false;
    }

    /**
     * Checks to see if the stock list is up to date, if not then update
     * @param cr ContentResolver to query db for the previous update time
     * @return true if list can be updated, else false
     */
    public static boolean canUpdateList(ContentResolver cr){

        Calendar lastUpdateTime = getLastUpdateTime(cr);
        if(lastUpdateTime == null) {
            return true;
        }

        Calendar nowTime = getNewYorkCalendarInstance();
        Calendar fourThirtyTime = Utility.getCalendarQuickSetup(
                Utility.STOCK_MARKET_UPDATE_HOUR,
                Utility.STOCK_MARKET_UPDATE_MINUTE,
                0);

        int dayOfWeek = nowTime.get(Calendar.DAY_OF_WEEK);

        // ALGORITHM:
        // Check to see if updateTime was before the last possible recent close. If so, update.
        // If nowTime is sunday or saturday
        // check if lastUpdateTime was before LAST FRIDAY @ 4:30pm EST, if so update.
        // If nowTime is monday < 4:30pm EST,
        // check if lastUpdateTime was before LAST LAST FRIDAY @ 4:30pm EST, if so update.
        // If nowTime(not monday) < 4:30pm EST,
        // check if lastUpdateTime was before YESTERDAY @ 4:30pm EST, if so update.
        // If nowTime >= 4:30pm EST,
        // check if lastUpdateTime was before TODAY @ 4:30pmEST, if so update.
        if ((dayOfWeek == Calendar.SATURDAY)) {
            // 1 days ago from Saturday is last Friday @ 4:30pm EST
            fourThirtyTime.add(Calendar.DAY_OF_MONTH, -1);

        } else if ((dayOfWeek == Calendar.SUNDAY)) {
            // 2 days ago from Sunday is last Friday @ 4:30pm EST
            fourThirtyTime.add(Calendar.DAY_OF_MONTH, -2);

        } else if(nowTime.before(fourThirtyTime)) {
            if (dayOfWeek == Calendar.MONDAY) {
                // 3 days ago from Monday is last Friday @ 4:30pm EST
                fourThirtyTime.add(Calendar.DAY_OF_MONTH, -3);
            } else{
                // 1 day ago is yesterday @ 4:30pm EST
                fourThirtyTime.add(Calendar.DAY_OF_MONTH, -1);
            }
        }

        //if lastUpdateTime is before the recentClose time, then update.
        if(lastUpdateTime.before(fourThirtyTime)){
            return true;
        }

        return false;
    }

    /**
     * @return true is should load from non latest data, else false
     * @throws IOException
     */
    public static boolean shouldLoadAFewNonLatest(ContentResolver cr) throws IOException{

        yahoofinance.Stock stock = YahooFinance.get("GOOG");
        Calendar recentCloseTime = stock.getQuote().getLastTradeTime();
        recentCloseTime.set(Calendar.HOUR_OF_DAY, STOCK_MARKET_UPDATE_HOUR);
        recentCloseTime.set(Calendar.MINUTE, STOCK_MARKET_UPDATE_MINUTE);
        recentCloseTime.set(Calendar.MILLISECOND, 0);

        Calendar lastUpdateTime = getLastUpdateTime(cr);
        if(lastUpdateTime == null){
            return false;
        }
        //If lastUpdateTime is before the most or would be recent close time then get the non latest
        return lastUpdateTime.before(recentCloseTime);
    }

    /**
     * Determines if the calendar is before 4:30pm of its respective day or not.
     * @param calendar calendar to test
     * @return
     */
    public static boolean isBeforeFourThirty(Calendar calendar){
        Calendar fourThirtyTime = getNewYorkCalendarInstance();
        fourThirtyTime.set(Calendar.HOUR_OF_DAY, STOCK_MARKET_UPDATE_HOUR);
        fourThirtyTime.set(Calendar.MINUTE, STOCK_MARKET_UPDATE_MINUTE);
        fourThirtyTime.set(Calendar.MILLISECOND, 0);

        return calendar.before(fourThirtyTime);
    }
}