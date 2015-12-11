package com.mcochin.stockstreaks.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.fragments.ListManipulatorFragment;

import java.util.ArrayList;

/**
 * Content Provider that gives us an interface to interact with the SQLite db.
 */
public class StockProvider extends ContentProvider {
    private StockDbHelper mStockDbHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final String TAG = StockProvider.class.getSimpleName();
    public static final String KEY_UPDATE_RESULTS = "updateResults";

    private static final String UNKNOWN_URI = "Unknown Uri: ";
    private static final String ERROR_ROW_INSERT = "Failed to insert row:  ";

    private static final int SAVE_STATE = 100;
    private static final int STOCKS = 200;
    private static final int STOCKS_WITH_SYMBOL = 201;

    // stocks.symbol = ?
    private static final String STOCK_SYMBOL_SELECTION =
            StockEntry.TABLE_NAME + "." + StockEntry.COLUMN_SYMBOL + " = ?";

    public static final String SHOWN_ID_BOOKMARK_SELECTION =
            StockEntry.TABLE_NAME + "." + StockEntry._ID + " >= ?";

    // ORDER BY _ID DESC
    public static final String ORDER_BY_ID_DESC = StockContract.StockEntry._ID + " DESC";

    private static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = StockContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, StockContract.PATH_SAVE_STATE, SAVE_STATE);
        matcher.addURI(authority, StockContract.PATH_STOCKS, STOCKS);
        matcher.addURI(authority, StockContract.PATH_STOCKS + "/*", STOCKS_WITH_SYMBOL);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "contentProvider create");
        mStockDbHelper = new StockDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match){
            case SAVE_STATE:
                return SaveStateEntry.CONTENT_DIR_TYPE;
            case STOCKS:
                return StockEntry.CONTENT_DIR_TYPE;
            case STOCKS_WITH_SYMBOL:
                return StockEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final int match = sUriMatcher.match(uri);
        Cursor retCursor;

        switch (match) {
            case SAVE_STATE:
                retCursor = mStockDbHelper.getWritableDatabase().query(
                        SaveStateEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case STOCKS:
                retCursor = mStockDbHelper.getWritableDatabase().query(
                        StockEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case STOCKS_WITH_SYMBOL:
                String symbol = StockContract.getSymbolFromUri(uri);

                retCursor = mStockDbHelper.getWritableDatabase().query(
                        StockEntry.TABLE_NAME,
                        projection,
                        STOCK_SYMBOL_SELECTION,
                        new String[]{symbol},
                        null,
                        null,
                        sortOrder);

                break;
            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        if(getContext()!= null) {
            // This will register an observer for the queried information
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return retCursor;
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        long id;

        switch (match){
            case SAVE_STATE:
                id = mStockDbHelper.getWritableDatabase()
                        .insert(SaveStateEntry.TABLE_NAME, null, values);
                break;

            case STOCKS_WITH_SYMBOL:
                id = mStockDbHelper.getWritableDatabase()
                        .insert(StockEntry.TABLE_NAME, null, values);
                break;

            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        if(id < 0){
            throw new SQLException(ERROR_ROW_INSERT + uri);
        }
        if(getContext()!= null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        switch (match){
            case STOCKS_WITH_SYMBOL:
                String symbol = StockContract.getSymbolFromUri(uri);

                rowsDeleted = mStockDbHelper.getWritableDatabase().delete(
                        StockEntry.TABLE_NAME, STOCK_SYMBOL_SELECTION, new String[]{symbol});
                break;

            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        if (rowsDeleted != 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int rowsAffected;

        switch (match){
            case SAVE_STATE:
                rowsAffected = mStockDbHelper.getWritableDatabase().update(
                        SaveStateEntry.TABLE_NAME,
                        values,
                        null,
                        null);
                break;

            case STOCKS_WITH_SYMBOL:
                String symbol = StockContract.getSymbolFromUri(uri);

                rowsAffected = mStockDbHelper.getWritableDatabase().update(
                        StockEntry.TABLE_NAME,
                        values,
                        STOCK_SYMBOL_SELECTION,
                        new String[]{symbol});
                break;

            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        if(getContext()!= null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsAffected;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        ContentProviderResult[] results =  super.applyBatch(operations);

        if(getContext() != null) {
            Intent updateBroadcast = new Intent(ListManipulatorFragment.BROADCAST_ACTION);
            updateBroadcast.putExtra(KEY_UPDATE_RESULTS, results[0]);
            getContext().sendBroadcast(updateBroadcast);
        }

        return results;
    }
}


