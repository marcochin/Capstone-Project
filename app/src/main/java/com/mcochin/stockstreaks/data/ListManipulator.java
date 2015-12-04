package com.mcochin.stockstreaks.data;

import android.database.Cursor;

import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.utils.Utility;

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
    public static final int INDEX_SYMBOL = 0;
    public static final int INDEX_FULL_NAME = 1;
    public static final int INDEX_RECENT_CLOSE = 2;
    public static final int INDEX_CHANGE_DOLLAR = 3;
    public static final int INDEX_CHANGE_PERCENT = 4;
    public static final int INDEX_STREAK = 5;

    private List<Stock> mShownList = new ArrayList<>();
    private String[] mLoadList;

    private Stock mLastRemovedItem = null;
    private int mLastRemovedPosition = -1;
    private int uniqueId = 0;

    public void setShownListCursor(Cursor shownListCursor){
    //TODO remove this, Using fruits array just to debug
//        for(String fruit : FRUITS){
//            Stock stock = new Stock();
//            stock.setSymbol(fruit);
//            stock.setId(uniqueId);
//
//            mShownList.add(stock);
//            uniqueId++;
//        }

        if(shownListCursor.moveToNext()) {
            uniqueId = 0;
            mShownList.clear();

            do{
                Stock stock = Utility.getStockFromCursor(shownListCursor);
                mShownList.add(stock);
                uniqueId++;
            }while(shownListCursor.moveToNext());
        }
    }

    public void setLoadList(String[] loadList){
        mLoadList = loadList;
    }

    public int getCount(){
        return mShownList.size();
    }

    public void addItem(Stock stock){
        stock.setId(uniqueId++);
        mShownList.add(stock);
    }

    public Stock getItem(int index) {
        if (index < 0 || index >= getCount()) {
            throw new IndexOutOfBoundsException("index = " + index);
        }
        return mShownList.get(index);
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        Stock stock = mShownList.remove(fromPosition);
        mShownList.add(toPosition, stock);
    }

    public void removeItem(int position) {
        mLastRemovedItem = mShownList.remove(position);
        mLastRemovedPosition = position;
    }

    public int undoLastRemoveItem() {
        if (mLastRemovedItem != null) {
            int insertedPosition;
            if (mLastRemovedPosition >= 0 && mLastRemovedPosition < mShownList.size()) {
                insertedPosition = mLastRemovedPosition;
            } else {
                insertedPosition = mShownList.size();
            }

            mShownList.add(insertedPosition, mLastRemovedItem);

            mLastRemovedItem = null;
            mLastRemovedPosition = -1;

            return insertedPosition;
        } else {
            return -1;
        }
    }
}
