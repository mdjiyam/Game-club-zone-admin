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
import com.gameclub.zone.admin.databinding.ItemWithdrawBinding;
import com.gameclub.zone.admin.model.WithdrawRequest;
import java.util.ArrayList;
import java.util.List;

public class WithdrawAdapter extends RecyclerView.Adapter<WithdrawAdapter.WithdrawViewHolder> {

    public interface WithdrawActionListener {
        void onApprove(WithdrawRequest req);
        void onReject(WithdrawRequest req);
    }

    private final Context context;
    private final List<WithdrawRequest> list = new ArrayList<>();
    private final WithdrawActionListener listener;

    public WithdrawAdapter(Context context, WithdrawActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRequests(List<WithdrawRequest> requests) {
        this.list.clear();
        if (requests != null) {
            this.list.addAll(requests);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WithdrawViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWithdrawBinding binding = ItemWithdrawBinding.inflate(LayoutInflater.from(context), parent, false);
        return new WithdrawViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WithdrawViewHolder holder, int position) {
        WithdrawRequest req = list.get(position);
        ItemWithdrawBinding binding = holder.binding;

        binding.tvWithdrawAmount.setText("৳" + req.getAmount());
        binding.tvWithdrawUser.setText(req.getUserName());
        binding.tvWithdrawMethod.setText(req.getMethod());
        binding.tvWithdrawNumber.setText(req.getMobileNumber());
        binding.tvWithdrawTime.setText(req.getTimestamp() != null ? req.getTimestamp() : "N/A");

        String status = req.getStatus();
        if ("approved".equalsIgnoreCase(status)) {
            binding.tvWithdrawStatusTag.setText("APPROVED");
            binding.tvWithdrawStatusTag.setTextColor(ContextCompat.getColor(context, R.color.success));
            binding.tvWithdrawStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2600D4AA")));
            binding.layoutWithdrawActions.setVisibility(View.GONE);
        } else if ("rejected".equalsIgnoreCase(status)) {
            binding.tvWithdrawStatusTag.setText("REJECTED");
            binding.tvWithdrawStatusTag.setTextColor(ContextCompat.getColor(context, R.color.danger));
            binding.tvWithdrawStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FF5E7A")));
            binding.layoutWithdrawActions.setVisibility(View.GONE);
        } else {
            binding.tvWithdrawStatusTag.setText("PENDING");
            binding.tvWithdrawStatusTag.setTextColor(ContextCompat.getColor(context, R.color.warning));
            binding.tvWithdrawStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FFD93D")));
            binding.layoutWithdrawActions.setVisibility(View.VISIBLE);

            binding.btnApproveWithdraw.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(req);
            });
            binding.btnRejectWithdraw.setOnClickListener(v -> {
                if (listener != null) listener.onReject(req);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class WithdrawViewHolder extends RecyclerView.ViewHolder {
        final ItemWithdrawBinding binding;

        WithdrawViewHolder(ItemWithdrawBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
