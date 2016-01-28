package com.mcochin.stockstreaks.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.utils.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Marco on 12/27/2015.
 */
public class StockWidgetRemoteViewsService extends RemoteViewsService{
    public static final int MORE = 12;

    public static final String[] STOCK_PROJECTION = new String[]{
            StockEntry.COLUMN_SYMBOL,
            StockEntry.COLUMN_FULL_NAME,
            StockEntry.COLUMN_RECENT_CLOSE,
            StockEntry.COLUMN_CHANGE_DOLLAR,
            StockEntry.COLUMN_CHANGE_PERCENT,
            StockEntry.COLUMN_STREAK,
            StockEntry.COLUMN_LIST_POSITION
    };

    //index must match projection
    public static final int INDEX_SYMBOL = 0;
    public static final int INDEX_FULL_NAME = 1;
    public static final int INDEX_RECENT_CLOSE = 2;
    public static final int INDEX_CHANGE_DOLLAR = 3;
    public static final int INDEX_CHANGE_PERCENT = 4;
    public static final int INDEX_STREAK = 5;
    public static final int INDEX_LIST_POSITION = 6;

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {

        return new RemoteViewsFactory() {
            private Cursor mData = null;
            private int mShownPositionBookmark;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (mData != null) {
                    mData.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                //We add one because we want to know if we should show the load more button or not
                mShownPositionBookmark = Utility.getShownPositionBookmark(
                        getContentResolver());

                mData = getContentResolver().query(StockEntry.CONTENT_URI,
                        STOCK_PROJECTION,
                        StockProvider.LIST_POSITION_SELECTION_LE,
                        new String[]{Integer.toString(mShownPositionBookmark)},
                        StockProvider.ORDER_BY_LIST_POSITION_ASC_ID_DESC);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        mData == null || !mData.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views;

                if(mData.getInt(INDEX_LIST_POSITION) == mShownPositionBookmark){
                    // Set the load more button item
                    views = new RemoteViews(getPackageName(), R.layout.widget_list_item_load_more);
                    // Fill in intent
                    final Intent fillInIntent = new Intent();
                    fillInIntent.setData(null);
                    views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                }else {
                    if (position == 0) { // widget_list_item_first
                        final long identityToken = Binder.clearCallingIdentity();
                        Date updateTime = Utility.getLastUpdateTime(getContentResolver()).getTime();
                        Binder.restoreCallingIdentity(identityToken);

                        SimpleDateFormat sdf = new SimpleDateFormat(
                                getString(R.string.update_time_format_wide), Locale.US);

                        views = new RemoteViews(getPackageName(), R.layout.widget_list_item_first);
                        views.setTextViewText(R.id.text_update_time,
                                getString(R.string.placeholder_update_time, sdf.format(updateTime)));

                    } else { // widget_list_item
                        views = new RemoteViews(getPackageName(),  R.layout.widget_list_item);
                    }

                    String symbol = mData.getString(INDEX_SYMBOL);
                    String fullName = mData.getString(INDEX_FULL_NAME);
                    String recentClose = getString(R.string.placeholder_dollar,
                            Utility.roundTo2StringDecimals(mData.getFloat(INDEX_RECENT_CLOSE)));
                    float changeDollar = mData.getFloat(INDEX_CHANGE_DOLLAR);
                    float changePercent = mData.getFloat(INDEX_CHANGE_PERCENT);
                    int streak = mData.getInt(INDEX_STREAK);

                    views.setTextViewText(R.id.text_symbol, symbol);
                    views.setTextViewText(R.id.text_full_name, fullName);
                    views.setTextViewText(R.id.text_recent_close, recentClose);
                    views.setTextViewText(R.id.text_streak, getString(
                            R.string.placeholder_d, streak));

                    // Determine the color and the arrow image of the changes
                    Pair<Integer, Integer> changeColorAndDrawableIds =
                            Utility.getChangeColorAndArrowDrawableIds(changeDollar);
                    int color = ContextCompat.getColor(StockWidgetRemoteViewsService.this,
                            changeColorAndDrawableIds.first);

                    // Format dollar/percent change float values to 2 decimals
                    String changeDollarFormat = getString(R.string.placeholder_dollar,
                            Utility.roundTo2StringDecimals(changeDollar));
                    String changePercentFormat = getString(R.string.placeholder_percent,
                            Utility.roundTo2StringDecimals(changePercent));

                    views.setTextViewText(R.id.text_change_amt, getString(
                            R.string.placeholder_change_amt,
                            changeDollarFormat,
                            changePercentFormat));
                    views.setTextColor(R.id.text_change_amt, color);

                    // Set arrow img
                    views.setImageViewResource(R.id.image_streak_arrow,
                            changeColorAndDrawableIds.second);

                    // Fill in intent
                    final Intent fillInIntent = new Intent();
                    fillInIntent.setData(StockEntry.buildUri(symbol));
                    views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                }

                return views;
            }

            @Override
            public void onDestroy() {
                if (mData != null) {
                    mData.close();
                    mData = null;
                }
            }

            @Override
            public int getCount() {
                return mData == null ? 0 : mData.getCount();
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_loading);
            }

            @Override
            public int getViewTypeCount() {
                return 3;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

        };
    }
}