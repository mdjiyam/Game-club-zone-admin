package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gameclub.zone.admin.databinding.ItemTopupPackageBinding;
import com.gameclub.zone.admin.model.TopUpPackage;
import java.util.ArrayList;
import java.util.List;

public class TopUpPackageAdapter extends RecyclerView.Adapter<TopUpPackageAdapter.PackageViewHolder> {

    public interface TopUpPackageActionListener {
        void onEditPackage(TopUpPackage pkg);
        void onDeletePackage(TopUpPackage pkg);
    }

    private final Context context;
    private final List<TopUpPackage> list = new ArrayList<>();
    private final TopUpPackageActionListener listener;

    public TopUpPackageAdapter(Context context, TopUpPackageActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPackages(List<TopUpPackage> packages) {
        this.list.clear();
        if (packages != null) {
            this.list.addAll(packages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTopupPackageBinding binding = ItemTopupPackageBinding.inflate(LayoutInflater.from(context), parent, false);
        return new PackageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        TopUpPackage pkg = list.get(position);
        ItemTopupPackageBinding binding = holder.binding;

        binding.tvPackageName.setText(pkg.getName());
        binding.tvPackagePrice.setText("৳" + pkg.getPrice());

        binding.btnEditPackage.setOnClickListener(v -> {
            if (listener != null) listener.onEditPackage(pkg);
        });

        binding.btnDeletePackage.setOnClickListener(v -> {
            if (listener != null) listener.onDeletePackage(pkg);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class PackageViewHolder extends RecyclerView.ViewHolder {
        final ItemTopupPackageBinding binding;

        PackageViewHolder(ItemTopupPackageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
