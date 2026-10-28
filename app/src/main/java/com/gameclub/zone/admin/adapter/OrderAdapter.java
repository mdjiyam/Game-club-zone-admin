package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemOrderBinding;
import com.gameclub.zone.admin.model.TopUpOrder;
import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OrderActionListener {
        void onAccept(TopUpOrder order);
        void onReject(TopUpOrder order);
    }

    private final Context context;
    private final List<TopUpOrder> list = new ArrayList<>();
    private final OrderActionListener listener;

    public OrderAdapter(Context context, OrderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setOrders(List<TopUpOrder> orders) {
        this.list.clear();
        if (orders != null) {
            this.list.addAll(orders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        TopUpOrder order = list.get(position);
        ItemOrderBinding binding = holder.binding;

        binding.tvOrderPackageName.setText(order.getPackageName());
        binding.tvOrderUserName.setText(order.getUserName());
        binding.tvOrderPlayerUid.setText(order.getUid());
        binding.tvOrderAmount.setText("৳" + order.getAmount());
        binding.tvOrderTime.setText(order.getTimestamp() != null ? order.getTimestamp() : "N/A");

        String status = order.getStatus();
        if ("approved".equalsIgnoreCase(status)) {
            binding.tvOrderStatusTag.setText("APPROVED");
            binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.success));
            binding.tvOrderStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2600D4AA")));
            binding.layoutOrderActions.setVisibility(View.GONE);
        } else if ("rejected".equalsIgnoreCase(status)) {
            binding.tvOrderStatusTag.setText("REJECTED");
            binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.danger));
            binding.tvOrderStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FF5E7A")));
            binding.layoutOrderActions.setVisibility(View.GONE);
        } else {
            binding.tvOrderStatusTag.setText("PENDING");
            binding.tvOrderStatusTag.setTextColor(ContextCompat.getColor(context, R.color.warning));
            binding.tvOrderStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FFD93D")));
            binding.layoutOrderActions.setVisibility(View.VISIBLE);

            binding.btnApproveOrder.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(order);
            });
            binding.btnRejectOrder.setOnClickListener(v -> {
                if (listener != null) listener.onReject(order);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final ItemOrderBinding binding;

        OrderViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
