package com.mcochin.stockstreaks.data;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;

import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.pojos.Stock;
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

    public static final int A_FEW = 12;
    public static final String LOADING_ITEM = "loadingItem";

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
    private int mLoadListPositionBookmark = 0;

    private Stock mLastRemovedItem = null;
    private int mLastRemovedPosition = -1;

    private int mUniqueId = 0;
    private boolean mListUpdated;

    /**
     * Add a new query item to the top of the list
     * @param stock
     */
    public void addItemToTop(Stock stock){
        stock.setId(generateUniqueId());
        mShownList.add(0, stock);
        mListUpdated = true;
    }

    /**
     * Add an updated db item to the bottom of the list
     * @param stock
     */
    public void addItemToBottom(Stock stock){
        stock.setId(generateUniqueId());
        mShownList.add(stock);
        addToLoadListPositionBookmark(1);
        mListUpdated = true;
    }

    public void addLoadingItem(){
        Stock stock = new Stock();
        stock.setId(generateUniqueId());
        stock.setSymbol(LOADING_ITEM);
        mShownList.add(stock);
    }

    public void removeLoadingItem(){
        if(isLoadingItemPresent()){
            mShownList.remove(getCount() - 1);
        }
    }

    public boolean isLoadingItemPresent(){
        if(getCount() > 0) {
            Stock stock = mShownList.get(getCount() - 1);
            return stock.getSymbol().equals(LOADING_ITEM);
        }
        return false;
    }

    public void addToLoadListPositionBookmark(int addToBookmark){
        if(addToBookmark > 0 ) {
            mLoadListPositionBookmark += addToBookmark;
        } else{
            throw new IllegalArgumentException("Must be a positive number.");
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

    public int generateUniqueId(){
        return mUniqueId++;
    }

    public boolean isListUpdated(){
        return mListUpdated;
    }

    public void setShownListCursor(Cursor cursor){
        mUniqueId = 0;
        mShownList.clear();

        if(cursor != null) {
            while (cursor.moveToNext()) {
                Stock stock = Utility.getStockFromCursor(cursor);
                stock.setId(generateUniqueId());
                mShownList.add(stock);
            }
        }
    }

    public void setLoadList(String[] loadList) {
        mLoadListPositionBookmark = 0;
        mLoadList = loadList;
    }

    public void moveItem(int fromPosition, int toPosition) {
        Stock stock = mShownList.remove(fromPosition);
        mShownList.add(toPosition, stock);
        mListUpdated = true;
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

    public void permanentlyDeleteLastRemoveItem(final ContentResolver cr){
        if(mLastRemovedItem != null) {
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    cr.delete(
                            StockEntry.buildUri(mLastRemovedItem.getSymbol()),
                            null,
                            null
                    );
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    mLastRemovedItem = null;
                    mListUpdated = true;
                }
            }.execute();
        }
    }

    public String[] getAFewToLoad(){
        String [] nextFewToLoad = null;

        if(canLoadAFew()) {
            boolean loadAFew;

            int whatsLeftToLoad = mLoadList.length - mLoadListPositionBookmark;
            if (whatsLeftToLoad >= A_FEW) {
                nextFewToLoad = new String[A_FEW];
                loadAFew = true;
            } else {
                nextFewToLoad = new String[whatsLeftToLoad];
                loadAFew = false;
            }

            // We can't update the REAL bookmark until we get a msg that update has succeeded.
            int bookmarkHelper = mLoadListPositionBookmark;
            for (int i = 0; i < (loadAFew ? A_FEW : whatsLeftToLoad); i++) {
                nextFewToLoad[i] = mLoadList[bookmarkHelper++];
            }
        }

        return nextFewToLoad;
    }

    public boolean canLoadAFew(){
        if(mLoadList != null) {
            return mLoadListPositionBookmark < mLoadList.length;
        }

        return false;
    }

    public void saveShownListState(ContentResolver cr){
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        removeLoadingItem();

        // Save bookmark in db
        ContentValues bookmarkValues = new ContentValues();
        bookmarkValues.put(SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK, getCount());

        ops.add(ContentProviderOperation
                .newUpdate(SaveStateEntry.CONTENT_URI)
                .withValues(bookmarkValues)
                .withYieldAllowed(true)
                .build());

        // Save shown list positions in db
        int i = 0;
        for(Stock stock: mShownList){
            // Save bookmark in db
            ContentValues positionValues = new ContentValues();
            positionValues.put(StockEntry.COLUMN_LIST_POSITION, i++);

            ops.add(ContentProviderOperation
                            .newUpdate(StockEntry.buildUri(stock.getSymbol()))
                            .withValues(positionValues)
                            .withYieldAllowed(true)
                            .build());
        }

        if(mLoadList != null) {
            // Save load list positions beneath the shown list positions in db
            for (int j = mLoadListPositionBookmark; j < mLoadList.length; j++) {
                // Save bookmark in db
                ContentValues positionValues = new ContentValues();
                positionValues.put(StockEntry.COLUMN_LIST_POSITION, i++);

                ops.add(ContentProviderOperation
                        .newUpdate(StockEntry.buildUri(mLoadList[j]))
                        .withValues(positionValues)
                        .withYieldAllowed(true)
                        .build());
            }
        }

        // Apply operations
        Bundle extras = new Bundle();
        extras.putParcelableArrayList(StockProvider.KEY_OPERATIONS, ops);
        cr.call(StockContract.BASE_CONTENT_URI, StockProvider.METHOD_UPDATE_LIST_POSITION, null, extras);

        mListUpdated = false;
    }
}
