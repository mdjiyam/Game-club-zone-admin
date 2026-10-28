package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemCategoryGridBinding;
import com.gameclub.zone.admin.model.Category;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryGridAdapter extends RecyclerView.Adapter<CategoryGridAdapter.CatGridViewHolder> {

    public interface CategorySelectListener {
        void onSelectCategory(Category category);
    }

    private final Context context;
    private final List<Category> categories = new ArrayList<>();
    private final Map<String, Integer> activeMatchCounts = new HashMap<>();
    private final CategorySelectListener listener;

    public CategoryGridAdapter(Context context, CategorySelectListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Category> list, Map<String, Integer> matchCounts) {
        this.categories.clear();
        if (list != null) {
            this.categories.addAll(list);
        }
        this.activeMatchCounts.clear();
        if (matchCounts != null) {
            this.activeMatchCounts.putAll(matchCounts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CatGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryGridBinding binding = ItemCategoryGridBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CatGridViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CatGridViewHolder holder, int position) {
        Category category = categories.get(position);
        ItemCategoryGridBinding binding = holder.binding;

        binding.tvCatGridName.setText(category.getName());
        int count = activeMatchCounts.getOrDefault(category.getId(), 0);
        binding.tvCatGridCount.setText(count + " Active Matches");

        if (category.getImg() != null && !category.getImg().isEmpty()) {
            Glide.with(context)
                    .load(category.getImg())
                    .placeholder(R.drawable.gc_admin_icon)
                    .error(R.drawable.gc_admin_icon)
                    .centerCrop()
                    .into(binding.ivCatGridImg);
        } else {
            binding.ivCatGridImg.setImageResource(R.drawable.gc_admin_icon);
        }

        binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onSelectCategory(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CatGridViewHolder extends RecyclerView.ViewHolder {
        final ItemCategoryGridBinding binding;

        CatGridViewHolder(ItemCategoryGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
