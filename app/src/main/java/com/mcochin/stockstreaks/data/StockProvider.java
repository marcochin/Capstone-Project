package com.mcochin.stockstreaks.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.UpdateDateEntry;

/**
 * Content Provider that gives us an interface to interact with the SQLite db.
 */
public class StockProvider extends ContentProvider {
    private StockDbHelper mStockDbHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static final int UPDATE_DATE = 100;
    static final int STOCKS = 200;
    static final int STOCKS_WITH_SYMBOL = 201;

    // stocks.symbol = ?
    private static final String STOCK_SYMBOL_SELCECTION =
            StockEntry.TABLE_NAME + "." + StockEntry.COLUMN_SYMBOL + " = ?";

    @Override
    public boolean onCreate() {
        mStockDbHelper = new StockDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match){
            case UPDATE_DATE:
                return UpdateDateEntry.CONTENT_ITEM_TYPE;
            case STOCKS:
                return StockEntry.CONTENT_DIR_TYPE;
            case STOCKS_WITH_SYMBOL:
                return StockEntry.CONTENT_ITEM_TYPE;
        }
        return null;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final int match = sUriMatcher.match(uri);

        switch (match){
            case UPDATE_DATE:
                break;
            case STOCKS:
                break;
            case STOCKS_WITH_SYMBOL:
                break;
        }
        return null;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);

        switch (match){
            case UPDATE_DATE:
                break;
            case STOCKS:
                break;
            case STOCKS_WITH_SYMBOL:
                break;
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);

        switch (match){
            case UPDATE_DATE:
                break;
            case STOCKS:
                break;
            case STOCKS_WITH_SYMBOL:
                break;
        }

        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);

        switch (match){
            case UPDATE_DATE:
                break;
            case STOCKS:
                break;
            case STOCKS_WITH_SYMBOL:
                break;
        }

        return 0;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        return super.bulkInsert(uri, values);
    }

    private Cursor getStockList(){
            return null;
    }

    private Cursor getUpdateDate(){
        return null;
    }

    private static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = StockContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, StockContract.PATH_UPDATE_DATE, UPDATE_DATE);
        matcher.addURI(authority, StockContract.PATH_STOCKS, STOCKS);
        matcher.addURI(authority, StockContract.PATH_STOCKS + "/*", STOCKS_WITH_SYMBOL);

        return matcher;
    }

}


