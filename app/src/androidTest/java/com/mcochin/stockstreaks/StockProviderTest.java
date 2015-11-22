package com.mcochin.stockstreaks;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockDbHelper;
import com.mcochin.stockstreaks.data.StockProvider;

/**
 * Test for ContentProvider
 */
public class StockProviderTest extends ProviderTestCase2<StockProvider> {
    private String mSymbol = "GPRO";
    private String mFullName = "GoPro Inc.";
    private float mRecentClose = 20.00f;
    private int mStreak = 2;
    private float mChangeDollar = 21.00f;
    private float mChangePercent = 22.00f;
    private int mDayCoverage = 3;
    private float mPrevStreakEndPrice = 23.00f;
    private long mPrevStreakEndDate = 1349333576093L;
    private long mPrevStreak = 4;
    private long mYearStreakHigh = 5;
    private long mYearStreakLow = -6;

    private Uri uri = StockContract.StockEntry.buildUri(mSymbol);

    private static final String CURSOR_NULL = "Cursor is null";
    private static final String CURSOR_NO_FIRST = "Cursor could not move to first";

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
            values.put(StockContract.StockEntry.COLUMN_SYMBOL, mSymbol);
            values.put(StockContract.StockEntry.COLUMN_FULL_NAME, mFullName);
            values.put(StockContract.StockEntry.COLUMN_RECENT_CLOSE, mRecentClose);
            values.put(StockContract.StockEntry.COLUMN_STREAK, mStreak);
            values.put(StockContract.StockEntry.COLUMN_CHANGE_DOLLAR, mChangeDollar);
            values.put(StockContract.StockEntry.COLUMN_CHANGE_PERCENT, mChangePercent);
            values.put(StockContract.StockEntry.COLUMN_STREAK_ABSOLUTE_DAY_COVERAGE, mDayCoverage);
            values.put(StockContract.StockEntry.COLUMN_PREV_STREAK_END_PRICE, mPrevStreakEndPrice);
            values.put(StockContract.StockEntry.COLUMN_PREV_STREAK_END_DATE, mPrevStreakEndDate);

            // Insert mock data
            getMockContentResolver().insert(uri, values);

            // Query the inserted data
            cursor = getMockContentResolver().query(uri, null, null, null, null);

            if(cursor == null){
                fail(CURSOR_NULL);
            } else{
                if(cursor.moveToFirst()){
                    assertEquals(mSymbol, cursor.getString(StockDbHelper.INDEX_SYMBOL));
                    assertEquals(mFullName, cursor.getString(StockDbHelper.INDEX_FULL_NAME));
                    assertEquals(mRecentClose, cursor.getFloat(StockDbHelper.INDEX_RECENT_CLOSE));
                    assertEquals(mStreak, cursor.getInt(StockDbHelper.INDEX_STREAK));
                    assertEquals(mChangeDollar, cursor.getFloat(StockDbHelper.INDEX_CHANGE_DOLLAR));
                    assertEquals(mChangePercent, cursor.getFloat(StockDbHelper.INDEX_CHANGE_PERCENT));
                    assertEquals(mDayCoverage, cursor.getInt(StockDbHelper.COLUMN_STREAK_ABSOLUTE_DAY_COVERAGE));
                    assertEquals(mPrevStreakEndPrice, cursor.getFloat(StockDbHelper.INDEX_PREV_STREAK_END_PRICE));
                    assertEquals(mPrevStreakEndDate, cursor.getLong(StockDbHelper.COLUMN_PREV_STREAK_END_DATE));

                    assertEquals(0, cursor.getInt(StockDbHelper.INDEX_PREV_STREAK));
                    assertEquals(0, cursor.getInt(StockDbHelper.INDEX_STREAK_YEAR_HIGH));
                    assertEquals(0, cursor.getInt(StockDbHelper.INDEX_STREAK_YEAR_LOW));

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
        insertBareMinimumValues();

        int rowDeleted = getMockContentResolver().delete(uri, null, null);
        assertEquals(1, rowDeleted);
    }



    public void testUpdate(){
        Cursor cursor = null;

        try {
            // Insert an entry
            insertBareMinimumValues();

            ContentValues updateValues = new ContentValues();
            updateValues.put(StockContract.StockEntry.COLUMN_PREV_STREAK, mPrevStreak);
            updateValues.put(StockContract.StockEntry.COLUMN_STREAK_YEAR_HIGH, mYearStreakHigh);
            updateValues.put(StockContract.StockEntry.COLUMN_STREAK_YEAR_LOW, mYearStreakLow);

            // Update values
            getMockContentResolver().update(uri, updateValues, null, null);

            // Query the data
            cursor = getMockContentResolver().query(uri, null, null, null, null);

            if(cursor == null){
                fail(CURSOR_NULL);
            } else {

                // test to see if updated values are what is expected
                if (cursor.moveToFirst()) {
                    assertEquals(mPrevStreak, cursor.getInt(StockDbHelper.INDEX_PREV_STREAK));
                    assertEquals(mYearStreakHigh, cursor.getInt(StockDbHelper.INDEX_STREAK_YEAR_HIGH));
                    assertEquals(mYearStreakLow, cursor.getInt(StockDbHelper.INDEX_STREAK_YEAR_LOW));
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

    private void testBulkUpdate(){

    }

    public void testUniqueSymbol(){
        Cursor cursor = null;

        try {
            // Insert an entry
            insertBareMinimumValues();

            // Query it to see if it exists
            cursor = getMockContentResolver().query(uri, null, null, null, null);

            // If exists that means entry exists
            if(cursor!= null && cursor.moveToFirst()){
                assertTrue(true);
            }

        }finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    private void insertBareMinimumValues(){
        ContentValues values = new ContentValues();
        values.put(StockContract.StockEntry.COLUMN_SYMBOL, mSymbol);
        values.put(StockContract.StockEntry.COLUMN_FULL_NAME, mFullName);

        getMockContentResolver().insert(uri, values);
    }

}
