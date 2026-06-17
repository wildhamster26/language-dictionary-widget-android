package com.joinrestartabroad.glancedict;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private DictionaryDbHelper db;
    private TextView fontValue;
    private TextView categoryFontValue;
    private TextView columnsValue;
    private TextView sortByLengthButton;
    private TextView sourceLanguageValue;
    private TextView targetLanguageValue;
    private View welcomeState;
    private View settingsScroll;
    private View settingsBottomActions;
    private TextView welcomeMessage;
    private View welcomeCreateWidgetButton;
    private AlertDialog downloadProgressDialog;
    private Translator downloadTranslator;
    private static final long DOWNLOAD_TIMEOUT_MS = 60_000L;
    private final Runnable downloadTimeoutRunnable = this::onDownloadTimeout;
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
        welcomeState = findViewById(R.id.welcome_state);
        settingsScroll = findViewById(R.id.settings_scroll);
        settingsBottomActions = findViewById(R.id.settings_bottom_actions);
        welcomeMessage = findViewById(R.id.welcome_message);
        welcomeCreateWidgetButton = findViewById(R.id.welcome_create_widget);
        welcomeCreateWidgetButton.setOnClickListener(v -> checkAndRequestWidgetPinning());
        updateWelcomeState();

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

        // Translation Section
        View translationHeader = findViewById(R.id.translation_header);
        ((TextView) translationHeader.findViewById(R.id.section_title)).setText(R.string.settings_translation_section);

        View sourceLanguageRow = findViewById(R.id.source_language_row);
        ((TextView) sourceLanguageRow.findViewById(R.id.language_label)).setText(R.string.settings_my_language);
        ((TextView) sourceLanguageRow.findViewById(R.id.language_sublabel)).setText(R.string.settings_my_language_sub);
        sourceLanguageValue = sourceLanguageRow.findViewById(R.id.language_value);
        sourceLanguageRow.setOnClickListener(v -> showLanguagePickerDialog(true));

        View targetLanguageRow = findViewById(R.id.target_language_row);
        ((TextView) targetLanguageRow.findViewById(R.id.language_label)).setText(R.string.settings_translate_to);
        ((TextView) targetLanguageRow.findViewById(R.id.language_sublabel)).setText(R.string.settings_translate_to_sub);
        targetLanguageValue = targetLanguageRow.findViewById(R.id.language_value);
        targetLanguageRow.setOnClickListener(v -> showLanguagePickerDialog(false));

        updateFontValue();
        updateCategoryFontValue();
        updateColumnsValue();
        updateSortByLengthButton();
        updateLanguageValues();
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
        mainHandler.removeCallbacks(downloadTimeoutRunnable);
        if (db != null) {
            db.close();
        }
        if (downloadTranslator != null) {
            downloadTranslator.close();
            downloadTranslator = null;
        }
        if (downloadProgressDialog != null && downloadProgressDialog.isShowing()) {
            downloadProgressDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWelcomeState();
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

    private void updateLanguageValues() {
        String source = DictionaryPrefs.getSourceLanguage(this);
        sourceLanguageValue.setText(displayLanguage(source));
        sourceLanguageValue.setTextColor(getColor(R.color.word_native));

        String target = DictionaryPrefs.getTargetLanguage(this);
        if (target != null) {
            targetLanguageValue.setText(displayLanguage(target));
            targetLanguageValue.setTextColor(getColor(R.color.word_native));
        } else {
            targetLanguageValue.setText(R.string.hint_tap_to_select);
            targetLanguageValue.setTextColor(getColor(R.color.text_hint));
        }
    }

    private void showLanguagePickerDialog(boolean isSource) {
        List<LanguageItem> languages = buildLanguageList();
        String[] names = new String[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            names[i] = languages.get(i).displayName;
        }

        String current = isSource
                ? DictionaryPrefs.getSourceLanguage(this)
                : DictionaryPrefs.getTargetLanguage(this);
        int checkedItem = -1;
        if (current != null) {
            for (int i = 0; i < languages.size(); i++) {
                if (languages.get(i).code.equals(current)) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setTitle(isSource ? R.string.settings_my_language : R.string.settings_translate_to)
                .setSingleChoiceItems(names, checkedItem, (dialog, which) -> {
                    String selected = languages.get(which).code;
                    if (isSource) {
                        DictionaryPrefs.setSourceLanguage(this, selected);
                    } else {
                        DictionaryPrefs.setTargetLanguage(this, selected);
                    }
                    updateLanguageValues();
                    dialog.dismiss();
                    downloadModelsIfBothSet();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void downloadModelsIfBothSet() {
        String source = DictionaryPrefs.getSourceLanguage(this);
        String target = DictionaryPrefs.getTargetLanguage(this);
        if (source == null || target == null) return;
        if (source.equals(target)) return;

        // Build options before showing any progress so an unsupported language is
        // reported accurately instead of as a network failure.
        TranslatorOptions options;
        try {
            options = new TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build();
        } catch (IllegalArgumentException e) {
            showUnsupportedLanguageDialog();
            return;
        }

        final Translator translator;
        try {
            translator = Translation.getClient(options);
        } catch (RuntimeException e) {
            showDownloadFailedDialog();
            return;
        }
        downloadTranslator = translator;

        downloadProgressDialog = new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setMessage(R.string.dialog_downloading_model)
                .setCancelable(true)
                .setOnCancelListener(d -> {
                    mainHandler.removeCallbacks(downloadTimeoutRunnable);
                    closeDownloadTranslator(translator);
                })
                .create();
        downloadProgressDialog.show();
        mainHandler.postDelayed(downloadTimeoutRunnable, DOWNLOAD_TIMEOUT_MS);

        try {
            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {
                        if (downloadTranslator != translator) {
                            translator.close();
                            return;
                        }
                        mainHandler.removeCallbacks(downloadTimeoutRunnable);
                        closeDownloadTranslator(translator);
                        if (!destroyed) {
                            if (downloadProgressDialog != null && downloadProgressDialog.isShowing()) {
                                downloadProgressDialog.dismiss();
                            }
                            Toast.makeText(this, R.string.toast_model_ready, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (downloadTranslator != translator) {
                            translator.close();
                            return;
                        }
                        mainHandler.removeCallbacks(downloadTimeoutRunnable);
                        closeDownloadTranslator(translator);
                        showDownloadFailedDialog();
                    });
        } catch (RuntimeException e) {
            mainHandler.removeCallbacks(downloadTimeoutRunnable);
            closeDownloadTranslator(translator);
            showDownloadFailedDialog();
        }
    }

    private void onDownloadTimeout() {
        Translator translator = downloadTranslator;
        downloadTranslator = null;
        if (translator != null) {
            translator.close();
        }
        showDownloadFailedDialog();
    }

    private void closeDownloadTranslator(Translator translator) {
        translator.close();
        if (downloadTranslator == translator) {
            downloadTranslator = null;
        }
    }

    private void showDownloadFailedDialog() {
        if (destroyed) return;
        if (downloadProgressDialog != null && downloadProgressDialog.isShowing()) {
            downloadProgressDialog.dismiss();
        }
        new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setTitle(R.string.dialog_download_failed_title)
                .setMessage(R.string.dialog_download_failed_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showUnsupportedLanguageDialog() {
        if (destroyed) return;
        new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setTitle(R.string.dialog_language_unsupported_title)
                .setMessage(R.string.dialog_language_unsupported_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private List<LanguageItem> buildLanguageList() {
        List<LanguageItem> list = new ArrayList<>();
        for (String code : TranslateLanguage.getAllLanguages()) {
            String display = displayLanguage(code);
            list.add(new LanguageItem(code, display));
        }
        //noinspection ComparatorCombinators
        Collections.sort(list, (a, b) -> a.displayName.compareTo(b.displayName));
        return list;
    }

    private String displayLanguage(String code) {
        return Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault());
    }

    private static final class LanguageItem {
        final String code;
        final String displayName;

        LanguageItem(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
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
        updateWelcomeState();
    }

    private void updateWelcomeState() {
        if (welcomeState == null || settingsScroll == null || settingsBottomActions == null) {
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, DictionaryWidgetProvider.class);
        boolean hasWidget = appWidgetManager.getAppWidgetIds(provider).length > 0;
        boolean showWelcome = appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !hasWidget;

        welcomeState.setVisibility(showWelcome ? View.VISIBLE : View.GONE);
        settingsScroll.setVisibility(showWelcome ? View.GONE : View.VISIBLE);
        settingsBottomActions.setVisibility(showWelcome ? View.GONE : View.VISIBLE);
        if (welcomeCreateWidgetButton != null) {
            boolean canPin = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && appWidgetManager.isRequestPinAppWidgetSupported();
            welcomeCreateWidgetButton.setEnabled(canPin);
            if (welcomeMessage != null) {
                welcomeMessage.setText(canPin
                        ? R.string.body_welcome_widget
                        : R.string.body_widget_unsupported);
            }
        }
    }
}
