package com.gameclub.zone.admin.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemPayoutParticipantBinding;
import com.gameclub.zone.admin.model.Participant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayoutParticipantAdapter extends RecyclerView.Adapter<PayoutParticipantAdapter.PayoutViewHolder> {

    public interface PayoutActionListener {
        void onSavePayout(Participant participant, long newAmount);
    }

    private final Context context;
    private final List<Participant> participants = new ArrayList<>();
    private final Map<String, Long> editedAmounts = new HashMap<>();
    private final PayoutActionListener listener;

    public PayoutParticipantAdapter(Context context, PayoutActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setParticipants(List<Participant> list) {
        this.participants.clear();
        this.editedAmounts.clear();
        if (list != null) {
            this.participants.addAll(list);
            for (Participant p : list) {
                this.editedAmounts.put(p.getId(), p.getRewardAmount());
            }
        }
        notifyDataSetChanged();
    }

    public Map<String, Long> getEditedAmounts() {
        return new HashMap<>(editedAmounts);
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    @NonNull
    @Override
    public PayoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPayoutParticipantBinding binding = ItemPayoutParticipantBinding.inflate(LayoutInflater.from(context), parent, false);
        return new PayoutViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PayoutViewHolder holder, int position) {
        Participant participant = participants.get(position);
        ItemPayoutParticipantBinding binding = holder.binding;

        binding.tvPayoutRank.setText("#" + (position + 1));
        binding.tvPayoutIgn.setText(participant.getIgn());
        binding.tvPayoutUid.setText("UID: " + (participant.getJoinedBy() != null ? participant.getJoinedBy() : participant.getId()));

        long currentReward = participant.getRewardAmount();
        if (currentReward > 0) {
            binding.tvPayoutStatus.setText("Paid: ৳" + currentReward);
            binding.tvPayoutStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
        } else {
            binding.tvPayoutStatus.setText("Not Paid");
            binding.tvPayoutStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
        }

        long enteredAmount = editedAmounts.getOrDefault(participant.getId(), currentReward);
        binding.etPayoutAmount.setText(enteredAmount > 0 ? String.valueOf(enteredAmount) : "");

        binding.etPayoutAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String str = s.toString().trim();
                    long val = str.isEmpty() ? 0 : Long.parseLong(str);
                    editedAmounts.put(participant.getId(), val);
                } catch (Exception ignored) {
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnSaveSinglePayout.setOnClickListener(v -> {
            long amt = editedAmounts.getOrDefault(participant.getId(), 0L);
            if (listener != null) {
                listener.onSavePayout(participant, amt);
            }
        });
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class PayoutViewHolder extends RecyclerView.ViewHolder {
        final ItemPayoutParticipantBinding binding;

        PayoutViewHolder(ItemPayoutParticipantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
