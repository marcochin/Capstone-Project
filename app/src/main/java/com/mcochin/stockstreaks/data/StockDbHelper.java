package com.mcochin.stockstreaks.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;

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
        final String SQL_CREATE_SAVE_STATE_TABLE = "CREATE TABLE " + SaveStateEntry.TABLE_NAME + " (" +
                SaveStateEntry._ID + " INTEGER PRIMARY KEY, " +
                SaveStateEntry.COLUMN_UPDATE_TIME_IN_MILLI + " INTEGER, " +
                SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK + " INTEGER " + ");";

        final String SQL_CREATE_STOCKS_TABLE = "CREATE TABLE " + StockEntry.TABLE_NAME + " (" +

                // Why AutoIncrement?
                // Unique keys will be auto-generated in either case. AutoIncrement just makes sure
                // the id will be greater than previous id.
                StockEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                StockEntry.COLUMN_LIST_POSITION + " INTEGER DEFAULT -1, " +
                StockEntry.COLUMN_SYMBOL + " TEXT NOT NULL, " +
                StockEntry.COLUMN_FULL_NAME + " TEXT NOT NULL, " +
                StockEntry.COLUMN_RECENT_CLOSE + " REAL DEFAULT 0, " +
                StockEntry.COLUMN_CHANGE_DOLLAR + " REAL DEFAULT 0, " +
                StockEntry.COLUMN_CHANGE_PERCENT + " REAL DEFAULT 0, " +
                StockEntry.COLUMN_STREAK + " INTEGER DEFAULT 0, " +
                StockEntry.COLUMN_PREV_STREAK_END_DATE + " INTEGER DEFAULT 0, " +
                StockEntry.COLUMN_PREV_STREAK_END_PRICE + " REAL DEFAULT 0, " +
                StockEntry.COLUMN_PREV_STREAK + " INTEGER DEFAULT 0, " +
                StockEntry.COLUMN_STREAK_YEAR_HIGH + " INTEGER DEFAULT 0, " +
                StockEntry.COLUMN_STREAK_YEAR_LOW + " INTEGER DEFAULT 0, " +
                StockEntry.COLUMN_STREAK_CHART_MAP + " TEXT DEFAULT '', " +

                // All symbols should be unique
                " UNIQUE (" + StockEntry.COLUMN_SYMBOL + "));";

        db.execSQL(SQL_CREATE_SAVE_STATE_TABLE);
        db.execSQL(SQL_CREATE_STOCKS_TABLE);

        // Initialize starting update time and bookmark position so it only updates never inserts
        ContentValues values = new ContentValues();
        values.put(SaveStateEntry.COLUMN_UPDATE_TIME_IN_MILLI, 0);
        values.put(SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK, 0);
        db.insert(SaveStateEntry.TABLE_NAME, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If you plan on adding/removing columns and the app is in production, do not drop tables
        // as it will delete all user data. Instead use ALTER TABLE.
        db.execSQL("DROP TABLE IF EXISTS " + SaveStateEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + StockEntry.TABLE_NAME);
        onCreate(db);
    }
}
