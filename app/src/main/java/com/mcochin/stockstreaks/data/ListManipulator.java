package com.mcochin.stockstreaks.data;

import android.database.Cursor;

import com.mcochin.stockstreaks.pojos.Stock;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>ListManipulator</code> holds the data for our list and provides the necessary methods
 * to manipulate that list.
 */
public class ListManipulator {
    static final String[] FRUITS = new String[] { "Apple", "Avocado", "Banana",
            "Blueberry", "Coconut", "Durian", "Guava", "Kiwi", "Jackfruit", "Mango",
            "Olive", "Pear", "Sugar-apple", "Orange", "Strawberry", "Pineapple",
            "Watermelon", "Grape", "PassionFruit", "DragonFruit", "Honey-dew",
            "Cantaloupe", "Papaya"};

    private List<Stock> mData = new ArrayList<>();
    private Stock mLastRemovedItem = null;
    private int mLastRemovedPosition = -1;
    private int uniqueId = 0;

    public void setCursor(Cursor data){
        uniqueId = 0;
        mData.clear();

//        //TODO remove this, Using fruits array just to debug
//        for(String fruit : FRUITS){
//            Stock stock = new Stock();
//            stock.setSymbol(fruit);
//            stock.setId(uniqueId);
//
//            mData.add(stock);
//            uniqueId++;
//        }

        while(data.moveToNext()){
            String symbol = data.getString(StockDbHelper.INDEX_SYMBOL);
            String fullName = data.getString(StockDbHelper.INDEX_FULL_NAME);
            float prevClose = data.getFloat(StockDbHelper.INDEX_PREV_CLOSE);
            float changeDollar = data.getFloat(StockDbHelper.INDEX_CHANGE_DOLLAR);
            float changePercent = data.getFloat(StockDbHelper.INDEX_CHANGE_PERCENT);
            int streak = data.getInt(StockDbHelper.INDEX_STREAK);

            Stock stock = new Stock();
            stock.setId(uniqueId);
            stock.setSymbol(symbol);
            stock.setFullName(fullName);
            stock.setPrevClose(prevClose);
            stock.setChangeDollar(changeDollar);
            stock.setChangePercent(changePercent);
            stock.setStreak(streak);

            mData.add(stock);
            uniqueId++;
        }
    }

    public int getCount(){
        return mData.size();
    }

    public void addItem(Stock stock){
        mData.add(stock);
        //need to set id or the added item will contain text from the previous last item
        stock.setId(uniqueId++);
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

}
