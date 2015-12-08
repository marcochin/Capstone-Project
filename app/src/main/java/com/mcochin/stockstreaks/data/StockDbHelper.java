package com.mcochin.stockstreaks.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;

/**
 * Manages a local database for stocks data.
 */
public class StockDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "stocks.db";

    public StockDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_STOCKS_TABLE = "CREATE TABLE " + StockEntry.TABLE_NAME + " (" +

                // Why AutoIncrement?
                // Unique keys will be auto-generated in either case. AutoIncrement just makes sure
                // the id will be greater than previous id.
                StockEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                StockEntry.COLUMN_SYMBOL + " TEXT NOT NULL, " +
                StockEntry.COLUMN_FULL_NAME + " TEXT NOT NULL, " +
                StockEntry.COLUMN_RECENT_CLOSE + " REAL, " +
                StockEntry.COLUMN_CHANGE_DOLLAR + " REAL, " +
                StockEntry.COLUMN_CHANGE_PERCENT + " REAL, " +
                StockEntry.COLUMN_STREAK + " INTEGER, " +
                StockEntry.COLUMN_PREV_STREAK_END_DATE + " INTEGER, " +
                StockEntry.COLUMN_PREV_STREAK_END_PRICE + " REAL, " +
                StockEntry.COLUMN_PREV_STREAK + " INTEGER, " +
                StockEntry.COLUMN_STREAK_YEAR_HIGH + " INTEGER, " +
                StockEntry.COLUMN_STREAK_YEAR_LOW + " INTEGER, " +
                StockEntry.COLUMN_UPDATE_TIME_IN_MILLI + " INTEGER, " +

                // All symbols should be unique
                " UNIQUE (" + StockEntry.COLUMN_SYMBOL + "));";

        db.execSQL(SQL_CREATE_STOCKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If you plan on adding/removing columns and the app is in production, do not drop tables
        // as it will delete all user data. Instead use ALTER TABLE.
        db.execSQL("DROP TABLE IF EXISTS " + StockEntry.TABLE_NAME);
        onCreate(db);
    }
}
