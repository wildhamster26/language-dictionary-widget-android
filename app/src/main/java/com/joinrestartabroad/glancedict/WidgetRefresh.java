package com.joinrestartabroad.glancedict;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

public final class WidgetRefresh {
    private WidgetRefresh() {
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, DictionaryWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(componentName);
        for (int widgetId : widgetIds) {
            DictionaryWidgetProvider.updateAppWidget(context, manager, widgetId);
        }
    }
}
