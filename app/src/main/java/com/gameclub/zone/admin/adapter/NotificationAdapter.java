package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gameclub.zone.admin.databinding.ItemNotificationBinding;
import com.gameclub.zone.admin.model.NotificationModel;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NoticeViewHolder> {

    public interface NotificationActionListener {
        void onDeactivateNotice(NotificationModel notice);
    }

    private final Context context;
    private final List<NotificationModel> notices = new ArrayList<>();
    private final NotificationActionListener listener;

    public NotificationAdapter(Context context, NotificationActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setNotices(List<NotificationModel> list) {
        this.notices.clear();
        if (list != null) {
            this.notices.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(LayoutInflater.from(context), parent, false);
        return new NoticeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        NotificationModel notice = notices.get(position);
        ItemNotificationBinding binding = holder.binding;

        binding.tvNoticeTitle.setText(notice.getTitle());
        binding.tvNoticeBody.setText(notice.getBody());

        binding.btnDeactivateNotice.setOnClickListener(v -> {
            if (listener != null) listener.onDeactivateNotice(notice);
        });
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }

    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        final ItemNotificationBinding binding;

        NoticeViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
