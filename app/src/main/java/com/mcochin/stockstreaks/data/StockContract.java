package com.mcochin.stockstreaks.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the stock streaks database.
 */
public class StockContract {

    /**The "Content authority" is a name for the entire content provider, similar to the
    relationship between a domain name and its website.  A convenient string to use for the
    content authority is the package name for the app, which is guaranteed to be unique on the
    device.*/
    public static final String CONTENT_AUTHORITY = "com.mcochin.stockstreaks";

    /** Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    the content provider.
    This full string will be: "content://com.mcochin.stockstreaks" */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /* Possible paths (appended to base content URI for possible URI's)
    For instance, content://com.mcochin.stockstreaks/stocks/ is a valid path for
    looking at stocks data. Simply put, these should be names of your tables! */
    public static final String PATH_UPDATE_DATE = "update_date";
    public static final String PATH_STOCKS = "stocks";

    /** Inner class that defines the table contents of the update_date table */
    public static final class UpdateDateEntry implements BaseColumns {

        // content://com.mcochin.stockstreaks/update_date
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_UPDATE_DATE).build();

        // There will only be one item in the update date table and that is the time_in_milli
        // "vnd.android.cursor.item/com.mcochin.stockstreaks/update_date
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                        + CONTENT_AUTHORITY + "/" + PATH_UPDATE_DATE;

        public static final String TABLE_NAME = PATH_UPDATE_DATE;
        public static final String COLUMN_TIME_IN_MILLI = "time_in_milli";

        /**
         * This will serve as the return URI for something like inserting a row.
         * @param id The id of the row
         * @return The URI of the row of the recent transaction.
         * e.g. content://com.mcochin.stockstreaks/update_date/123
         */
        public static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /** Inner class that defines the table contents of the update_date table */
    public static final class StockEntry implements BaseColumns {

        // content://com.mcochin.stockstreaks/stocks
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_STOCKS).build();

        // "vnd.android.cursor.dir/com.mcochin.stockstreaks/stocks
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                        + CONTENT_AUTHORITY + "/" + PATH_STOCKS;

        // "vnd.android.cursor.item/com.mcochin.stockstreaks/stocks
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
                        + CONTENT_AUTHORITY + "/" + PATH_STOCKS;

        public static final String TABLE_NAME = PATH_STOCKS;
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_FULL_NAME = "full_name";
        public static final String COLUMN_RECENT_CLOSE = "recent_close";
        public static final String COLUMN_CHANGE_DOLLAR = "change_dollar";
        public static final String COLUMN_CHANGE_PERCENT = "change_percent";
        public static final String COLUMN_STREAK = "streak";
        public static final String COLUMN_STREAK_ABSOLUTE_DAY_COVERAGE = "streak_absolute_day_coverage";
        public static final String COLUMN_PREV_STREAK_END_DATE = "prev_streak_end_date";
        public static final String COLUMN_PREV_STREAK_END_PRICE = "prev_streak_end_price";
        public static final String COLUMN_PREV_STREAK= "prev_streak";
        public static final String COLUMN_STREAK_YEAR_HIGH = "streak_year_high";
        public static final String COLUMN_STREAK_YEAR_LOW = "streak_year_low";

        /**
         * This will serve as the return URI for something like inserting a row.
         *
         * @param id The id of the row
         * @return The URI of the row of the recent transaction.
         * e.g. content://com.mcochin.stockstreaks/stocks/123
         */
        public static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        /**
         * This will serve as the return URI for something like inserting a row.
         *
         * @param symbol The symbol of the stock
         * @return The URI of the row of the recent transaction.
         * e.g. content://com.mcochin.stockstreaks/stocks/GPRO
         */
        public static Uri buildUri(String symbol) {
            return CONTENT_URI.buildUpon().appendPath(symbol).build();
        }
    }

    public static String getSymbolFromUri(Uri uri){
        // content://com.mcochin.stockstreaks/stocks/GPRO
        return uri.getPathSegments().get(1);
    }
}
