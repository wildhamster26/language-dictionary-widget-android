package com.example.glancedict;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Category> categories;
    private final OnCategoryActionListener listener;
    private final DragStartListener dragStartListener;

    public interface OnCategoryActionListener {
        void onCreateCategory(String name);
        void onDeleteCategory(Category category);
    }

    public interface DragStartListener {
        void onDragStarted(RecyclerView.ViewHolder viewHolder);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryActionListener listener, DragStartListener dragStartListener) {
        this.categories = categories;
        this.listener = listener;
        this.dragStartListener = dragStartListener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_category_manager, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_category_item, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.createButton.setOnClickListener(v -> {
                String name = h.nameInput.getText().toString().trim();
                if (!name.isEmpty()) {
                    listener.onCreateCategory(name);
                    h.nameInput.setText("");
                }
            });
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder h = (ItemViewHolder) holder;
            Category category = categories.get(position - 1);
            h.nameView.setText(category.name);
            
            // Reorder handle logic
            h.reorderHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    dragStartListener.onDragStarted(h);
                }
                return false;
            });

            // Delete logic (hide for Uncategorized)
            if (category.name.equals(DictionaryDbHelper.DEFAULT_CATEGORY)) {
                h.deleteButton.setVisibility(View.GONE);
                h.reorderHandle.setVisibility(View.INVISIBLE); // Lock Uncategorized
            } else {
                h.deleteButton.setVisibility(View.VISIBLE);
                h.reorderHandle.setVisibility(View.VISIBLE);
                h.deleteButton.setOnClickListener(v -> listener.onDeleteCategory(category));
            }
        }
    }

    @Override
    public int getItemCount() {
        return categories.size() + 1;
    }

    public void onItemMove(int fromPosition, int toPosition) {
        // Prevent moving header or moving items into header position
        if (fromPosition == 0 || toPosition == 0) return;
        
        // Prevent moving "Uncategorized" (usually first item at pos 1)
        if (categories.get(fromPosition - 1).name.equals(DictionaryDbHelper.DEFAULT_CATEGORY) ||
            categories.get(toPosition - 1).name.equals(DictionaryDbHelper.DEFAULT_CATEGORY)) {
            return;
        }

        Collections.swap(categories, fromPosition - 1, toPosition - 1);
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<Category> getCategories() {
        return categories;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        EditText nameInput;
        View createButton;

        HeaderViewHolder(View itemView) {
            super(itemView);
            nameInput = itemView.findViewById(R.id.category_name_input);
            createButton = itemView.findViewById(R.id.create_category);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        View deleteButton;
        View reorderHandle;

        ItemViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.category_name);
            deleteButton = itemView.findViewById(R.id.delete_category);
            reorderHandle = itemView.findViewById(R.id.reorder_handle);
        }
    }
}
