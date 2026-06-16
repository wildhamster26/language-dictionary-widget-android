package com.joinrestartabroad.glancedict;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private DictionaryDbHelper db;
    private TextView fontValue;
    private TextView categoryFontValue;
    private TextView columnsValue;
    private TextView sortByLengthButton;
    private int fontSizeSp;
    private int categoryFontSizeSp;
    private int columnCount;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean destroyed;

    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DictionaryWidgetProvider.ACTION_CLOSE_SETTINGS.equals(intent.getAction())) {
                finish();
            }
        }
    };

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

        // Category Font Size Control
        View categoryFontSizeRow = findViewById(R.id.category_font_size_row);
        ((TextView) categoryFontSizeRow.findViewById(R.id.control_label)).setText(R.string.settings_category_font_size);
        ((TextView) categoryFontSizeRow.findViewById(R.id.control_sublabel)).setText(R.string.settings_category_font_size_sub);
        categoryFontValue = categoryFontSizeRow.findViewById(R.id.control_value);
        categoryFontSizeRow.findViewById(R.id.control_decrement).setOnClickListener(v -> changeCategoryFontSize(-1));
        categoryFontSizeRow.findViewById(R.id.control_increment).setOnClickListener(v -> changeCategoryFontSize(1));

        // Columns Control
        View columnsRow = findViewById(R.id.columns_row);
        ((TextView) columnsRow.findViewById(R.id.control_label)).setText(R.string.settings_columns);
        ((TextView) columnsRow.findViewById(R.id.control_sublabel)).setText(R.string.settings_columns_sub);
        columnsValue = columnsRow.findViewById(R.id.control_value);
        columnsRow.findViewById(R.id.control_decrement).setOnClickListener(v -> changeColumnCount(-1));
        columnsRow.findViewById(R.id.control_increment).setOnClickListener(v -> changeColumnCount(1));

        // Sort by Length Toggle
        View sortByLengthRow = findViewById(R.id.sort_by_length_row);
        ((TextView) sortByLengthRow.findViewById(R.id.toggle_label)).setText(R.string.settings_sort_by_length);
        ((TextView) sortByLengthRow.findViewById(R.id.toggle_sublabel)).setText(R.string.settings_sort_by_length_sub);
        sortByLengthButton = sortByLengthRow.findViewById(R.id.toggle_button);
        sortByLengthButton.setOnClickListener(v -> toggleSortByLength());

        fontSizeSp = DictionaryPrefs.getFontSizeSp(this);
        categoryFontSizeSp = DictionaryPrefs.getCategoryFontSizeSp(this);
        columnCount = DictionaryPrefs.getColumnCount(this);

        View save = findViewById(R.id.settings_done);
        View cancel = findViewById(R.id.settings_cancel);
        View topCancel = findViewById(R.id.settings_top_cancel);

        cancel.setOnClickListener(v -> finish());
        topCancel.setOnClickListener(v -> finish());
        findViewById(R.id.privacy_policy).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))));
        save.setOnClickListener(v -> {
            save.setEnabled(false);
            executor.execute(() -> {
                db.refreshLongestTextCache(getApplicationContext());
                mainHandler.post(() -> {
                    if (!destroyed) {
                        save.setEnabled(true);
                        WidgetRefresh.refreshAll(SettingsActivity.this);
                        finishConfiguration();
                    }
                });
            });
        });

        updateFontValue();
        updateCategoryFontValue();
        updateColumnsValue();
        updateSortByLengthButton();
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
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(
                this,
                closeReceiver,
                new IntentFilter(DictionaryWidgetProvider.ACTION_CLOSE_SETTINGS),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(closeReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        executor.shutdownNow();
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

    private void changeCategoryFontSize(int delta) {
        categoryFontSizeSp = Math.max(
                DictionaryPrefs.MIN_CATEGORY_FONT_SP,
                Math.min(DictionaryPrefs.MAX_CATEGORY_FONT_SP, categoryFontSizeSp + delta));
        DictionaryPrefs.setCategoryFontSizeSp(this, categoryFontSizeSp);
        updateCategoryFontValue();
        WidgetRefresh.refreshAll(this);
    }

    private void updateCategoryFontValue() {
        categoryFontValue.setText(getString(R.string.font_size_format, categoryFontSizeSp));
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

    private void toggleSortByLength() {
        boolean next = !DictionaryPrefs.isSortByLength(this);
        DictionaryPrefs.setSortByLength(this, next);
        updateSortByLengthButton();
        WidgetRefresh.refreshAll(this);
    }

    private void updateSortByLengthButton() {
        boolean on = DictionaryPrefs.isSortByLength(this);
        sortByLengthButton.setText(on ? R.string.toggle_on : R.string.toggle_off);
        sortByLengthButton.setTextColor(getColor(on ? R.color.secondary : R.color.text_hint));
    }

    private void checkAndRequestWidgetPinning() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName provider = new ComponentName(this, DictionaryWidgetProvider.class);
            int[] ids = appWidgetManager.getAppWidgetIds(provider);

            if (ids.length == 0) {
                if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                    Intent pinnedIntent = new Intent(this, DictionaryWidgetProvider.class);
                    pinnedIntent.setAction(DictionaryWidgetProvider.ACTION_WIDGET_PINNED);
                    PendingIntent successCallback = PendingIntent.getBroadcast(
                            this,
                            0,
                            pinnedIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    appWidgetManager.requestPinAppWidget(provider, null, successCallback);
                }
            }
        }
    }
}
