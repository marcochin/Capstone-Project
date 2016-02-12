package com.mcochin.stockstreaks.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pair;
import android.widget.Toast;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.pojos.Stock;

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
     * Calculates the change in dollars and percentage between the two prices.
     *
     * @param recentClose        Stock's recent close
     * @param prevStreakEndPrice Previous streak's end price for the stock
     * @return A Pair containing:
     * <ul>
     * <li>Pair.first is the change in dollars</li>
     * <li>Pair.second is the change in percentage</li>
     * </ul>
     */
    public static Pair<Float, Float> calculateChange(float recentClose, float prevStreakEndPrice) {
        float changeDollar = recentClose - prevStreakEndPrice;
        float changePercent = changeDollar / prevStreakEndPrice * 100;

        return new Pair<>(changeDollar, changePercent);
    }


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
     * Detects to see if specified service is running.
     *
     * @param manager ActivityManager
     * @return returns true if running, false otherwise
     */
    public static boolean isServiceRunning(ActivityManager manager, String serviceName) {
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is intended for threads to show toast messages on the main thread.
     *
     * @param context
     * @param toastMsg
     */
    public static void showToast(final Context context, final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * @return a {@link Calendar} instance with {@link TimeZone} set to New York.
     */
    public static Calendar getNewYorkCalendarInstance() {
        return Calendar.getInstance(TimeZone.getTimeZone(YahooFinance.TIMEZONE));
    }

    /**
     * Resets the Calendar to midnight.
     *
     * @param calendar
     * @return
     */
    public static Calendar calendarTimeReset(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    /**
     * Quick way to set the {@link Calendar} time without having to use the
     * {@link Calendar#set(int, int)}
     * method.
     *
     * @param hourOfDay
     * @param minute
     * @param sec
     * @param milli
     * @return
     */
    public static Calendar getNewYorkCalendarQuickSetup(int hourOfDay, int minute, int sec, int milli) {
        Calendar calendar = getNewYorkCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, milli);

        return calendar;
    }

    /**
     * Extracts the {@link Stock} from the cursor's current position.
     *
     * @param cursor
     * @return
     */
    public static Stock getStockFromCursor(Cursor cursor) {
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
     * Gets last update time from db
     *
     * @param cr
     * @return returns the lastUpdateTime or null if not yet exist
     */
    public static Calendar getLastUpdateTime(ContentResolver cr) {
        Cursor cursor = null;
        Calendar lastUpdateTime = null;
        try {
            final String[] projection = {SaveStateEntry.COLUMN_UPDATE_TIME_IN_MILLI};
            final int indexTimeInMilli = 0;

            cursor = cr.query(
                    SaveStateEntry.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                lastUpdateTime = Calendar.getInstance();
                long updateTimeInMilli = cursor.getLong(indexTimeInMilli);
                lastUpdateTime.setTimeInMillis(updateTimeInMilli);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return lastUpdateTime;
    }

    /**
     * Gets the last shown position + 1. The bookmark is always 1 above the actual position.
     *
     * @param cr
     * @return returns the last shown position or -1 if not yet exist
     */
    public static int getShownPositionBookmark(ContentResolver cr) {
        Cursor cursor = null;
        int shownPosBookmark = -1;
        try {
            final String[] projection = {SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK};
            final int indexBookmark = 0;

            cursor = cr.query(
                    SaveStateEntry.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                shownPosBookmark = cursor.getInt(indexBookmark);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return shownPosBookmark;
    }

    /**
     * Checks if the current time is between trading hours, regardless if stock market is closed or
     * not.
     *
     * @return
     */
    public static boolean isDuringTradingHours() {
        //9:30am
        Calendar stockMarketOpen = Utility.getNewYorkCalendarQuickSetup(
                Utility.STOCK_MARKET_OPEN_HOUR,
                Utility.STOCK_MARKET_OPEN_MINUTE,
                0,
                0);

        //4:30pm
        Calendar stockMarketClose = Utility.getNewYorkCalendarQuickSetup(
                Utility.STOCK_MARKET_UPDATE_HOUR,
                Utility.STOCK_MARKET_UPDATE_MINUTE,
                0,
                0);

        Calendar nowTime = getNewYorkCalendarInstance();

        // If nowTime is between 9:30am EST and 4:30 pm EST
        // assume it is trading hours
        if (!nowTime.before(stockMarketOpen) && nowTime.before(stockMarketClose)) {
            return true;
        }

        return false;
    }

    /**
     * Checks to see if the stock list is up to date, if not then update
     *
     * @param cr ContentResolver to query db for the previous update time
     * @return true if list can be updated, else false
     */
    public static boolean canUpdateList(ContentResolver cr) {
        // NOTE: This method can be a problem for those holidays where the market is only open
        // for half a day. For best accuracy, we need to make an api call to get the last trade day
        // to compare w/ our updateTime. However, I think this is good enough.
        Calendar lastUpdateTime = getLastUpdateTime(cr);
        if (lastUpdateTime == null) {
            return true;
        }

        Calendar nowTime = getNewYorkCalendarInstance();
        Calendar newYorkFourThirtyTime = Utility.getNewYorkCalendarQuickSetup(
                Utility.STOCK_MARKET_UPDATE_HOUR,
                Utility.STOCK_MARKET_UPDATE_MINUTE,
                0,
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
            newYorkFourThirtyTime.add(Calendar.DAY_OF_MONTH, -1);

        } else if ((dayOfWeek == Calendar.SUNDAY)) {
            // 2 days ago from Sunday is last Friday @ 4:30pm EST
            newYorkFourThirtyTime.add(Calendar.DAY_OF_MONTH, -2);

        } else if (nowTime.before(newYorkFourThirtyTime)) {
            if (dayOfWeek == Calendar.MONDAY) {
                // 3 days ago from Monday is last Friday @ 4:30pm EST
                newYorkFourThirtyTime.add(Calendar.DAY_OF_MONTH, -3);
            } else {
                // 1 day ago is yesterday @ 4:30pm EST
                newYorkFourThirtyTime.add(Calendar.DAY_OF_MONTH, -1);
            }
        }

        //if lastUpdateTime is before the recentClose time, then update.
        if (lastUpdateTime.before(newYorkFourThirtyTime)) {
            return true;
        }

        return false;
    }

    /**
     * Determines a stock's change color, e.g. green for up and red for down. It also determines
     * which image id to use for the stock arrow image.
     *
     * @param streak The change amount of a stock
     * @return Pair.first = colorId. Pair.second = arrowDrawableId.
     */
    public static Pair<Integer, Integer> getChangeColorAndArrowDrawableIds(int streak) {
        Integer colorId;
        Integer arrowDrawableId;

        // Get our dollar/percent change colors and set our stock arrow ImageView
        if (streak > 0) {
            colorId = R.color.stock_up_green;
            arrowDrawableId = R.drawable.ic_streak_up;

        } else if (streak < 0) {
            colorId = R.color.stock_down_red;
            arrowDrawableId = R.drawable.ic_streak_down;

        } else {
            colorId = R.color.stock_neutral;
            arrowDrawableId = 0;
        }

        return new Pair<>(colorId, arrowDrawableId);
    }

    /**
     * Rounds a float to two decimals.
     *
     * @param f the float to round
     * @return the float rounded to two decimals.
     */
    public static float roundTo2FloatDecimals(float f) {
        return Float.parseFloat(roundTo2StringDecimals(f));
    }

    /**
     * Rounds a float to two decimals.
     *
     * @param f the float to round
     * @return a String representation of the float rounded to two decimals.
     */
    public static String roundTo2StringDecimals(float f) {
        return String.format("%.2f", f);
    }

    /**
     * Compat version of the {@link Integer#compare(int, int)} that works in API < 19
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public static int compare(int lhs, int rhs) {
        return (lhs < rhs) ? -1 : ((lhs == rhs) ? 0 : 1);
    }
}