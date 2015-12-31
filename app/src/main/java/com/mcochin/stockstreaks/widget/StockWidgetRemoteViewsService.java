package com.mcochin.stockstreaks.widget;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.ListManipulator;
import com.mcochin.stockstreaks.data.StockContract;
import com.mcochin.stockstreaks.data.StockContract.StockEntry;
import com.mcochin.stockstreaks.data.StockContract.SaveStateEntry;
import com.mcochin.stockstreaks.data.StockProvider;
import com.mcochin.stockstreaks.utils.Utility;

/**
 * Created by Marco on 12/27/2015.
 */
public class StockWidgetRemoteViewsService extends RemoteViewsService{
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                int shownPositionBookmark = Utility.getShownPositionBookmark(getContentResolver());
                data = getContentResolver().query(StockEntry.CONTENT_URI,
                        ListManipulator.STOCK_PROJECTION,
                        StockProvider.SHOWN_POSITION_BOOKMARK_SELECTION,
                        new String[]{Integer.toString(shownPositionBookmark)},
                        StockProvider.ORDER_BY_LIST_POSITION_ASC);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_list_item);

                String symbol = data.getString(ListManipulator.INDEX_SYMBOL);
                String fullName = data.getString(ListManipulator.INDEX_FULL_NAME);
                String recentClose = getString(R.string.placeholder_dollar,
                        Utility.roundTo2StringDecimals(data.getFloat(ListManipulator.INDEX_RECENT_CLOSE)));
                float changeDollar = data.getFloat(ListManipulator.INDEX_CHANGE_DOLLAR);
                float changePercent = data.getFloat(ListManipulator.INDEX_CHANGE_PERCENT);
                int streak = data.getInt(ListManipulator.INDEX_STREAK);

                views.setTextViewText(R.id.text_symbol, symbol);
                views.setTextViewText(R.id.text_full_name, fullName);
                views.setTextViewText(R.id.text_recent_close, recentClose);
                views.setTextViewText(R.id.text_streak, getString(R.string.placeholder_d, streak));

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

                views.setTextViewText(R.id.text_change_amt, getString(R.string.placeholder_change_amt,
                        changeDollarFormat, changePercentFormat));
                views.setTextColor(R.id.text_change_amt, color);
                views.setImageViewResource(R.id.image_streak_arrow, changeColorAndDrawableIds.second);

                return views;
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
