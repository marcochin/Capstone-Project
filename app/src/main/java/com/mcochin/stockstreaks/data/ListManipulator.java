package com.mcochin.stockstreaks.data;

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

    public void setShownList(ArrayList<Stock> shownList){
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
        mShownList = shownList;

    }

    public void setLoadList(String[] loadList){
        mLoadListPositionBookmark = 0;
        mLoadList = loadList;
    }

    public int generateUniqueId(){
        return mUniqueId++;
    }

    public void addItem(Stock stock){
        stock.setId(mUniqueId++);
        mShownList.add(stock);
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

    public String[] getAFewToLoad(){
        if(!canLoadAFew()){
            return null;
        }

        String [] nextFewToLoad;
        boolean loadAFew;

        int whatsLeftToLoad = mLoadList.length - mLoadListPositionBookmark;
        if (whatsLeftToLoad > A_FEW){
            nextFewToLoad = new String[A_FEW];
            loadAFew = true;
        } else{
            nextFewToLoad = new String[whatsLeftToLoad];
            loadAFew = false;
        }

        for(int i = 0; i < (loadAFew? A_FEW: whatsLeftToLoad); i++){
            nextFewToLoad[i] = mLoadList[mLoadListPositionBookmark];
            mLoadListPositionBookmark++;
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
