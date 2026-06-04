package com.example.glancedict;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class DictionaryRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private final DictionaryDbHelper db;
    private List<WidgetItem> items = new ArrayList<>();
    private int fontSizeSp = DictionaryPrefs.DEFAULT_FONT_SP;

    public DictionaryRemoteViewsFactory(Context context) {
        this.context = context;
        this.db = new DictionaryDbHelper(context);
    }

    @Override
    public void onCreate() {
        loadItems();
    }

    @Override
    public void onDataSetChanged() {
        loadItems();
    }

    @Override
    public void onDestroy() {
        items.clear();
        db.close();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }

        WidgetItem item = items.get(position);
        if (item.categoryHeader) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_category_item);
            views.setTextViewText(R.id.category_header, item.primary);
            views.setTextViewTextSize(R.id.category_header, TypedValue.COMPLEX_UNIT_SP, Math.max(10, fontSizeSp - 2));
            return views;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_word_item);
        views.setTextViewText(R.id.word_native, item.primary);
        views.setTextViewText(R.id.word_translation, item.secondary);
        views.setTextViewTextSize(R.id.word_native, TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        views.setTextViewTextSize(R.id.word_translation, TypedValue.COMPLEX_UNIT_SP, Math.max(10, fontSizeSp - 1));

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(DictionaryWidgetProvider.EXTRA_WORD_ID, item.wordId);
        views.setOnClickFillInIntent(R.id.word_item_root, fillInIntent);
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) {
            return position;
        }
        WidgetItem item = items.get(position);
        return item.categoryHeader ? -position - 1L : item.wordId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void loadItems() {
        fontSizeSp = DictionaryPrefs.getFontSizeSp(context);
        items = db.getWidgetItems(DictionaryPrefs.getActiveCategoryIds(context));
    }
}
