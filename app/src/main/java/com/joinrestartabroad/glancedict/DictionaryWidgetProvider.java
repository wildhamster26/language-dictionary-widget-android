package com.joinrestartabroad.glancedict;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

public class DictionaryWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_WORD_ID = "com.joinrestartabroad.glancedict.EXTRA_WORD_ID";
    public static final String EXTRA_CATEGORY_ID = "com.joinrestartabroad.glancedict.EXTRA_CATEGORY_ID";
    public static final String ACTION_WIDGET_PINNED = "com.joinrestartabroad.glancedict.ACTION_WIDGET_PINNED";
    public static final String ACTION_CLOSE_SETTINGS = "com.joinrestartabroad.glancedict.ACTION_CLOSE_SETTINGS";

    private static final int REQUEST_BASE_ITEM   = 20000;
    private static final int REQUEST_BASE_FOOTER = 30000;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_WIDGET_PINNED.equals(intent.getAction())) {
            // Close the settings activity
            Intent closeIntent = new Intent(ACTION_CLOSE_SETTINGS);
            closeIntent.setPackage(context.getPackageName());
            context.sendBroadcast(closeIntent);

            // Navigate to home screen
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    public static void updateAppWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        int columnCount = DictionaryPrefs.getColumnCount(context);

        int layoutId = R.layout.widget_dictionary;
        int collectionId = R.id.widget_list_1;
        if (columnCount == 2) {
            layoutId = R.layout.widget_dictionary_2;
            collectionId = R.id.widget_list_2;
        } else if (columnCount == 3) {
            layoutId = R.layout.widget_dictionary_3;
            collectionId = R.id.widget_list_3;
        } else if (columnCount >= 4) {
            layoutId = R.layout.widget_dictionary_4;
            collectionId = R.id.widget_list_4;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        Intent serviceIntent = new Intent(context, DictionaryWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.putExtra("column_count", columnCount);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(collectionId, serviceIntent);
        views.setEmptyView(collectionId, R.id.widget_empty);

        views.setOnClickPendingIntent(
                R.id.widget_add,
                activityPendingIntent(context, AddWordActivity.class, appWidgetId));
        views.setOnClickPendingIntent(
                R.id.widget_settings,
                activityPendingIntent(context, SettingsActivity.class, appWidgetId));

        Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://joinrestartabroad.com"));
        views.setOnClickPendingIntent(
                R.id.widget_footer_link,
                PendingIntent.getActivity(context, appWidgetId + REQUEST_BASE_FOOTER, urlIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent itemIntent = new Intent(context, QuickActionActivity.class);
        PendingIntent itemPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + REQUEST_BASE_ITEM,
                itemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(collectionId, itemPendingIntent);

        manager.updateAppWidget(appWidgetId, views);
        manager.notifyAppWidgetViewDataChanged(appWidgetId, collectionId);
    }

    private static PendingIntent activityPendingIntent(
            Context context,
            Class<?> activityClass,
            int appWidgetId) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getActivity(
                context,
                appWidgetId + activityClass.getName().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
