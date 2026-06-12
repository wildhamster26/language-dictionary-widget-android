package com.joinrestartabroad.glancedict;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuickActionActivity extends Activity {
    private DictionaryDbHelper db;
    private long wordId = -1L;
    private Word word;
    private Spinner categorySpinner;
    private EditText nativeInput;
    private EditText translationInput;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long categoryId = getIntent().getLongExtra(DictionaryWidgetProvider.EXTRA_CATEGORY_ID, -1L);
        if (categoryId != -1L) {
            DictionaryPrefs.toggleCollapsedCategory(this, categoryId);
            WidgetRefresh.refreshAll(this);
            finish();
            return;
        }

        wordId = getIntent().getLongExtra(DictionaryWidgetProvider.EXTRA_WORD_ID, -1L);
        db = new DictionaryDbHelper(this);
        word = db.getWord(wordId);
        if (word == null) {
            finish();
            return;
        }

        if (getIntent().getBooleanExtra(DictionaryWidgetProvider.EXTRA_COPY_TARGET, false)) {
            copyTranslatedWord(word.translatedWord);
            finish();
            return;
        }

        setContentView(R.layout.activity_quick_action);

        categorySpinner = findViewById(R.id.category_spinner);
        nativeInput = findViewById(R.id.native_input);
        translationInput = findViewById(R.id.translation_input);

        bindCategories();
        nativeInput.setText(word.nativeWord);
        translationInput.setText(word.translatedWord);
        translationInput.setOnLongClickListener(v -> {
            copyTranslatedWord(translationInput.getText().toString().trim());
            return false;
        });

        View save = findViewById(R.id.save_word);
        View delete = findViewById(R.id.delete_word);
        View cancel = findViewById(R.id.edit_word_cancel);

        save.setOnClickListener(v -> saveWord());
        delete.setOnClickListener(v -> deleteWord());
        cancel.setOnClickListener(v -> finish());
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

    private void bindCategories() {
        List<Category> categories = db.getCategories();
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id == word.categoryId) {
                categorySpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveWord() {
        Category category = (Category) categorySpinner.getSelectedItem();
        String nativeWord = nativeInput.getText().toString().trim();
        String translatedWord = translationInput.getText().toString().trim();
        if (category == null || nativeWord.isEmpty() || translatedWord.isEmpty()) {
            Toast.makeText(this, "Both fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        long catId = category.id;
        executor.execute(() -> {
            db.updateWord(wordId, catId, nativeWord, translatedWord);
            db.refreshLongestTextCache(getApplicationContext());
            mainHandler.post(() -> {
                if (!destroyed) {
                    WidgetRefresh.refreshAll(QuickActionActivity.this);
                    finish();
                }
            });
        });
    }

    private void deleteWord() {
        executor.execute(() -> {
            db.deleteWord(wordId);
            db.refreshLongestTextCache(getApplicationContext());
            mainHandler.post(() -> {
                if (!destroyed) {
                    WidgetRefresh.refreshAll(QuickActionActivity.this);
                    finish();
                }
            });
        });
    }

    private void copyTranslatedWord(String translatedWord) {
        if (translatedWord == null || translatedWord.trim().isEmpty()) {
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }

        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.clipboard_label_target_word),
                translatedWord));
        Toast.makeText(this, R.string.toast_copied_target_word, Toast.LENGTH_SHORT).show();
    }
}
