package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemPushNotificationBinding;
import com.gameclub.zone.admin.model.PushNotificationModel;
import java.util.ArrayList;
import java.util.List;

public class PushNotificationAdapter extends RecyclerView.Adapter<PushNotificationAdapter.PushViewHolder> {

    public interface PushNotificationListener {
        void onDeletePush(PushNotificationModel item);
    }

    private final Context context;
    private final List<PushNotificationModel> items = new ArrayList<>();
    private final PushNotificationListener listener;

    public PushNotificationAdapter(Context context, PushNotificationListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<PushNotificationModel> list) {
        this.items.clear();
        if (list != null) {
            this.items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PushViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPushNotificationBinding binding = ItemPushNotificationBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new PushViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PushViewHolder holder, int position) {
        PushNotificationModel item = items.get(position);
        ItemPushNotificationBinding binding = holder.binding;

        binding.tvPushItemTitle.setText(item.getTitle());
        binding.tvPushItemMessage.setText(item.getMessage());
        binding.tvPushItemDate.setText(item.getFormattedDate());

        // Target badge
        String targetText = "🎯 Target: " + item.getTargetDisplay();
        binding.tvPushItemTarget.setText(targetText);

        // Optional Image URL
        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
            binding.tvPushItemImageUrl.setVisibility(View.VISIBLE);
            binding.tvPushItemImageUrl.setText("🖼️ " + item.getImageUrl().trim());
        } else {
            binding.tvPushItemImageUrl.setVisibility(View.GONE);
        }

        // Status badge: Pending / Sent / Failed
        String status = item.getStatus().toLowerCase();
        if ("sent".equals(status)) {
            binding.tvPushItemStatus.setText("SENT");
            binding.tvPushItemStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
            binding.tvPushItemStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tag_green_bg)));
        } else if ("failed".equals(status)) {
            binding.tvPushItemStatus.setText("FAILED");
            binding.tvPushItemStatus.setTextColor(ContextCompat.getColor(context, R.color.danger));
            binding.tvPushItemStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tag_red_bg)));
        } else {
            // Default is pending
            binding.tvPushItemStatus.setText("PENDING");
            binding.tvPushItemStatus.setTextColor(ContextCompat.getColor(context, R.color.warning));
            binding.tvPushItemStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tag_yellow_bg)));
        }

        binding.btnDeletePush.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeletePush(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PushViewHolder extends RecyclerView.ViewHolder {
        final ItemPushNotificationBinding binding;

        PushViewHolder(ItemPushNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
