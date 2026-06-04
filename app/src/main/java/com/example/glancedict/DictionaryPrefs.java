package com.example.glancedict;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class DictionaryPrefs {
    public static final int DEFAULT_FONT_SP = 14;
    public static final int MIN_FONT_SP = 10;
    public static final int MAX_FONT_SP = 36;
    public static final int DEFAULT_COLUMN_COUNT = 1;
    public static final int MIN_COLUMN_COUNT = 1;
    public static final int MAX_COLUMN_COUNT = 4;

    private static final String PREFS = "dictionary_widget_prefs";
    private static final String KEY_FONT_SP = "font_size_sp";
    private static final String KEY_COLUMN_COUNT = "column_count";
    private static final String KEY_ACTIVE_CATEGORY_IDS = "active_categories";
    private static final String KEY_LONGEST_TEXT_CACHE = "longest_text_cache";
    private static final String KEY_COLLAPSED_CATEGORY_IDS = "collapsed_categories";

    private DictionaryPrefs() {
    }

    public static int getFontSizeSp(Context context) {
        return prefs(context).getInt(KEY_FONT_SP, DEFAULT_FONT_SP);
    }

    public static void setFontSizeSp(Context context, int fontSizeSp) {
        int clamped = Math.max(MIN_FONT_SP, Math.min(MAX_FONT_SP, fontSizeSp));
        prefs(context).edit().putInt(KEY_FONT_SP, clamped).apply();
    }

    public static int getColumnCount(Context context) {
        int stored = prefs(context).getInt(KEY_COLUMN_COUNT, DEFAULT_COLUMN_COUNT);
        return Math.max(MIN_COLUMN_COUNT, Math.min(MAX_COLUMN_COUNT, stored));
    }

    public static void setColumnCount(Context context, int columnCount) {
        int clamped = Math.max(MIN_COLUMN_COUNT, Math.min(MAX_COLUMN_COUNT, columnCount));
        prefs(context).edit().putInt(KEY_COLUMN_COUNT, clamped).apply();
    }

    public static Set<Long> getActiveCategoryIds(Context context) {
        SharedPreferences preferences = prefs(context);
        if (!preferences.contains(KEY_ACTIVE_CATEGORY_IDS)) {
            return null;
        }

        Set<String> stored = preferences.getStringSet(KEY_ACTIVE_CATEGORY_IDS, new HashSet<String>());
        Set<Long> ids = new HashSet<>();
        for (String value : stored) {
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                // Ignore stale preference entries.
            }
        }
        return ids;
    }

    public static void setActiveCategoryIds(Context context, Set<Long> categoryIds) {
        Set<String> stored = new HashSet<>();
        for (Long id : categoryIds) {
            stored.add(String.valueOf(id));
        }
        prefs(context).edit().putStringSet(KEY_ACTIVE_CATEGORY_IDS, stored).apply();
    }

    public static Set<Long> getCollapsedCategoryIds(Context context) {
        Set<String> stored = prefs(context).getStringSet(KEY_COLLAPSED_CATEGORY_IDS, new HashSet<String>());
        Set<Long> ids = new HashSet<>();
        for (String value : stored) {
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    public static void toggleCollapsedCategory(Context context, long categoryId) {
        Set<Long> collapsed = getCollapsedCategoryIds(context);
        if (!collapsed.remove(categoryId)) {
            collapsed.add(categoryId);
        }
        Set<String> stored = new HashSet<>();
        for (Long id : collapsed) {
            stored.add(String.valueOf(id));
        }
        prefs(context).edit().putStringSet(KEY_COLLAPSED_CATEGORY_IDS, stored).apply();
    }

    public static String getLongestTextCache(Context context) {
        return prefs(context).getString(KEY_LONGEST_TEXT_CACHE, DictionaryDbHelper.DEFAULT_CATEGORY);
    }

    public static void setLongestTextCache(Context context, String value) {
        String cached = value == null || value.trim().isEmpty()
                ? DictionaryDbHelper.DEFAULT_CATEGORY
                : value.trim();
        prefs(context).edit().putString(KEY_LONGEST_TEXT_CACHE, cached).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
