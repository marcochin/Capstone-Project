package com.mcochin.stockstreaks;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.ProviderTestCase2;

import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;

import java.util.ArrayList;

/**
 * Test for ContentProvider
 */
public class StockProviderTest extends ProviderTestCase2<StockProvider> {
    // NOTE: These are the column indexes if your return all columns in your projection
    public static final int INDEX_SYMBOL = 1;
    public static final int INDEX_FULL_NAME = INDEX_SYMBOL + 1;
    public static final int INDEX_RECENT_CLOSE = INDEX_FULL_NAME + 1;
    public static final int INDEX_CHANGE_DOLLAR = INDEX_RECENT_CLOSE + 1;
    public static final int INDEX_CHANGE_PERCENT = INDEX_CHANGE_DOLLAR + 1;
    public static final int INDEX_STREAK = INDEX_CHANGE_PERCENT + 1;
    public static final int COLUMN_PREV_STREAK_END_DATE = INDEX_STREAK + 1;
    public static final int INDEX_PREV_STREAK_END_PRICE = COLUMN_PREV_STREAK_END_DATE + 1;
    public static final int INDEX_PREV_STREAK = INDEX_PREV_STREAK_END_PRICE + 1;
    public static final int INDEX_STREAK_YEAR_HIGH = INDEX_PREV_STREAK + 1;
    public static final int INDEX_STREAK_YEAR_LOW = INDEX_STREAK_YEAR_HIGH + 1;

    private static final String CURSOR_NULL = "Cursor is null";
    private static final String CURSOR_NO_FIRST = "Cursor could not move to first";

    private static final String SYMBOL = "GPRO";
    private static final String FULL_NAME = "GoPro Inc.";
    private static final float RECENT_CLOSE = 20.00f;
    private static final int STREAK = 2;
    private static final float CHANGE_DOLLAR = 21.00f;
    private static final float CHANGE_PERCENT = 22.00f;
    private static final float PREV_STREAK_END_PRICE = 23.00f;
    private static final long PREV_STREAK_END_DATE = 1349333576093L;
    private static final long PREV_STREAK = 4;
    private static final long YEAR_STREAK_HIGH = 5;
    private static final long YEAR_STREAK_LOW = -6;

    private Uri mUri = StockEntry.buildUri(SYMBOL);

    public StockProviderTest() {
        super(StockProvider.class, StockContract.CONTENT_AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testQuery() {
        Cursor cursor = null;
        try {

            ContentValues values = new ContentValues();
            values.put(StockEntry.COLUMN_SYMBOL, SYMBOL);
            values.put(StockEntry.COLUMN_FULL_NAME, FULL_NAME);
            values.put(StockEntry.COLUMN_RECENT_CLOSE, RECENT_CLOSE);
            values.put(StockEntry.COLUMN_STREAK, STREAK);
            values.put(StockEntry.COLUMN_CHANGE_DOLLAR, CHANGE_DOLLAR);
            values.put(StockEntry.COLUMN_CHANGE_PERCENT, CHANGE_PERCENT);
            values.put(StockEntry.COLUMN_PREV_STREAK_END_PRICE, PREV_STREAK_END_PRICE);
            values.put(StockEntry.COLUMN_PREV_STREAK_END_DATE, PREV_STREAK_END_DATE);

            // Insert mock data
            getMockContentResolver().insert(mUri, values);

            // Query the inserted data
            cursor = getMockContentResolver().query(mUri, null, null, null, null);

            if(cursor == null){
                fail(CURSOR_NULL);
            } else{
                if(cursor.moveToFirst()){
                    assertEquals(SYMBOL, cursor.getString(INDEX_SYMBOL));
                    assertEquals(FULL_NAME, cursor.getString(INDEX_FULL_NAME));
                    assertEquals(RECENT_CLOSE, cursor.getFloat(INDEX_RECENT_CLOSE));
                    assertEquals(STREAK, cursor.getInt(INDEX_STREAK));
                    assertEquals(CHANGE_DOLLAR, cursor.getFloat(INDEX_CHANGE_DOLLAR));
                    assertEquals(CHANGE_PERCENT, cursor.getFloat(INDEX_CHANGE_PERCENT));
                    assertEquals(PREV_STREAK_END_PRICE, cursor.getFloat(INDEX_PREV_STREAK_END_PRICE));
                    assertEquals(PREV_STREAK_END_DATE, cursor.getLong(COLUMN_PREV_STREAK_END_DATE));

                    assertEquals(0, cursor.getInt(INDEX_PREV_STREAK));
                    assertEquals(0, cursor.getInt(INDEX_STREAK_YEAR_HIGH));
                    assertEquals(0, cursor.getInt(INDEX_STREAK_YEAR_LOW));

                }else{
                    fail(CURSOR_NO_FIRST);
                }
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    public void testDelete(){
        // Insert an entry
        ContentValues values = createBareMinimumValues(SYMBOL, FULL_NAME);
        getMockContentResolver().insert(mUri, values);

        int rowDeleted = getMockContentResolver().delete(mUri, null, null);
        assertEquals(1, rowDeleted);
    }

    public void testUpdate(){
        Cursor cursor = null;

        try {
            // Insert an entry
            ContentValues values = createBareMinimumValues(SYMBOL, FULL_NAME);
            getMockContentResolver().insert(mUri, values);

            ContentValues updateValues = new ContentValues();
            updateValues.put(StockEntry.COLUMN_PREV_STREAK, PREV_STREAK);
            updateValues.put(StockEntry.COLUMN_STREAK_YEAR_HIGH, YEAR_STREAK_HIGH);
            updateValues.put(StockEntry.COLUMN_STREAK_YEAR_LOW, YEAR_STREAK_LOW);

            // Update values
            getMockContentResolver().update(mUri, updateValues, null, null);

            // Query the data
            cursor = getMockContentResolver().query(mUri, null, null, null, null);

            if(cursor == null){
                fail(CURSOR_NULL);
            } else {

                // Test to see if updated values are what is expected
                if (cursor.moveToFirst()) {
                    assertEquals(PREV_STREAK, cursor.getInt(INDEX_PREV_STREAK));
                    assertEquals(YEAR_STREAK_HIGH, cursor.getInt(INDEX_STREAK_YEAR_HIGH));
                    assertEquals(YEAR_STREAK_LOW, cursor.getInt(INDEX_STREAK_YEAR_LOW));
                } else {
                    fail(CURSOR_NO_FIRST);
                }
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    public void testBulkUpdate(){
        String symbolNke = "NKE";
        String symbolUa = "UA";

        String fullNameNke = "NIKE, Inc.";
        String fullNameUa = "Under Armour, Inc.";

        Uri uriNke = StockEntry.buildUri(symbolNke);
        Uri uriUa = StockEntry.buildUri(symbolUa);

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        Cursor cursor = null;
        int streak = 5;

        // Create out insert operations for GPRO, NKE, and UA
        ContentValues valuesGpro = createBareMinimumValues(SYMBOL, FULL_NAME);
        ContentValues valuesNke = createBareMinimumValues(symbolNke, fullNameNke);
        ContentValues valuesUa = createBareMinimumValues(symbolUa, fullNameUa);

        operations.add(ContentProviderOperation.newInsert(mUri).withValues(valuesGpro)
                .withYieldAllowed(true).build());
        operations.add(ContentProviderOperation.newInsert(uriNke).withValues(valuesNke)
                .withYieldAllowed(true).build());
        operations.add(ContentProviderOperation.newInsert(uriUa).withValues(valuesUa)
                .withYieldAllowed(true).build());

        // Create our update operations for GPRO, NKE, and UA
        operations.add(ContentProviderOperation.newUpdate(mUri)
                .withValue(StockEntry.COLUMN_STREAK, streak)
                .withYieldAllowed(true).build());

        operations.add(ContentProviderOperation.newUpdate(uriNke)
                .withValue(StockEntry.COLUMN_STREAK, streak)
                .withYieldAllowed(true).build());

        operations.add(ContentProviderOperation.newUpdate(uriUa)
                .withValue(StockEntry.COLUMN_STREAK, streak)
                .withYieldAllowed(true).build());

        // Apply the operations
        try {
            getMockContentResolver().applyBatch(StockContract.CONTENT_AUTHORITY, operations);
            cursor = getMockContentResolver().query(StockEntry.CONTENT_URI, null, null, null, null);

            if(cursor == null){
                fail(CURSOR_NULL);

            } else {
                // Assert updated rows are what is expected
                while(cursor.moveToNext()){
                    assertEquals(streak, cursor.getInt(INDEX_STREAK));
                }
            }
        }catch (RemoteException | OperationApplicationException e){
            fail("Apply batch failed");

        }finally {
            if (cursor != null){
                cursor.close();
            }
        }
    }

    public void testUniqueSymbol(){
        Cursor cursor = null;

        try {
            // Insert an entry
            ContentValues values = createBareMinimumValues(SYMBOL, FULL_NAME);
            getMockContentResolver().insert(mUri, values);

            // Query it to see if it exists
            cursor = getMockContentResolver().query(mUri, null, null, null, null);

            // If moveToFirst that means entry exists
            if(cursor!= null && cursor.moveToFirst()){
                assertTrue(true);
            }

        }finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    private ContentValues createBareMinimumValues(String symbol, String fullName){
        ContentValues values = new ContentValues();
        values.put(StockEntry.COLUMN_SYMBOL, symbol);
        values.put(StockEntry.COLUMN_FULL_NAME, fullName);

        return values;
    }

}
