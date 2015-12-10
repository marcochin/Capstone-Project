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

    public static final int A_FEW = 20;

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

    private String[] mLoadList;
    private int mLoadListPositionBookmark;

    private List<Stock> mShownList = new ArrayList<>();
    private Stock mLastRemovedItem = null;
    private int mLastRemovedPosition = -1;
    private int mUniqueId = 0;

    public int generateUniqueId(){
        return mUniqueId++;
    }

    public void setShownListCursor(Cursor cursor){
    //TODO remove this, Using fruits array just to debug
//        for(String fruit : FRUITS){
//            Stock stock = new Stock();
//            stock.setSymbol(fruit);
//            stock.setId(mUniqueId);
//
//            mShownList.add(stock);
//            mUniqueId++;
//        }
        mUniqueId = 0;
        mShownList.clear();

        while(cursor.moveToNext()) {
            Stock stock = Utility.getStockFromCursor(cursor);
            addItem(stock);
        }
    }

    public void setLoadList(String[] loadList) {
        mLoadListPositionBookmark = 0;
        mLoadList = loadList;
    }

    public void addItem(Stock stock){
        stock.setId(generateUniqueId());
        mShownList.add(0, stock);
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

    public int getCount(){
        return mShownList.size();
    }

    public Stock getItem(int index) {
        if (index < 0 || index >= getCount()) {
            throw new IndexOutOfBoundsException("index = " + index);
        }
        return mShownList.get(index);
    }

    public int getLoadListPositionBookmark(){
        return mLoadListPositionBookmark;
    }

    public void addToLoadListPositionBookmark(int addToBookmark){
        if(addToBookmark > 0 ) {
            mLoadListPositionBookmark += addToBookmark;
        } else{
            throw new IllegalArgumentException("Must be a positive number.");
        }
    }

    public String[] getAFewToLoad(){
        if(!canLoadAFew()){
            return null;
        }

        String [] nextFewToLoad;
        boolean loadAFew;

        int whatsLeftToLoad = mLoadList.length - mLoadListPositionBookmark;
        if (whatsLeftToLoad >= A_FEW){
            nextFewToLoad = new String[A_FEW];
            loadAFew = true;
        } else{
            nextFewToLoad = new String[whatsLeftToLoad];
            loadAFew = false;
        }

        // We can't update the REAL bookmark until we get a msg that update has succeeded.
        int bookmarkHelper = mLoadListPositionBookmark;
        for(int i = 0; i < (loadAFew? A_FEW: whatsLeftToLoad); i++){
            nextFewToLoad[i] = mLoadList[bookmarkHelper];
            bookmarkHelper++;
        }
        return nextFewToLoad;
    }

    public boolean canLoadAFew(){
        if(mLoadList != null) {
            return mLoadListPositionBookmark < mLoadList.length;
        }

        return false;
    }
}
