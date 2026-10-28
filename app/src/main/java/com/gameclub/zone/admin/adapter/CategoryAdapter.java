package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemCategoryBinding;
import com.gameclub.zone.admin.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface CategoryActionListener {
        void onEditCategory(Category category);
        void onDeleteCategory(Category category);
    }

    private final Context context;
    private final List<Category> list = new ArrayList<>();
    private final CategoryActionListener listener;

    public CategoryAdapter(Context context, CategoryActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.list.clear();
        if (categories != null) {
            this.list.addAll(categories);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = list.get(position);
        ItemCategoryBinding binding = holder.binding;

        binding.tvCategoryName.setText(category.getName());
        binding.tvCategoryId.setText("ID: " + (category.getId() != null ? category.getId() : ""));

        if (category.getImg() != null && !category.getImg().isEmpty()) {
            Glide.with(context)
                    .load(category.getImg())
                    .placeholder(R.drawable.gc_admin_icon)
                    .error(R.drawable.gc_admin_icon)
                    .centerCrop()
                    .into(binding.ivCategoryThumb);
        } else {
            binding.ivCategoryThumb.setImageResource(R.drawable.gc_admin_icon);
        }

        binding.btnEditCategory.setOnClickListener(v -> {
            if (listener != null) listener.onEditCategory(category);
        });

        binding.btnDeleteCategory.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteCategory(category);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        final ItemCategoryBinding binding;

        CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
