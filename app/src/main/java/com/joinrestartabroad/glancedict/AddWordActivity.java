package com.joinrestartabroad.glancedict;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddWordActivity extends Activity implements CategoryAdapter.OnCategoryActionListener {
    private DictionaryDbHelper db;
    private Spinner categorySpinner;
    private EditText nativeInput;
    private EditText translationInput;
    private EditText bulkInput;
    private TextView saveButton;
    private View panelWords;
    private RecyclerView categoryRecycler;
    private TextView tabWords;
    private TextView tabCategories;
    private View tabWordsIndicator;
    private View tabCategoriesIndicator;
    private CategoryAdapter categoryAdapter;
    private TextView translateButton;
    private TextView translateHint;
    private Translator activeTranslator;
    private ItemTouchHelper itemTouchHelper;
    private boolean onWordsTab = true;

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
        saveButton = findViewById(R.id.save_word);
        panelWords = findViewById(R.id.panel_words);
        categoryRecycler = findViewById(R.id.panel_categories);
        tabWords = findViewById(R.id.tab_words);
        tabCategories = findViewById(R.id.tab_categories);
        tabWordsIndicator = findViewById(R.id.tab_words_indicator);
        tabCategoriesIndicator = findViewById(R.id.tab_categories_indicator);

        translateButton = findViewById(R.id.translate_button);
        translateHint = findViewById(R.id.translate_hint);
        translateButton.setOnClickListener(v -> fetchTranslation());
        nativeInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateTranslateButton(); }
        });

        tabWords.setOnClickListener(v -> switchTab(true));
        tabCategories.setOnClickListener(v -> switchTab(false));
        saveButton.setOnClickListener(v -> {
            if (onWordsTab) saveWords();
            else saveCategoryOrder();
        });
        findViewById(R.id.add_word_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.add_word_top_cancel).setOnClickListener(v -> finish());

        categoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        refreshCategoryAdapter();
        updateTranslateButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindSpinnerCategories();
        updateTranslateButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        executor.shutdownNow();
        if (db != null) db.close();
        if (activeTranslator != null) {
            activeTranslator.close();
            activeTranslator = null;
        }
    }

    private void updateTranslateButton() {
        boolean hasTarget = DictionaryPrefs.getTargetLanguage(this) != null;
        boolean hasText = nativeInput.getText().length() > 0;
        boolean enabled = hasTarget && hasText;
        translateButton.setEnabled(enabled);
        translateButton.setAlpha(enabled ? 1.0f : 0.4f);
        translateHint.setVisibility(!hasTarget ? View.VISIBLE : View.GONE);
    }

    private void fetchTranslation() {
        String nativeWord = nativeInput.getText().toString().trim();
        if (nativeWord.isEmpty()) return;

        String source = DictionaryPrefs.getSourceLanguage(this);
        String target = DictionaryPrefs.getTargetLanguage(this);
        if (target == null) return;

        translateButton.setEnabled(false);
        translateButton.setAlpha(0.4f);
        translateButton.setText("…");

        if (activeTranslator != null) {
            activeTranslator.close();
        }
        TranslatorOptions options;
        try {
            options = new TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build();
        } catch (IllegalArgumentException e) {
            translateButton.setText(R.string.action_translate);
            updateTranslateButton();
            Toast.makeText(this, R.string.toast_translation_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        activeTranslator = Translation.getClient(options);
        activeTranslator.translate(nativeWord)
                .addOnSuccessListener(result -> {
                    if (!destroyed) {
                        translationInput.setText(result);
                        translateButton.setText(R.string.action_translate);
                        updateTranslateButton();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!destroyed) {
                        translateButton.setText(R.string.action_translate);
                        updateTranslateButton();
                        Toast.makeText(this, R.string.toast_translation_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void switchTab(boolean wordsTab) {
        onWordsTab = wordsTab;
        panelWords.setVisibility(wordsTab ? View.VISIBLE : View.GONE);
        categoryRecycler.setVisibility(wordsTab ? View.GONE : View.VISIBLE);
        tabWords.setTextColor(getColor(wordsTab ? R.color.button_create_bg : R.color.text_hint));
        tabCategories.setTextColor(getColor(wordsTab ? R.color.text_hint : R.color.button_create_bg));
        tabWordsIndicator.setVisibility(wordsTab ? View.VISIBLE : View.INVISIBLE);
        tabCategoriesIndicator.setVisibility(wordsTab ? View.INVISIBLE : View.VISIBLE);
        saveButton.setText(R.string.action_save);
    }

    private void bindSpinnerCategories() {
        List<Category> categories = db.getCategories();
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void refreshCategoryAdapter() {
        refreshCategoryAdapter(categoryAdapter == null
                ? DictionaryPrefs.getActiveCategoryIds(this)
                : categoryAdapter.getActiveCategoryIds());
    }

    private void refreshCategoryAdapter(Set<Long> activeCategoryIds) {
        List<Category> categories = db.getCategories();
        categoryAdapter = new CategoryAdapter(categories, activeCategoryIds, this, viewHolder -> {
            if (itemTouchHelper != null) itemTouchHelper.startDrag(viewHolder);
        });
        categoryRecycler.setAdapter(categoryAdapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (target.getBindingAdapterPosition() == 0) return false;
                categoryAdapter.onItemMove(vh.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public int getDragDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                if (vh.getItemViewType() == 0) return 0;
                return super.getDragDirs(rv, vh);
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(categoryRecycler);
    }

    @Override
    public void onCreateCategory(String name) {
        long id = db.createCategory(name);
        if (id <= 0) {
            Toast.makeText(this, "Category already exists or error.", Toast.LENGTH_SHORT).show();
            return;
        }
        Set<Long> activeIds = currentActiveCategoryIds();
        activeIds.add(id);
        db.refreshLongestTextCache(this);
        refreshCategoryAdapter(activeIds);
        bindSpinnerCategories();
        WidgetRefresh.refreshAll(this);
    }

    @Override
    public void onDeleteCategory(Category category) {
        int wordCount = db.getWordCountForCategory(category.id);
        String message = wordCount == 0
                ? "This category is empty."
                : "All " + wordCount + " words in this category will be permanently deleted.";
        new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setTitle("Delete \"" + category.name + "\"?")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    Set<Long> activeIds = currentActiveCategoryIds();
                    activeIds.remove(category.id);
                    db.deleteCategoryAndWords(category.id);
                    db.refreshLongestTextCache(this);
                    refreshCategoryAdapter(activeIds);
                    bindSpinnerCategories();
                    WidgetRefresh.refreshAll(this);
                })
                .show();
    }

    private void saveCategoryOrder() {
        List<Category> currentList = categoryAdapter.getCategories();
        List<Long> ids = new ArrayList<>();
        for (Category c : currentList) ids.add(c.id);
        db.updateCategoryOrder(ids);
        DictionaryPrefs.setActiveCategoryIds(this, categoryAdapter.getActiveCategoryIds());
        db.refreshLongestTextCache(this);
        WidgetRefresh.refreshAll(this);
        bindSpinnerCategories();
        finish();
    }

    private Set<Long> currentActiveCategoryIds() {
        if (categoryAdapter != null) {
            return categoryAdapter.getActiveCategoryIds();
        }
        Set<Long> storedIds = DictionaryPrefs.getActiveCategoryIds(this);
        return storedIds == null ? new HashSet<>() : storedIds;
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
        saveButton.setEnabled(false);

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
        if (destroyed) return;

        saveButton.setEnabled(true);
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
