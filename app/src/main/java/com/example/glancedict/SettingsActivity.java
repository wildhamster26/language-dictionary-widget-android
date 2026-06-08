package com.example.glancedict;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
        } else {
            checkAndRequestWidgetPinning();
        }

        db = new DictionaryDbHelper(this);

        // Display Section Header
        View displayHeader = findViewById(R.id.display_header);
        ((TextView) displayHeader.findViewById(R.id.section_title)).setText(R.string.settings_display_section);

        // Font Size Control
        View fontSizeRow = findViewById(R.id.font_size_row);
        ((TextView) fontSizeRow.findViewById(R.id.control_label)).setText(R.string.settings_font_size);
        ((TextView) fontSizeRow.findViewById(R.id.control_sublabel)).setText(R.string.settings_font_size_sub);
        fontValue = fontSizeRow.findViewById(R.id.control_value);
        fontSizeRow.findViewById(R.id.control_decrement).setOnClickListener(v -> changeFontSize(-1));
        fontSizeRow.findViewById(R.id.control_increment).setOnClickListener(v -> changeFontSize(1));

        // Columns Control
        View columnsRow = findViewById(R.id.columns_row);
        ((TextView) columnsRow.findViewById(R.id.control_label)).setText(R.string.settings_columns);
        ((TextView) columnsRow.findViewById(R.id.control_sublabel)).setText(R.string.settings_columns_sub);
        columnsValue = columnsRow.findViewById(R.id.control_value);
        columnsRow.findViewById(R.id.control_decrement).setOnClickListener(v -> changeColumnCount(-1));
        columnsRow.findViewById(R.id.control_increment).setOnClickListener(v -> changeColumnCount(1));

        // Categories Section Header
        View categoriesHeader = findViewById(R.id.categories_header);
        ((TextView) categoriesHeader.findViewById(R.id.section_title)).setText(R.string.settings_categories_section);

        categoryChecks = findViewById(R.id.category_checks);
        fontSizeSp = DictionaryPrefs.getFontSizeSp(this);
        columnCount = DictionaryPrefs.getColumnCount(this);

        View save = findViewById(R.id.settings_done);
        View cancel = findViewById(R.id.settings_cancel);
        View topCancel = findViewById(R.id.settings_top_cancel);

        cancel.setOnClickListener(v -> finish());
        topCancel.setOnClickListener(v -> finish());
        save.setOnClickListener(v -> {
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
        fontValue.setText(getString(R.string.font_size_format, fontSizeSp));
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
        columnsValue.setText(String.format(java.util.Locale.getDefault(), "%d", columnCount));
    }

    private void bindCategoryChecks() {
        Set<Long> activeIds = DictionaryPrefs.getActiveCategoryIds(this);
        List<Category> categories = db.getCategories();
        categoryChecks.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Category category : categories) {
            View itemView = inflater.inflate(R.layout.component_category_select_item, categoryChecks, false);
            
            TextView nameView = itemView.findViewById(R.id.category_name);
            CheckBox checkBox = itemView.findViewById(R.id.category_checkbox);

            nameView.setText(category.name);
            itemView.setTag(category.id);
            checkBox.setChecked(activeIds == null || activeIds.contains(category.id));
            
            // Reusable row click to toggle checkbox
            itemView.setOnClickListener(v -> checkBox.toggle());

            categoryChecks.addView(itemView);
        }
    }

    private void saveActiveCategories() {
        Set<Long> activeIds = new HashSet<>();
        for (int i = 0; i < categoryChecks.getChildCount(); i++) {
            View itemView = categoryChecks.getChildAt(i);
            CheckBox checkBox = itemView.findViewById(R.id.category_checkbox);
            if (checkBox.isChecked()) {
                activeIds.add((Long) itemView.getTag());
            }
        }
        DictionaryPrefs.setActiveCategoryIds(this, activeIds);
    }

    private void checkAndRequestWidgetPinning() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName provider = new ComponentName(this, DictionaryWidgetProvider.class);
            int[] ids = appWidgetManager.getAppWidgetIds(provider);

            if (ids.length == 0) {
                if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                    appWidgetManager.requestPinAppWidget(provider, null, null);
                }
            }
        }
    }
}
