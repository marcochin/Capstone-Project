package com.mcochin.stockstreaks.data;

import android.database.Cursor;
import android.util.Log;

import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>ListManipulator</code> holds the data for our list and provides the necessary methods
 * to manipulate that list.
 */
public class ListManipulator {
    private static final String TAG = ListManipulator.class.getSimpleName();

//    private static final String[] FRUITS = new String[] { "Apple", "Avocado", "Banana",
//            "Blueberry", "Coconut", "Durian", "Guava", "Kiwi", "Jackfruit", "Mango",
//            "Olive", "Pear", "Sugar-apple", "Orange", "Strawberry", "Pineapple",
//            "Watermelon", "Grape", "PassionFruit", "DragonFruit", "Honey-dew",
//            "Cantaloupe", "Papaya"};

    public static final String[] STOCK_PROJECTION = new String[]{
            StockEntry.COLUMN_SYMBOL,
            StockEntry.COLUMN_FULL_NAME,
            StockEntry.COLUMN_RECENT_CLOSE,
            StockEntry.COLUMN_CHANGE_DOLLAR,
            StockEntry.COLUMN_CHANGE_PERCENT,
            StockEntry.COLUMN_STREAK
    };

    //index must match projection
    private static final int INDEX_SYMBOL = 0;
    private static final int INDEX_FULL_NAME = 1;
    private static final int INDEX_RECENT_CLOSE = 2;
    private static final int INDEX_CHANGE_DOLLAR = 3;
    private static final int INDEX_CHANGE_PERCENT = 4;
    private static final int INDEX_STREAK = 5;

    private List<Stock> mData = new ArrayList<>();
    private Stock mLastRemovedItem = null;
    private int mLastRemovedPosition = -1;
    private int uniqueId = 0;

    public void setCursor(Cursor cursor){
    //TODO remove this, Using fruits array just to debug
//        for(String fruit : FRUITS){
//            Stock stock = new Stock();
//            stock.setSymbol(fruit);
//            stock.setId(uniqueId);
//
//            mData.add(stock);
//            uniqueId++;
//        }

        if(cursor == null) {
            return;
        }

        if(cursor.moveToNext()) {
            uniqueId = 0;
            mData.clear();

            do{
                Stock stock = getStockFromCursor(cursor);
                mData.add(stock);
                uniqueId++;
            }while(cursor.moveToNext());
        }
    }

    public int getCount(){
        return mData.size();
    }

    public void addCursorItem(Cursor cursorItem){
        if(cursorItem.moveToFirst()){
            Stock stock = getStockFromCursor(cursorItem);
            //need to set id or the added item will contain text from the previous last item
            stock.setId(uniqueId++);
            mData.add(stock);
        }
    }

    public void addItem(Stock stock){
        stock.setId(uniqueId++);
        mData.add(stock);
    }

    public Stock getItem(int index) {
        if (index < 0 || index >= getCount()) {
            throw new IndexOutOfBoundsException("index = " + index);
        }
        return mData.get(index);
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        Stock stock = mData.remove(fromPosition);
        mData.add(toPosition, stock);
    }

    public void removeItem(int position) {
        mLastRemovedItem = mData.remove(position);
        mLastRemovedPosition = position;
    }

    public int undoLastRemoveItem() {
        if (mLastRemovedItem != null) {
            int insertedPosition;
            if (mLastRemovedPosition >= 0 && mLastRemovedPosition < mData.size()) {
                insertedPosition = mLastRemovedPosition;
            } else {
                insertedPosition = mData.size();
            }

            mData.add(insertedPosition, mLastRemovedItem);

            mLastRemovedItem = null;
            mLastRemovedPosition = -1;

            return insertedPosition;
        } else {
            return -1;
        }
    }

    private Stock getStockFromCursor(Cursor cursor){
        String symbol = cursor.getString(INDEX_SYMBOL);
        String fullName = cursor.getString(INDEX_FULL_NAME);
        float recentClose = cursor.getFloat(INDEX_RECENT_CLOSE);
        float changeDollar = cursor.getFloat(INDEX_CHANGE_DOLLAR);
        float changePercent = cursor.getFloat(INDEX_CHANGE_PERCENT);
        int streak = cursor.getInt(INDEX_STREAK);


        Stock stock = new Stock();
        stock.setId(uniqueId);
        stock.setSymbol(symbol);
        stock.setFullName(fullName);
        stock.setRecentClose(recentClose);
        stock.setChangeDollar(changeDollar);
        stock.setChangePercent(changePercent);
        stock.setStreak(streak);

        return stock;
    }
}
