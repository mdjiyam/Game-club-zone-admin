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
import com.gameclub.zone.admin.databinding.ItemMatchBinding;
import com.gameclub.zone.admin.model.Match;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    public interface MatchActionListener {
        void onManageMatch(Match match);
        void onPayouts(Match match);
        void onDeleteMatch(Match match);
    }

    private final Context context;
    private final List<Match> matches = new ArrayList<>();
    private final Map<String, Integer> participantCounts = new HashMap<>();
    private final MatchActionListener listener;

    public MatchAdapter(Context context, MatchActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setMatches(List<Match> list, Map<String, Integer> counts) {
        this.matches.clear();
        if (list != null) {
            this.matches.addAll(list);
        }
        this.participantCounts.clear();
        if (counts != null) {
            this.participantCounts.putAll(counts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMatchBinding binding = ItemMatchBinding.inflate(LayoutInflater.from(context), parent, false);
        return new MatchViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matches.get(position);
        ItemMatchBinding binding = holder.binding;

        binding.tvMatchTitle.setText(match.getTitle());
        binding.tvMatchTime.setText(match.getTime() != null && !match.getTime().isEmpty() ? match.getTime() : "N/A");
        binding.tvMatchMapType.setText(match.getMap() + " - " + match.getType());
        binding.tvMatchEntryPrize.setText("৳" + match.getEntry() + " / ৳" + match.getTotal_prize() + " (Kill: ৳" + match.getPer_kill() + ")");

        int joinedCount = participantCounts.getOrDefault(match.getDbKey(), match.getJoined());
        binding.tvMatchJoined.setText(joinedCount + "/" + match.getTotal());

        String status = match.getStatus();
        if ("Finished".equalsIgnoreCase(status)) {
            binding.tvMatchStatusTag.setText("FINISHED");
            binding.tvMatchStatusTag.setTextColor(ContextCompat.getColor(context, R.color.success));
            binding.tvMatchStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2600D4AA")));
        } else if ("Ongoing".equalsIgnoreCase(status)) {
            binding.tvMatchStatusTag.setText("ONGOING");
            binding.tvMatchStatusTag.setTextColor(ContextCompat.getColor(context, R.color.info));
            binding.tvMatchStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#264FC3F7")));
        } else {
            binding.tvMatchStatusTag.setText("UPCOMING");
            binding.tvMatchStatusTag.setTextColor(ContextCompat.getColor(context, R.color.warning));
            binding.tvMatchStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FFD93D")));
        }

        if (match.getWinner() != null && !match.getWinner().trim().isEmpty()) {
            binding.layoutMatchWinner.setVisibility(View.VISIBLE);
            binding.tvMatchWinner.setText(match.getWinner());
        } else {
            binding.layoutMatchWinner.setVisibility(View.GONE);
        }

        String adminName = match.getAdminName();
        if (adminName == null || adminName.trim().isEmpty() || "All Admins".equalsIgnoreCase(adminName)) {
            binding.tvMatchAssignedAdmin.setText("All Admins");
            binding.tvMatchAssignedAdmin.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
            binding.tvMatchAssignedAdmin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#20FFFFFF")));
        } else {
            binding.tvMatchAssignedAdmin.setText("👤 " + adminName);
            binding.tvMatchAssignedAdmin.setTextColor(ContextCompat.getColor(context, R.color.primary));
            binding.tvMatchAssignedAdmin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#266366F1")));
        }

        binding.btnPayoutMatch.setText("PAYOUTS (" + joinedCount + ")");

        binding.btnEditMatch.setOnClickListener(v -> {
            if (listener != null) listener.onManageMatch(match);
        });

        binding.btnPayoutMatch.setOnClickListener(v -> {
            if (listener != null) listener.onPayouts(match);
        });

        binding.btnDeleteMatch.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteMatch(match);
        });
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        final ItemMatchBinding binding;

        MatchViewHolder(ItemMatchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
