package com.example.glancedict;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

        Button create = findViewById(R.id.create_category);
        Button done = findViewById(R.id.categories_done);
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
        long id = db.createCategory(nameInput.getText().toString());
        if (id <= 0) {
            Toast.makeText(this, "Enter a category name.", Toast.LENGTH_SHORT).show();
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
        for (Category category : categories) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(48));

            TextView label = new TextView(this);
            label.setText(category.name + " (" + db.getWordCountForCategory(category.id) + ")");
            label.setTextColor(0xFF111827);
            label.setTextSize(16);
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            if (category.id != defaultId) {
                Button delete = new Button(this);
                delete.setText("Delete");
                delete.setTextColor(getColor(R.color.danger));
                delete.setOnClickListener(v -> confirmDelete(category));
                row.addView(delete, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(44)));
            }

            categoryList.addView(row);
        }
    }

    private void confirmDelete(Category category) {
        int wordCount = db.getWordCountForCategory(category.id);
        String message = wordCount == 0
                ? "This category is empty."
                : "All " + wordCount + " words in this category will be permanently deleted.";
        new AlertDialog.Builder(this)
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
