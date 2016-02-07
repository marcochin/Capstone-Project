package com.mcochin.stockstreaks.data;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.widget.Toast;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.pojos.Stock;
import com.mcochin.stockstreaks.utils.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    // Need to bitwise OR these guys together to make fusion ha
    public static final int SORT_ALPHABETICAL = 1;
    public static final int SORT_STREAK = 2;
    public static final int SORT_CHANGE_DOLLAR = 4;
    public static final int SORT_CHANGE_PERCENT = 8;
    public static final int SORT_RECENT_CLOSE = 16;

    public static final int SORT_ASC = 1;
    public static final int SORT_DESC = 1024;

    // These are our fusion ha's
    public static final int SORT_ASC_ALPHABETICAL = 1;
    public static final int SORT_ASC_STREAK = 3;
    public static final int SORT_ASC_CHANGE_DOLLAR = 5;
    public static final int SORT_ASC_CHANGE_PERCENT = 9;
    public static final int SORT_ASC_RECENT_CLOSE = 17;

    public static final int SORT_DESC_ALPHABETICAL = 1025;
    public static final int SORT_DESC_STREAK = 1026;
    public static final int SORT_DESC_CHANGE_DOLLAR = 1028;
    public static final int SORT_DESC_CHANGE_PERCENT = 1032;
    public static final int SORT_DESC_RECENT_CLOSE = 1040;

    private List<Stock> mShownList = new ArrayList<>();
    private String[] mLoadList;
    private int mLoadListPositionBookmark = 0;

    // Used to track our removed items
    private Stock mLastRemovedItem = null;
    private int mLastRemovedPosition = -1;

    // Keep track of how many items are in the list to limit quantity
    private int mTotalStockItems;

    private int mUniqueId = 0;
    private boolean mListUpdated;


    /**
     * Sets the cursor of the shown list. It will extract data from the cursor to populate the list.
     *
     * @param cursor
     */
    public void setShownListCursor(Cursor cursor) {
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

    /**
     * Sets the load list. This list is used to keep track of what items need to be loaded and
     * which ones are already loaded.
     *
     * @param loadList
     */
    public void setLoadList(String[] loadList) {
        mLoadList = loadList;
        mLoadListPositionBookmark = 0;
        mTotalStockItems = loadList.length;
    }

    /**
     * Add a new query item to the top of the list
     *
     * @param stock
     */
    public void addItemToTop(Stock stock) {
        synchronized (this) {
            stock.setId(mUniqueId++);
            mShownList.add(0, stock);

            mListUpdated = true;
            mTotalStockItems++;
        }
    }

    /**
     * Add an updated db item to the bottom of the list
     *
     * @param stock
     */
    public void addItemToBottom(Stock stock) {
        synchronized (this) {
            stock.setId(mUniqueId++);
            mShownList.add(stock);
            addToLoadListPositionBookmark(1);
            mListUpdated = true;
        }
    }

    /**
     * Add an updated db item to the bottom of the list
     *
     * @param stock
     */
    public void addItemToPosition(int position, Stock stock) {
        synchronized (this) {
            stock.setId(mUniqueId++);
            mShownList.add(position, stock);
            addToLoadListPositionBookmark(1);
            mListUpdated = true;
        }
    }

    /**
     * Adds a "dummy" loading item to the bottom of the list with a specific signature to let
     * the adapter know that it is a loading item. This item is used for dynamic loads.
     */
    public void addLoadingItem() {
        Stock stock = new Stock();
        stock.setId(mUniqueId++);
        stock.setSymbol(LOADING_ITEM);
        mShownList.add(stock);
    }

    /**
     * Removes the "dummy" loading item from the bottom of the list.
     */
    public void removeLoadingItem() {
        if (isLoadingItemPresent()) {
            mShownList.remove(getCount() - 1);
        }
    }

    /**
     * Returns the list item at specified index
     *
     * @param position
     * @return
     */
    public Stock getItem(int position) {
        if (position < 0 || position >= getCount()) {
            throw new IndexOutOfBoundsException("index = " + position);
        }
        return mShownList.get(position);
    }

    /**
     * Moves an item from one position of the list to another.
     *
     * @param fromPosition
     * @param toPosition
     */
    public void moveItem(int fromPosition, int toPosition) {
        synchronized (this) {
            Stock stock = mShownList.remove(fromPosition);
            mShownList.add(toPosition, stock);
            mListUpdated = true;
        }
    }

    /**
     * Removes an item from specified position.
     *
     * @param position
     */
    public void removeItem(int position) {
        mLastRemovedItem = mShownList.remove(position);
        mLastRemovedPosition = position;
        mListUpdated = true;
        mTotalStockItems--;
    }

    /**
     * Undoes the removal of the last removed item.
     *
     * @return the previous position of the removed item
     */
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
            mTotalStockItems++;

            return insertedPosition;
        } else {
            return -1;
        }
    }

    /**
     * Permanently deletes an items from the db once the chance to undo a removal has passed.
     *
     * @param context
     */
    public void permanentlyDeleteLastRemoveItem(Context context) {
        synchronized (this) {
            if (mLastRemovedItem != null) {
                context.getContentResolver().delete(
                        StockEntry.buildUri(mLastRemovedItem.getSymbol()),
                        null,
                        null
                );
                mLastRemovedItem = null;
                mListUpdated = true;
            }
        }
    }

    /**
     * @return an array of items that will be dynamically loaded according to the bookmark of the
     * the load list.
     */
    public String[] getMoreToLoad() {
        String[] nextFewToLoad = null;

        if (canLoadMore()) {
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

    /**
     * @return true if the loading item is present, false otherwise.
     */
    public boolean isLoadingItemPresent() {
        if (getCount() > 0) {
            return mShownList.get(getCount() - 1).getSymbol().equals(LOADING_ITEM);
        }
        return false;
    }

    /**
     * @return true if the list was updated/modified after an orientation change or app startup,
     * false otherwise.
     */
    public boolean isListUpdated() {
        return mListUpdated;
    }

    /**
     * We have to limit the amount of items a user can have to prevent the API quota from being
     * reached. Checks to see if the user can add more items or not.
     *
     * @return true if you have reached the limit, false otherwise.
     */
    public boolean isListLimitReached(Context context) {
        if (mTotalStockItems >= ListManipulator.LIST_LIMIT) {
            Toast.makeText(context, context.getString(R.string.toast_placeholder_error_stock_limit,
                    LIST_LIMIT), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /**
     * @return true if there are more items to load from the load list, false otherwise.
     */
    public boolean canLoadMore() {
        return mLoadList != null && mLoadListPositionBookmark < mLoadList.length;
    }

    /**
     * @return The current list size.
     */
    public int getCount() {
        return mShownList.size();
    }

    /**
     * THe load list is a list of all symbols in the db in their correct list positions. This
     * increments an index integer bookmark to keep track of what symbols need to be dynamically
     * loaded.
     *
     * @param addToBookmark The amount to increment the bookmark by.
     */
    public void addToLoadListPositionBookmark(int addToBookmark) {
        if (addToBookmark > 0) {
            mLoadListPositionBookmark += addToBookmark;
        } else {
            throw new IllegalArgumentException("Must be a positive number.");
        }
    }

    /**
     * Sorts the list depending on you sort preference
     * @param sortPreference
     * <ul>
     *     <li>SORT_ASC_ALPHABETICAL</li>
     *     <li>SORT_ASC_STREAK</li>
     *     <li>SORT_ASC_CHANGE_DOLLAR</li>
     *     <li>SORT_ASC_CHANGE_PERCENT</li>
     *     <li>SORT_ASC_RECENT_CLOSE</li>
     *     <li>SORT_DESC_ALPHABETICAL</li>
     *     <li>SORT_DESC_STREAK</li>
     *     <li>SORT_DESC_CHANGE_DOLLAR</li>
     *     <li>SORT_DESC_CHANGE_PERCENT</li>
     *     <li>SORT_DESC_RECENT_CLOSE</li>
     * </ul>
     */
    public void sort(int sortPreference) {
        Comparator<Stock> comparator;

        switch (sortPreference) {
            case SORT_ASC_ALPHABETICAL:
                comparator = new ComparatorAscAlphabetical();
                break;
            case SORT_DESC_ALPHABETICAL:
                comparator = new ComparatorDescAlphabetical();
                break;
            case SORT_ASC_STREAK:
                comparator = new ComparatorAscStreak();
                break;
            case SORT_DESC_STREAK:
                comparator = new ComparatorDescStreak();
                break;
            case SORT_ASC_CHANGE_DOLLAR:
                comparator = new ComparatorAscChangeDollar();
                break;
            case SORT_DESC_CHANGE_DOLLAR:
                comparator = new ComparatorDescChangeDollar();
                break;
            case SORT_ASC_CHANGE_PERCENT:
                comparator = new ComparatorAscChangePercent();
                break;
            case SORT_DESC_CHANGE_PERCENT:
                comparator = new ComparatorDescChangePercent();
                break;
            case SORT_ASC_RECENT_CLOSE:
                comparator = new ComparatorAscRecentClose();
                break;
            case SORT_DESC_RECENT_CLOSE:
                comparator = new ComparatorDescRecentClose();
                break;
            default:
                throw new IllegalArgumentException("Invalid sort preference");
        }

        Collections.sort(mShownList, comparator);
        mListUpdated = true;
    }

    /**
     * Saves the list positions of every item. Should be called from a background thread
     *
     * @param context Context
     */
    // All synchronized blocks in this class are so they don't interfere with this method.
    public void saveShownListState(Context context) {
        synchronized (this) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            // Determine if the last item is a loading item. If so, skip it. We can't remove it
            // because during orientation change, we need to persist the loading item.
            int mShownListSize;

            if (mShownList.isEmpty()) {
                mShownListSize = 0;
            } else {
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

            context.getContentResolver().call(StockContract.BASE_CONTENT_URI,
                    StockProvider.METHOD_UPDATE_LIST_POSITION,
                    null,
                    extras);

            mListUpdated = false;
        }
    }

    private static class ComparatorAscAlphabetical implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return lhs.getSymbol().compareTo(rhs.getSymbol());
        }
    }

    private static class ComparatorDescAlphabetical implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return -lhs.getSymbol().compareTo(rhs.getSymbol());
        }
    }

    private static class ComparatorAscStreak implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return Utility.compare(lhs.getStreak(), rhs.getStreak());
        }
    }

    private static class ComparatorDescStreak implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return -Utility.compare(lhs.getStreak(), rhs.getStreak());
        }
    }

    private static class ComparatorAscChangeDollar implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return Float.compare(lhs.getChangeDollar(), rhs.getChangeDollar());
        }
    }

    private static class ComparatorDescChangeDollar implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return -Float.compare(lhs.getChangeDollar(), rhs.getChangeDollar());
        }
    }

    private static class ComparatorAscChangePercent implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return Float.compare(lhs.getChangePercent(), rhs.getChangePercent());
        }
    }

    private static class ComparatorDescChangePercent implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return -Float.compare(lhs.getChangePercent(), rhs.getChangePercent());
        }
    }

    private static class ComparatorAscRecentClose implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return Float.compare(lhs.getRecentClose(), rhs.getRecentClose());
        }
    }

    private static class ComparatorDescRecentClose implements Comparator<Stock>{
        @Override
        public int compare(Stock lhs, Stock rhs) {
            return -Float.compare(lhs.getRecentClose(), rhs.getRecentClose());
        }
    }
}
