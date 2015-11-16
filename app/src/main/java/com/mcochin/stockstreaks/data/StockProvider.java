package com.mcochin.stockstreaks.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.UpdateDateEntry;

/**
 * Content Provider that gives us an interface to interact with the SQLite db.
 */
public class StockProvider extends ContentProvider {
    private StockDbHelper mStockDbHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int UPDATE_DATE = 100;
    private static final int STOCKS = 200;
    private static final int STOCKS_WITH_SYMBOL = 201;

    private static final String UNKNOWN_URI = "Unknown Uri: ";
    private static final String ERROR_ROW_INSERT = "Failed to insert row:  ";

    // stocks.symbol = ?
    private static final String STOCK_SYMBOL_SELECTION =
            StockEntry.TABLE_NAME + "." + StockEntry.COLUMN_SYMBOL + " = ?";

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
            case UPDATE_DATE:
                return UpdateDateEntry.CONTENT_DIR_TYPE;
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
            case UPDATE_DATE:
                retCursor = mStockDbHelper.getWritableDatabase().query(
                        UpdateDateEntry.TABLE_NAME,
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
            case UPDATE_DATE:
                id = mStockDbHelper.getWritableDatabase()
                        .insert(UpdateDateEntry.TABLE_NAME, null, values);

                break;

            case STOCKS_WITH_SYMBOL:
                id = mStockDbHelper.getWritableDatabase()
                        .insert(StockEntry.TABLE_NAME, null, values);

                break;

            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        if(id < 0){
            throw new android.database.SQLException(ERROR_ROW_INSERT + uri);
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
        int rowsAffected = 0;

        switch (match){
            case UPDATE_DATE:
                mStockDbHelper.getWritableDatabase()
                        .update(UpdateDateEntry.TABLE_NAME, values, null, null);
                break;
            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        if(getContext()!= null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsAffected;
    }

//    @NonNull
//    @Override
//    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
//        ContentProviderResult[] results =  super.applyBatch(operations);
//        if(getContext()!= null) {
//            getContext().getContentResolver().notifyChange(StockEntry.CONTENT_URI, null);
//        }
//
//        return results;
//    }

    //    /**
//     * Updates db items efficiently in a bulk.
//     * @param uri The content:// Uri of the update request.
//     * @param values An array of sets of column_name/value pairs to add to the database.
//     *               This must not be null.
//     * @return The number of values that were updated.
//     */
//    public int bulkUpdate(@NonNull Uri uri, ContentValues[] values) {
//        final int match = sUriMatcher.match(uri);
//        int rowsAffected = 0;
//
//        switch(match){
//            case STOCKS:
//                SQLiteDatabase db = mStockDbHelper.getWritableDatabase();
//                try {
//                    db.beginTransaction();
//                    for (ContentValues cv : values) {
//                        String symbol = cv.getAsString(StockEntry.COLUMN_SYMBOL);
//                        if (symbol != null) {
//                            db.update(
//                                    StockEntry.TABLE_NAME,
//                                    cv,
//                                    STOCK_SYMBOL_SELECTION,
//                                    new String[] {symbol}
//                            );
//                            // Update will only update one row since all symbols are unique
//                            rowsAffected++;
//                        }
//                    }
//                    db.setTransactionSuccessful();
//                } finally {
//                db.endTransaction();
//            }
//
//            break;
//        }
//
//        if(getContext()!= null) {
//            getContext().getContentResolver().notifyChange(uri, null);
//        }
//        return rowsAffected;
//    }

//    // http://stackoverflow.com/questions/9917935/adding-rows-into-cursor-manually
//    private Cursor mergeWithDateCursor(Cursor cursor){
//        Cursor dateCursor = null;
//
//        try{
//            dateCursor = mStockDbHelper.getWritableDatabase().query(
//                    UpdateDateEntry.TABLE_NAME, null, null, null, null, null, null);
//
//            return new MergeCursor(new Cursor[]{dateCursor, cursor});
//        } finally {
//            if (dateCursor != null){
//                dateCursor.close();
//            }
//        }
//    }

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


