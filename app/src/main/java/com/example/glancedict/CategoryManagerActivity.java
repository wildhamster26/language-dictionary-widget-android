package com.example.glancedict;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class CategoryManagerActivity extends Activity {
    private DictionaryDbHelper db;
    private EditText nameInput;
    private LinearLayout categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        db = new DictionaryDbHelper(this);
        nameInput = findViewById(R.id.category_name_input);
        categoryList = findViewById(R.id.category_list);

        View create = findViewById(R.id.create_category);
        View done = findViewById(R.id.categories_done);
        create.setOnClickListener(v -> createCategory());
        done.setOnClickListener(v -> {
            WidgetRefresh.refreshAll(this);
            finish();
        });

        renderCategories();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    private void createCategory() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a category name.", Toast.LENGTH_SHORT).show();
            return;
        }
        long id = db.createCategory(name);
        if (id <= 0) {
            Toast.makeText(this, "Category already exists or error.", Toast.LENGTH_SHORT).show();
            return;
        }
        nameInput.setText("");
        db.refreshLongestTextCache(this);
        renderCategories();
        WidgetRefresh.refreshAll(this);
    }

    private void renderCategories() {
        categoryList.removeAllViews();
        List<Category> categories = db.getCategories();
        long defaultId = db.ensureDefaultCategory();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Category category : categories) {
            View itemView = inflater.inflate(R.layout.component_category_item, categoryList, false);

            TextView nameView = itemView.findViewById(R.id.category_name);
            TextView countView = itemView.findViewById(R.id.word_count);
            ImageView deleteIcon = itemView.findViewById(R.id.delete_category);

            nameView.setText(category.name);
            int wordCount = db.getWordCountForCategory(category.id);
            countView.setText(wordCount + (wordCount == 1 ? " Word" : " Words"));

            if (category.id != defaultId) {
                deleteIcon.setVisibility(View.VISIBLE);
                deleteIcon.setOnClickListener(v -> confirmDelete(category));
            } else {
                deleteIcon.setVisibility(View.GONE);
            }

            categoryList.addView(itemView);
        }
    }

    private void confirmDelete(Category category) {
        int wordCount = db.getWordCountForCategory(category.id);
        String message = wordCount == 0
                ? "This category is empty."
                : "All " + wordCount + " words in this category will be permanently deleted.";
        new AlertDialog.Builder(this, R.style.RoundedDialogTheme)
                .setTitle("Delete \"" + category.name + "\"?")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deleteCategoryAndWords(category.id);
                    db.refreshLongestTextCache(this);
                    renderCategories();
                    WidgetRefresh.refreshAll(this);
                })
                .show();
    }
}
