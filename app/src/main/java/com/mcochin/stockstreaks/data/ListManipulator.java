package com.mcochin.stockstreaks.data;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

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

    public static final int MORE = 8;
    public static final int LIST_LIMIT = 200;
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
        synchronized (this) {
            stock.setId(mUniqueId++);
            mShownList.add(0, stock);
            mListUpdated = true;
            printList();
        }
    }

    /**
     * Add an updated db item to the bottom of the list
     * @param stock
     */
    public void addItemToBottom(Stock stock){
        synchronized (this) {
            stock.setId(mUniqueId++);
            mShownList.add(stock);
            addToLoadListPositionBookmark(1);
            mListUpdated = true;
            printList();
        }
    }

    /**
     * Add an updated db item to the bottom of the list
     * @param stock
     */
    public void addItemToPosition(int position, Stock stock){
        synchronized (this) {
            stock.setId(mUniqueId++);
            mShownList.add(position, stock);
            addToLoadListPositionBookmark(1);
            mListUpdated = true;
            printList();
        }
    }

    public void addLoadingItem(){
        Stock stock = new Stock();
        stock.setId(mUniqueId++);
        stock.setSymbol(LOADING_ITEM);
        mShownList.add(stock);
        printList();
    }

    public void removeLoadingItem(){
        if(isLoadingItemPresent()){
            mShownList.remove(getCount() - 1);
        }
        printList();
    }

    public boolean isLoadingItemPresent(){
        if(getCount() > 0) {
            return mShownList.get(getCount() - 1).getSymbol().equals(LOADING_ITEM);
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

    public boolean isListUpdated(){
        return mListUpdated;
    }

    public void setShownListCursor(Cursor cursor){
        synchronized (this) {
            mUniqueId = 0;
            mShownList.clear();

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Stock stock = Utility.getStockFromCursor(cursor);
                    stock.setId(mUniqueId++);
                    mShownList.add(stock);
                }
            }
            mListUpdated = true;
        }
    }

    public void setLoadList(String[] loadList) {
        mLoadListPositionBookmark = 0;
        mLoadList = loadList;
    }

    public void moveItem(int fromPosition, int toPosition) {
        synchronized (this) {
            Stock stock = mShownList.remove(fromPosition);
            mShownList.add(toPosition, stock);
            mListUpdated = true;
        }
    }

    public void removeItem(int position) {
        mLastRemovedItem = mShownList.remove(position);
        mLastRemovedPosition = position;
        mListUpdated = true;
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
        synchronized (this) {
            if (mLastRemovedItem != null) {
                cr.delete(
                        StockEntry.buildUri(mLastRemovedItem.getSymbol()),
                        null,
                        null
                );

                mLastRemovedItem = null;
                mListUpdated = true;
            }
        }
    }

    public String[] getMoreToLoad(){
        String [] nextFewToLoad = null;

        if(canLoadMore()) {
            boolean loadMore;

            int whatsLeftToLoad = mLoadList.length - mLoadListPositionBookmark;
            if (whatsLeftToLoad >= MORE) {
                nextFewToLoad = new String[MORE];
                loadMore = true;
            } else {
                nextFewToLoad = new String[whatsLeftToLoad];
                loadMore = false;
            }

            // We can't update the REAL bookmark until we get a msg that update has succeeded.
            int bookmarkHelper = mLoadListPositionBookmark;
            for (int i = 0; i < (loadMore ? MORE : whatsLeftToLoad); i++) {
                nextFewToLoad[i] = mLoadList[bookmarkHelper++];
            }
        }

        return nextFewToLoad;
    }

    public boolean canLoadMore(){
        return mLoadList != null && mLoadListPositionBookmark < mLoadList.length;
    }

    /**
     * Saves the list positions of every item. Should be called from a background thread
     * @param cr ContentResolver
     */
    // All synchronized blocks in this class are so they don't interfere with this method.
    public void saveShownListState(ContentResolver cr) {
        synchronized (this) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            // Determine if the last item is a loading item. If so, skip it. We can't remove it
            // because during orientation change, we need to persist the loading item.
            int mShownListSize;

            if(mShownList.isEmpty()){
                mShownListSize = 0;
            }else {
                mShownListSize = isLoadingItemPresent() ? mShownList.size() - 1 : mShownList.size();
            }

            // Save bookmark in db
            ContentValues bookmarkValues = new ContentValues();
            bookmarkValues.put(SaveStateEntry.COLUMN_SHOWN_POSITION_BOOKMARK, mShownListSize);
            ops.add(ContentProviderOperation
                    .newUpdate(SaveStateEntry.CONTENT_URI)
                    .withValues(bookmarkValues)
                    .withYieldAllowed(true)
                    .build());

            // Save shown list positions in db
            int listPosition = 0;
            for (int i = 0; i < mShownListSize; i++) {
                // Save bookmark in db
                ContentValues positionValues = new ContentValues();
                positionValues.put(StockEntry.COLUMN_LIST_POSITION, listPosition++);
                ops.add(ContentProviderOperation
                        .newUpdate(StockEntry.buildUri(mShownList.get(i).getSymbol()))
                        .withValues(positionValues)
                        .withYieldAllowed(true)
                        .build());
            }

            if (mLoadList != null) {
                // Save load list positions beneath the shown list positions in db
                for (int j = mLoadListPositionBookmark; j < mLoadList.length; j++) {
                    // Save bookmark in db
                    ContentValues positionValues = new ContentValues();
                    positionValues.put(StockEntry.COLUMN_LIST_POSITION, listPosition++);
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

    private void printList(){
        String printList = "";
        StringBuilder stringBuilder = new StringBuilder(printList);
        for(int i = 0; i < mShownList.size(); i++){
            stringBuilder.append(mShownList.get(i).getSymbol());
            stringBuilder.append(", ");
        }
        Log.d(TAG, stringBuilder.toString());
    }
}
