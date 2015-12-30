package com.mcochin.stockstreaks.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.mcochin.stockstreaks.DetailActivity;
import com.mcochin.stockstreaks.MainActivity;
import com.mcochin.stockstreaks.R;
import com.mcochin.stockstreaks.data.StockContract;

/**
 * Created by Marco on 12/27/2015.
 */
public class StockWidgetProvider extends AppWidgetProvider{
    public static final String ACTION_DATA_REFRESH =
            StockContract.CONTENT_AUTHORITY + ".widget." + "ACTION_DATA_REFRESH";

    public static final String ACTION_DATA_MIRROR_APP =
            StockContract.CONTENT_AUTHORITY + ".widget." + "ACTION_DATA_MIRROR_APP";

    @Override
    public void onEnabled(Context context) {
        //TODO start alarmManager here
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.logo, pendingIntent);

            // Set up the collection
            views.setRemoteAdapter(R.id.widget_list,
                    new Intent(context, StockWidgetRemoteViewsService.class));

            boolean useDetailActivity = context.getResources()
                    .getBoolean(R.bool.is_phone);
            Intent clickIntentTemplate = useDetailActivity
                    ? new Intent(context, DetailActivity.class)
                    : new Intent(context, MainActivity.class);

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntentTemplate);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        //TODO just call onUpdate dont call notifyAppWidgetViewDataChanged because we need to pass
        //TODO intent to Service.
//        if (ACTION_DATA_UPDATED.equals(intent.getAction())) {
//            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
//                    new ComponentName(context, getClass()));
//
//            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
//        }
    }

    @Override
    public void onDisabled(Context context) {
        //TODO disable alarm manager

        super.onDisabled(context);
    }
}
