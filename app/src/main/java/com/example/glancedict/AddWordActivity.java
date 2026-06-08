package com.example.glancedict;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddWordActivity extends Activity {
    private DictionaryDbHelper db;
    private Spinner categorySpinner;
    private EditText nativeInput;
    private EditText translationInput;
    private EditText bulkInput;
    private View save;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_word);

        db = new DictionaryDbHelper(this);
        categorySpinner = findViewById(R.id.category_spinner);
        nativeInput = findViewById(R.id.native_input);
        translationInput = findViewById(R.id.translation_input);
        bulkInput = findViewById(R.id.bulk_input);

        save = findViewById(R.id.save_word);
        TextView manageCategories = findViewById(R.id.manage_categories);
        View cancel = findViewById(R.id.add_word_cancel);
        View topCancel = findViewById(R.id.add_word_top_cancel);
        
        save.setOnClickListener(v -> saveWords());
        manageCategories.setOnClickListener(v -> startActivity(new Intent(this, CategoryManagerActivity.class)));
        cancel.setOnClickListener(v -> finish());
        topCancel.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindCategories();
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
    }

    private void saveWords() {
        Category category = (Category) categorySpinner.getSelectedItem();
        if (category == null) {
            Toast.makeText(this, "Please select or create a category.", Toast.LENGTH_SHORT).show();
            return;
        }

        String bulk = bulkInput.getText().toString().trim();
        String nativeWord = nativeInput.getText().toString().trim();
        String translatedWord = translationInput.getText().toString().trim();

        if (bulk.isEmpty() && (nativeWord.isEmpty() || translatedWord.isEmpty())) {
            Toast.makeText(this, "Enter a word pair or use bulk upload.", Toast.LENGTH_SHORT).show();
            return;
        }

        long categoryId = category.id;
        save.setEnabled(false);

        executor.execute(() -> {
            int saved = 0;
            RuntimeException failure = null;
            try (DictionaryDbHelper workerDb = new DictionaryDbHelper(getApplicationContext())) {
                if (!bulk.isEmpty() && BulkWordParser.isJson(bulk)) {
                    List<BulkWordParser.CategoryGroup> groups = BulkWordParser.parseJson(bulk);
                    for (BulkWordParser.CategoryGroup group : groups) {
                        long gCategoryId = workerDb.createCategory(group.name);
                        if (gCategoryId > 0) {
                            saved += workerDb.addWords(gCategoryId, group.pairs);
                        }
                    }
                } else if (!bulk.isEmpty()) {
                    saved = workerDb.addWords(categoryId, BulkWordParser.parse(bulk));
                } else {
                    if (workerDb.addWord(categoryId, nativeWord, translatedWord) > 0) {
                        saved = 1;
                    }
                }

                if (saved > 0) {
                    workerDb.refreshLongestTextCache(getApplicationContext());
                }
            } catch (RuntimeException exception) {
                failure = exception;
            }

            int finalSaved = saved;
            RuntimeException finalFailure = failure;
            mainHandler.post(() -> onSaveComplete(finalSaved, finalFailure));
        });
    }

    private void onSaveComplete(int saved, RuntimeException failure) {
        if (destroyed) {
            return;
        }

        save.setEnabled(true);
        if (failure != null) {
            Toast.makeText(this, "Could not save words.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (saved == 0) {
            Toast.makeText(this, "Enter a word pair or valid bulk lines.", Toast.LENGTH_SHORT).show();
            return;
        }

        WidgetRefresh.refreshAll(this);
        finish();
    }
}
