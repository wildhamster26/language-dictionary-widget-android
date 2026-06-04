package com.example.glancedict;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

public class DictionaryWidgetProvider extends AppWidgetProvider {
    public static final String EXTRA_WORD_ID = "com.example.glancedict.EXTRA_WORD_ID";
    public static final String EXTRA_CATEGORY_ID = "com.example.glancedict.EXTRA_CATEGORY_ID";
    public static final String ACTION_WIDGET_CLICK = "com.example.glancedict.ACTION_WIDGET_CLICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!ACTION_WIDGET_CLICK.equals(intent.getAction())) {
            return;
        }
        long wordId = intent.getLongExtra(EXTRA_WORD_ID, -1L);
        long categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L);
        if (wordId != -1L) {
            Intent editIntent = new Intent(context, QuickActionActivity.class);
            editIntent.putExtra(EXTRA_WORD_ID, wordId);
            editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(editIntent);
        } else if (categoryId != -1L) {
            DictionaryPrefs.toggleCollapsedCategory(context, categoryId);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, DictionaryWidgetProvider.class));
            for (int id : ids) {
                updateAppWidget(context, manager, id);
            }
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

        Intent clickIntent = new Intent(ACTION_WIDGET_CLICK, null, context, DictionaryWidgetProvider.class);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 20000,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(collectionId, clickPendingIntent);

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
