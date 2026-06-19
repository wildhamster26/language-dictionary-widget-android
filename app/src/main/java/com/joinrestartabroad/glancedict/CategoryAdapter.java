package com.joinrestartabroad.glancedict;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Category> categories;
    private final OnCategoryActionListener listener;
    private final DragStartListener dragStartListener;
    private final Set<Long> activeCategoryIds;

    public interface OnCategoryActionListener {
        void onCreateCategory(String name);
        void onDeleteCategory(Category category);
    }

    public interface DragStartListener {
        void onDragStarted(RecyclerView.ViewHolder viewHolder);
    }

    public CategoryAdapter(
            List<Category> categories,
            Set<Long> activeCategoryIds,
            OnCategoryActionListener listener,
            DragStartListener dragStartListener) {
        this.categories = categories;
        this.activeCategoryIds = activeCategoryIds == null ? allCategoryIds(categories) : new HashSet<>(activeCategoryIds);
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
        switch (viewType) {
            case TYPE_HEADER:
                View headerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_category_manager, parent, false);
                return new HeaderViewHolder(headerView);
            case TYPE_ITEM:
            default:
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_category_item, parent, false);
                return new ItemViewHolder(itemView);
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
            h.wordCountView.setText(h.itemView.getContext().getResources()
                    .getQuantityString(R.plurals.word_count_format, category.wordCount, category.wordCount));
            h.activeCheckbox.setOnCheckedChangeListener(null);
            h.activeCheckbox.setChecked(activeCategoryIds.contains(category.id));
            h.activeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    activeCategoryIds.add(category.id);
                } else {
                    activeCategoryIds.remove(category.id);
                }
            });

            // Reorder handle logic
            h.reorderHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.performClick();
                    dragStartListener.onDragStarted(h);
                }
                return false;
            });

            // Delete logic (hide for the default category)
            if (category.isDefault) {
                h.deleteButton.setVisibility(View.GONE);
                h.reorderHandle.setVisibility(View.INVISIBLE);
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
        
        // Prevent moving the default category, usually first item at pos 1.
        if (categories.get(fromPosition - 1).isDefault || categories.get(toPosition - 1).isDefault) {
            return;
        }

        Collections.swap(categories, fromPosition - 1, toPosition - 1);
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Set<Long> getActiveCategoryIds() {
        return new HashSet<>(activeCategoryIds);
    }

    private static Set<Long> allCategoryIds(List<Category> categories) {
        Set<Long> ids = new HashSet<>();
        for (Category category : categories) {
            ids.add(category.id);
        }
        return ids;
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
        TextView wordCountView;
        CheckBox activeCheckbox;
        View deleteButton;
        View reorderHandle;

        ItemViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.category_name);
            wordCountView = itemView.findViewById(R.id.word_count);
            activeCheckbox = itemView.findViewById(R.id.category_active_checkbox);
            deleteButton = itemView.findViewById(R.id.delete_category);
            reorderHandle = itemView.findViewById(R.id.reorder_handle);
        }
    }
}
