package com.example.glancedict;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {
    private DictionaryDbHelper db;
    private TextView fontValue;
    private TextView columnsValue;
    private LinearLayout categoryChecks;
    private int fontSizeSp;
    private int columnCount;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED);
            setFinishOnTouchOutside(false);
        }

        db = new DictionaryDbHelper(this);
        fontValue = findViewById(R.id.font_value);
        columnsValue = findViewById(R.id.columns_value);
        categoryChecks = findViewById(R.id.category_checks);
        fontSizeSp = DictionaryPrefs.getFontSizeSp(this);
        columnCount = DictionaryPrefs.getColumnCount(this);

        Button decrement = findViewById(R.id.font_decrement);
        Button increment = findViewById(R.id.font_increment);
        Button columnsDecrement = findViewById(R.id.columns_decrement);
        Button columnsIncrement = findViewById(R.id.columns_increment);
        Button done = findViewById(R.id.settings_done);
        Button addWords = findViewById(R.id.settings_add_words);
        Button selectAll = findViewById(R.id.select_all);
        Button deselectAll = findViewById(R.id.deselect_all);

        selectAll.setOnClickListener(v -> setAllCategoriesChecked(true));
        deselectAll.setOnClickListener(v -> setAllCategoriesChecked(false));
        addWords.setOnClickListener(v -> startActivity(new Intent(this, AddWordActivity.class)));
        decrement.setOnClickListener(v -> changeFontSize(-1));
        increment.setOnClickListener(v -> changeFontSize(1));
        columnsDecrement.setOnClickListener(v -> changeColumnCount(-1));
        columnsIncrement.setOnClickListener(v -> changeColumnCount(1));
        done.setOnClickListener(v -> {
            saveActiveCategories();
            db.refreshLongestTextCache(this);
            WidgetRefresh.refreshAll(this);
            finishConfiguration();
        });

        updateFontValue();
        updateColumnsValue();
        bindCategoryChecks();
    }

    private void finishConfiguration() {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            DictionaryWidgetProvider.updateAppWidget(this, manager, appWidgetId);
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, result);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    private void changeFontSize(int delta) {
        fontSizeSp = Math.max(
                DictionaryPrefs.MIN_FONT_SP,
                Math.min(DictionaryPrefs.MAX_FONT_SP, fontSizeSp + delta));
        DictionaryPrefs.setFontSizeSp(this, fontSizeSp);
        updateFontValue();
        WidgetRefresh.refreshAll(this);
    }

    private void updateFontValue() {
        fontValue.setText(fontSizeSp + "sp");
    }

    private void changeColumnCount(int delta) {
        columnCount = Math.max(
                DictionaryPrefs.MIN_COLUMN_COUNT,
                Math.min(DictionaryPrefs.MAX_COLUMN_COUNT, columnCount + delta));
        DictionaryPrefs.setColumnCount(this, columnCount);
        updateColumnsValue();
        WidgetRefresh.refreshAll(this);
    }

    private void updateColumnsValue() {
        columnsValue.setText(String.valueOf(columnCount));
    }

    private void bindCategoryChecks() {
        Set<Long> activeIds = DictionaryPrefs.getActiveCategoryIds(this);
        List<Category> categories = db.getCategories();
        categoryChecks.removeAllViews();
        for (Category category : categories) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(category.name);
            checkBox.setTag(category.id);
            checkBox.setChecked(activeIds == null || activeIds.contains(category.id));
            checkBox.setMinHeight(dp(44));
            categoryChecks.addView(checkBox);
        }
    }

    private void saveActiveCategories() {
        Set<Long> activeIds = new HashSet<>();
        for (int i = 0; i < categoryChecks.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) categoryChecks.getChildAt(i);
            if (checkBox.isChecked()) {
                activeIds.add((Long) checkBox.getTag());
            }
        }
        DictionaryPrefs.setActiveCategoryIds(this, activeIds);
    }

    private void setAllCategoriesChecked(boolean checked) {
        for (int i = 0; i < categoryChecks.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) categoryChecks.getChildAt(i);
            checkBox.setChecked(checked);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
