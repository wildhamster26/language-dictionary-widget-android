package com.joinrestartabroad.glancedict;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DictionaryRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private final DictionaryDbHelper db;
    private List<WidgetRow> rows = new ArrayList<>();
    private int fontSizeSp = DictionaryPrefs.DEFAULT_FONT_SP;
    private int categoryFontSizeSp = DictionaryPrefs.DEFAULT_CATEGORY_FONT_SP;
    private int columnCount = DictionaryPrefs.DEFAULT_COLUMN_COUNT;
    private Set<Long> collapsedCategoryIds = new HashSet<>();
    private boolean sortByLength = false;
    private boolean showRomanization = true;

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
    private static final int[] ROMANIZATION_IDS = new int[]{
            R.id.word_romanization_1,
            R.id.word_romanization_2,
            R.id.word_romanization_3,
            R.id.word_romanization_4
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
            boolean isCollapsed = collapsedCategoryIds.contains(row.categoryId);
            String prefix = isCollapsed ? "▶ " : "▼ ";
            views.setTextViewText(R.id.category_header, prefix + row.categoryName);
            views.setTextViewTextSize(R.id.category_header, TypedValue.COMPLEX_UNIT_SP, categoryFontSizeSp);
            Intent fillInIntent = new Intent();
            fillInIntent.setAction(DictionaryWidgetProvider.ACTION_TOGGLE_CATEGORY);
            fillInIntent.putExtra(DictionaryWidgetProvider.EXTRA_CATEGORY_ID, row.categoryId);
            views.setOnClickFillInIntent(R.id.category_header, fillInIntent);
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
                views.setTextViewText(ROMANIZATION_IDS[index], "");
                views.setViewVisibility(ROMANIZATION_IDS[index], View.GONE);
                continue;
            }

            views.setViewVisibility(CELL_IDS[index], View.VISIBLE);
            views.setTextViewText(NATIVE_IDS[index], word.primary);
            views.setTextViewText(TRANSLATION_IDS[index], word.secondary);
            views.setTextViewTextSize(NATIVE_IDS[index], TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
            views.setTextViewTextSize(TRANSLATION_IDS[index], TypedValue.COMPLEX_UNIT_SP, Math.max(10, fontSizeSp - 1));

            boolean hasRomanization = showRomanization && word.tertiary != null && !word.tertiary.isEmpty();
            if (hasRomanization) {
                views.setTextViewText(ROMANIZATION_IDS[index], word.tertiary);
                views.setTextViewTextSize(ROMANIZATION_IDS[index], TypedValue.COMPLEX_UNIT_SP, Math.max(9, fontSizeSp - 2));
                views.setViewVisibility(ROMANIZATION_IDS[index], View.VISIBLE);
            } else {
                views.setTextViewText(ROMANIZATION_IDS[index], "");
                views.setViewVisibility(ROMANIZATION_IDS[index], View.GONE);
            }

            Intent fillInIntent = new Intent();
            fillInIntent.setAction(DictionaryWidgetProvider.ACTION_OPEN_WORD);
            fillInIntent.putExtra(DictionaryWidgetProvider.EXTRA_WORD_ID, word.wordId);
            views.setOnClickFillInIntent(CELL_IDS[index], fillInIntent);

            Intent copyIntent = new Intent();
            copyIntent.setAction(DictionaryWidgetProvider.ACTION_COPY_WORD);
            copyIntent.putExtra(DictionaryWidgetProvider.EXTRA_WORD_ID, word.wordId);
            copyIntent.putExtra(DictionaryWidgetProvider.EXTRA_COPY_TARGET, true);
            views.setOnClickFillInIntent(TRANSLATION_IDS[index], copyIntent);
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
        return false;
    }

    private void loadItems() {
        fontSizeSp = DictionaryPrefs.getFontSizeSp(context);
        categoryFontSizeSp = DictionaryPrefs.getCategoryFontSizeSp(context);
        columnCount = DictionaryPrefs.getColumnCount(context);
        collapsedCategoryIds = DictionaryPrefs.getCollapsedCategoryIds(context);
        sortByLength = DictionaryPrefs.isSortByLength(context);
        showRomanization = DictionaryPrefs.isShowRomanization(context);
        rows = buildRows(db.getWidgetItems(DictionaryPrefs.getActiveCategoryIds(context)));
    }

    private List<WidgetRow> buildRows(List<WidgetItem> items) {
        List<WidgetRow> result = new ArrayList<>();
        List<WidgetItem> pendingWords = new ArrayList<>();
        long currentCategoryId = -1L;
        for (WidgetItem item : items) {
            if (item.categoryHeader) {
                flushWordRows(result, pendingWords);
                result.add(WidgetRow.category(item.categoryId, item.primary));
                currentCategoryId = item.categoryId;
            } else if (!collapsedCategoryIds.contains(currentCategoryId)) {
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

        if (sortByLength) {
            Collections.sort(pendingWords, (a, b) -> {
                int lenA = a.primary.length() + a.secondary.length();
                int lenB = b.primary.length() + b.secondary.length();
                return Integer.compare(lenA, lenB);
            });
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
        final long categoryId;
        final String categoryName;
        final List<WidgetItem> words;
        final long stableId;

        private WidgetRow(boolean categoryHeader, long categoryId, String categoryName, List<WidgetItem> words, long stableId) {
            this.categoryHeader = categoryHeader;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.words = words;
            this.stableId = stableId;
        }

        static WidgetRow category(long categoryId, String categoryName) {
            return new WidgetRow(true, categoryId, categoryName, new ArrayList<>(), -categoryId);
        }

        static WidgetRow words(List<WidgetItem> words) {
            long stableId = 0L;
            for (WidgetItem word : words) {
                if (word != null) {
                    stableId = word.wordId;
                    break;
                }
            }
            return new WidgetRow(false, -1L, "", words, stableId);
        }
    }
}
