package com.example.glancedict;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class DictionaryRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private final DictionaryDbHelper db;
    private List<WidgetRow> rows = new ArrayList<>();
    private int fontSizeSp = DictionaryPrefs.DEFAULT_FONT_SP;
    private int columnCount = DictionaryPrefs.DEFAULT_COLUMN_COUNT;

    private static final int[] CELL_IDS = new int[]{
            R.id.word_cell_1,
            R.id.word_cell_2,
            R.id.word_cell_3,
            R.id.word_cell_4
    };
    private static final int[] NATIVE_IDS = new int[]{
            R.id.word_native_1,
            R.id.word_native_2,
            R.id.word_native_3,
            R.id.word_native_4
    };
    private static final int[] TRANSLATION_IDS = new int[]{
            R.id.word_translation_1,
            R.id.word_translation_2,
            R.id.word_translation_3,
            R.id.word_translation_4
    };

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
        rows.clear();
        db.close();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= rows.size()) {
            return null;
        }

        WidgetRow row = rows.get(position);
        if (row.categoryHeader) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_category_item);
            views.setTextViewText(R.id.category_header, row.categoryName);
            views.setTextViewTextSize(R.id.category_header, TypedValue.COMPLEX_UNIT_SP, Math.max(10, fontSizeSp - 2));
            return views;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_word_row);
        for (int index = 0; index < CELL_IDS.length; index++) {
            if (index >= columnCount) {
                views.setViewVisibility(CELL_IDS[index], View.GONE);
                continue;
            }

            WidgetItem word = row.words.get(index);
            if (word == null) {
                views.setViewVisibility(CELL_IDS[index], View.INVISIBLE);
                views.setTextViewText(NATIVE_IDS[index], "");
                views.setTextViewText(TRANSLATION_IDS[index], "");
                continue;
            }

            views.setViewVisibility(CELL_IDS[index], View.VISIBLE);
            views.setTextViewText(NATIVE_IDS[index], word.primary);
            views.setTextViewText(TRANSLATION_IDS[index], word.secondary);
            views.setTextViewTextSize(NATIVE_IDS[index], TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
            views.setTextViewTextSize(TRANSLATION_IDS[index], TypedValue.COMPLEX_UNIT_SP, Math.max(10, fontSizeSp - 1));

            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(DictionaryWidgetProvider.EXTRA_WORD_ID, word.wordId);
            views.setOnClickFillInIntent(CELL_IDS[index], fillInIntent);
        }
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
        if (position < 0 || position >= rows.size()) {
            return position;
        }
        return rows.get(position).stableId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void loadItems() {
        fontSizeSp = DictionaryPrefs.getFontSizeSp(context);
        columnCount = DictionaryPrefs.getColumnCount(context);
        rows = buildRows(db.getWidgetItems(DictionaryPrefs.getActiveCategoryIds(context)));
    }

    private List<WidgetRow> buildRows(List<WidgetItem> items) {
        List<WidgetRow> result = new ArrayList<>();
        List<WidgetItem> pendingWords = new ArrayList<>();
        for (WidgetItem item : items) {
            if (item.categoryHeader) {
                flushWordRows(result, pendingWords);
                result.add(WidgetRow.category(item.primary, result.size()));
            } else {
                pendingWords.add(item);
            }
        }
        flushWordRows(result, pendingWords);
        return result;
    }

    private void flushWordRows(List<WidgetRow> result, List<WidgetItem> pendingWords) {
        if (pendingWords.isEmpty()) {
            return;
        }

        for (int start = 0; start < pendingWords.size(); start += columnCount) {
            List<WidgetItem> rowWords = new ArrayList<>();
            for (int index = 0; index < DictionaryPrefs.MAX_COLUMN_COUNT; index++) {
                int wordIndex = start + index;
                rowWords.add(index < columnCount && wordIndex < pendingWords.size()
                        ? pendingWords.get(wordIndex)
                        : null);
            }
            result.add(WidgetRow.words(rowWords));
        }
        pendingWords.clear();
    }

    private static class WidgetRow {
        final boolean categoryHeader;
        final String categoryName;
        final List<WidgetItem> words;
        final long stableId;

        private WidgetRow(boolean categoryHeader, String categoryName, List<WidgetItem> words, long stableId) {
            this.categoryHeader = categoryHeader;
            this.categoryName = categoryName;
            this.words = words;
            this.stableId = stableId;
        }

        static WidgetRow category(String categoryName, int position) {
            long stableId = -1L - Math.abs((categoryName + position).hashCode());
            return new WidgetRow(true, categoryName, new ArrayList<WidgetItem>(), stableId);
        }

        static WidgetRow words(List<WidgetItem> words) {
            long stableId = 0L;
            for (WidgetItem word : words) {
                if (word != null) {
                    stableId = word.wordId;
                    break;
                }
            }
            return new WidgetRow(false, "", words, stableId);
        }
    }
}
