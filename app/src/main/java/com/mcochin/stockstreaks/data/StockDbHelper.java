package com.mcochin.stockstreaks.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.UpdateDateEntry;

/**
 * Manages a local database for stocks data.
 */
public class StockDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;
    static final String DATABASE_NAME = "stocks.db";

    public StockDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // NOTE: These are the column indexes if your return all columns in your projection
    public static final int INDEX_TIME_IN_MILLI = 1;

//    public static final int INDEX_SYMBOL = 1;
//    public static final int INDEX_FULL_NAME = INDEX_SYMBOL + 1;
//    public static final int INDEX_RECENT_CLOSE = INDEX_FULL_NAME + 1;
//    public static final int INDEX_CHANGE_DOLLAR = INDEX_PREV_CLOSE + 1;
//    public static final int INDEX_CHANGE_PERCENT = INDEX_CHANGE_DOLLAR + 1;
//    public static final int INDEX_STREAK = INDEX_CHANGE_PERCENT + 1;
//    public static final int INDEX_PREV_STREAK_END_PRICE = INDEX_STREAK + 1;
//    public static final int INDEX_PREV_STREAK = INDEX_PREV_STREAK_END_PRICE + 1;
//    public static final int INDEX_STREAK_YEAR_HIGH = INDEX_PREV_STREAK + 1;
//    public static final int INDEX_STREAK_YEAR_LOW = INDEX_STREAK_YEAR_HIGH + 1;

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_UPDATE_DATE_TABLE = "CREATE TABLE " + UpdateDateEntry.TABLE_NAME + " (" +
                UpdateDateEntry._ID + " INTEGER PRIMARY KEY," +
                UpdateDateEntry.COLUMN_TIME_IN_MILLI + " INTEGER NOT NULL " +
                " );";

        final String SQL_CREATE_STOCKS_TABLE = "CREATE TABLE " + StockEntry.TABLE_NAME + " (" +

                // Why no AutoIncrement?
                // Unique keys will be auto-generated in either case. AutoIncrement just makes sure
                // the id will be greater than previous id. We don't need that feature.
                StockEntry._ID + " INTEGER PRIMARY KEY," +
                StockEntry.COLUMN_SYMBOL + " TEXT NOT NULL, " +
                StockEntry.COLUMN_FULL_NAME + " TEXT NOT NULL, " +
                StockEntry.COLUMN_RECENT_CLOSE + " REAL, " +
                StockEntry.COLUMN_CHANGE_DOLLAR + " REAL, " +
                StockEntry.COLUMN_CHANGE_PERCENT + " REAL, " +
                StockEntry.COLUMN_STREAK + " INTEGER, " +
                StockEntry.COLUMN_STREAK_ABSOLUTE_DAY_COVERAGE + " INTEGER, " +
                StockEntry.COLUMN_PREV_STREAK_END_PRICE + " REAL, " +
                StockEntry.COLUMN_PREV_STREAK + " INTEGER, " +
                StockEntry.COLUMN_STREAK_YEAR_HIGH + " INTEGER, " +
                StockEntry.COLUMN_STREAK_YEAR_LOW + " INTEGER, " +

                // All symbols should be unique
                " UNIQUE (" + StockEntry.COLUMN_SYMBOL + "));";

        db.execSQL(SQL_CREATE_UPDATE_DATE_TABLE);
        db.execSQL(SQL_CREATE_STOCKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If you plan on adding/removing columns and the app is in production, do not drop tables
        // as it will delete all user data. Instead use ALTER TABLE.
        db.execSQL("DROP TABLE IF EXISTS " + UpdateDateEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StockEntry.TABLE_NAME);
        onCreate(db);
    }
}
