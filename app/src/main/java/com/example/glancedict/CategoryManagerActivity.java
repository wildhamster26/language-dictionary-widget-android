package com.example.glancedict;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CategoryManagerActivity extends Activity implements CategoryAdapter.OnCategoryActionListener {
    private DictionaryDbHelper db;
    private RecyclerView recycler;
    private CategoryAdapter adapter;
    private ItemTouchHelper itemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        db = new DictionaryDbHelper(this);
        recycler = findViewById(R.id.category_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        setupAdapter();

        findViewById(R.id.categories_cancel_top).setOnClickListener(v -> finish());
        findViewById(R.id.categories_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.categories_save).setOnClickListener(v -> saveOrder());
    }

    private void setupAdapter() {
        List<Category> categories = db.getCategories();
        adapter = new CategoryAdapter(categories, this, viewHolder -> {
            if (itemTouchHelper != null) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        recycler.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (target.getAdapterPosition() == 0) return false;
                adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public int getDragDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == 0) return 0;
                return super.getDragDirs(recyclerView, viewHolder);
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recycler);
    }

    @Override
    public void onCreateCategory(String name) {
        long id = db.createCategory(name);
        if (id <= 0) {
            Toast.makeText(this, "Category already exists or error.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.refreshLongestTextCache(this);
        setupAdapter(); // Refresh entire list for simplicity
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
                    db.deleteCategoryAndWords(category.id);
                    db.refreshLongestTextCache(this);
                    setupAdapter();
                    WidgetRefresh.refreshAll(this);
                })
                .show();
    }

    private void saveOrder() {
        List<Category> currentList = adapter.getCategories();
        List<Long> ids = new ArrayList<>();
        for (Category c : currentList) {
            ids.add(c.id);
        }
        db.updateCategoryOrder(ids);
        db.refreshLongestTextCache(this);
        WidgetRefresh.refreshAll(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}
