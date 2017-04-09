package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.ui.ChartActivity;
import com.udacity.stockhawk.ui.MainActivity;

import timber.log.Timber;

/**
 * Created by kondeelai on 2017-03-30.
 */
public class StockHawkWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; ++i){
            Intent intent = new Intent(context, StockHawkWidgetService.class);

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME )));

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.stock_widget);

            rv.setRemoteAdapter(appWidgetIds[i], R.id.stock_list_view, intent);

            rv.setEmptyView(R.id.stock_list_view, R.id.empty_view);

            Intent mIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mIntent, 0);
            rv.setOnClickPendingIntent(R.id.widget_header, pendingIntent);


            // Set the fill-in intents

            Intent listStockIntentTemplate = new Intent(context, ChartActivity.class);
            PendingIntent listStockIntentPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(listStockIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.stock_list_view, listStockIntentPendingIntentTemplate);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);

    }
    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] appWidgetIds = mgr.getAppWidgetIds( new
                ComponentName(context, StockHawkWidgetProvider.class));

        if (QuoteSyncJob.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            for (int appWidgetId : appWidgetIds) {
                Timber.d("It gets the Broadcast");
                mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.stock_list_view);
            }
        }
    }


}
