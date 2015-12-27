package com.mcochin.stockstreaks.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.pojos.LoadAFewFinishedEvent;
import com.mcochin.stockstreaks.pojos.LoadSymbolFinishedEvent;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Content Provider that gives us an interface to interact with the SQLite db.
 */
public class StockProvider extends ContentProvider {
    private StockDbHelper mStockDbHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final String TAG = StockProvider.class.getSimpleName();
    public static final String KEY_OPERATIONS = "operations";

    public static final String METHOD_INSERT_ITEM = "insertItem";
    public static final String METHOD_UPDATE_ITEMS = "updateItems";
    public static final String METHOD_UPDATE_LIST_POSITION = "updateListPosition";

    private static final String UNKNOWN_URI = "Unknown Uri: ";
    private static final String ERROR_ROW_INSERT = "Failed to insert row: ";

    private static final int SAVE_STATE = 100;
    private static final int STOCKS = 200;
    private static final int STOCKS_WITH_SYMBOL = 201;

    // stocks.symbol = ?
    private static final String STOCK_SYMBOL_SELECTION =
            StockEntry.TABLE_NAME + "." + StockEntry.COLUMN_SYMBOL + " = ?";

    // stocks.list_position < ?
    public static final String SHOWN_POSITION_BOOKMARK_SELECTION =
            StockEntry.TABLE_NAME + "." + StockEntry.COLUMN_LIST_POSITION + " < ?";

    // list_position ASC
    public static final String ORDER_BY_LIST_POSITION_ASC = StockEntry.COLUMN_LIST_POSITION + " ASC";

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

    @Nullable
    @Override
    public Bundle call(@NonNull String method, String arg, Bundle extras) {
            if(extras != null) {
                ArrayList<ContentProviderOperation> operations =
                        extras.getParcelableArrayList(KEY_OPERATIONS);

                if (operations != null) {
                    try {
                        applyBatch(operations);

                        switch (method) {
                            case METHOD_INSERT_ITEM:
                                performInsertItem(operations);
                                break;

                            case METHOD_UPDATE_ITEMS:
                                performUpdateItems(operations);
                                break;

                            case METHOD_UPDATE_LIST_POSITION:
                                //Do nothing
                                break;
                        }
                    }catch (OperationApplicationException e){
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            }

        return super.call(method, arg, extras);
    }

    private void performInsertItem(ArrayList<ContentProviderOperation> ops){
        List<Stock> stockList = loopThroughOperations(ops);
        LoadSymbolFinishedEvent event = new LoadSymbolFinishedEvent(stockList.get(0), true);
        ListEventQueue.getInstance().post(event);
    }

    private void performUpdateItems(ArrayList<ContentProviderOperation> ops){
        List<Stock> stockList = loopThroughOperations(ops);
        LoadAFewFinishedEvent event = new LoadAFewFinishedEvent(stockList, true);
        ListEventQueue.getInstance().post(event);
    }

    private List<Stock> loopThroughOperations(ArrayList<ContentProviderOperation> ops){
        List<Stock> stockList = new ArrayList<>();
        Cursor cursor = null;

        //Loop through results
        for (ContentProviderOperation op : ops) {
            try {
                // Filter out save state Uri
                if (!StockContract.isSaveStateUri(op.getUri())) {
                    // Query the stocks from db
                    cursor = query(op.getUri(),
                            ListManipulator.STOCK_PROJECTION,
                            null,
                            null,
                            null);

                    // Insert stock in shownList
                    if (cursor != null && cursor.moveToFirst()) {
                        Stock stock = Utility.getStockFromCursor(cursor);
                        stockList.add(stock);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return stockList;
    }
}


