package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemSliderBinding;
import com.gameclub.zone.admin.model.BannerModel;
import java.util.ArrayList;
import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

    public interface SliderActionListener {
        void onDeleteBanner(BannerModel banner);
    }

    private final Context context;
    private final List<BannerModel> banners = new ArrayList<>();
    private final SliderActionListener listener;

    public SliderAdapter(Context context, SliderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBanners(List<BannerModel> list) {
        this.banners.clear();
        if (list != null) {
            this.banners.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSliderBinding binding = ItemSliderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new SliderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        BannerModel banner = banners.get(position);
        ItemSliderBinding binding = holder.binding;

        String link = banner.getLink();
        binding.tvBannerLink.setText(link != null && !link.isEmpty() ? link : "No action link");

        if (banner.getImg() != null && !banner.getImg().isEmpty()) {
            Glide.with(context)
                    .load(banner.getImg())
                    .placeholder(R.drawable.gc_admin_icon)
                    .error(R.drawable.gc_admin_icon)
                    .centerCrop()
                    .into(binding.ivBannerPreview);
        } else {
            binding.ivBannerPreview.setImageResource(R.drawable.gc_admin_icon);
        }

        binding.btnDeleteBanner.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteBanner(banner);
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        final ItemSliderBinding binding;

        SliderViewHolder(ItemSliderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
