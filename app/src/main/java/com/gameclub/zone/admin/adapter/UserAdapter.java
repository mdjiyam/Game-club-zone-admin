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
import com.gameclub.zone.admin.databinding.ItemUserBinding;
import com.gameclub.zone.admin.model.User;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface UserActionListener {
        void onAdjustBalance(User user);
        void onToggleBan(User user);
        void onSoftDelete(User user);
        void onRestore(User user);
        void onHardDelete(User user);
    }

    private final Context context;
    private final List<User> userList = new ArrayList<>();
    private final UserActionListener listener;
    private String currentTab = "active";

    public UserAdapter(Context context, UserActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setUsers(List<User> list, String tab) {
        this.currentTab = tab;
        this.userList.clear();
        if (list != null) {
            this.userList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(LayoutInflater.from(context), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        ItemUserBinding binding = holder.binding;

        binding.tvUserName.setText(user.getUsername());
        binding.tvUserEmail.setText(user.getEmail());
        binding.tvUserPhone.setText(user.getDisplayPhone());
        binding.tvUserUid.setText(user.getUid() != null ? user.getUid() : "N/A");
        binding.tvUserDeposit.setText("৳" + user.getDeposit());
        binding.tvUserWinning.setText("৳" + user.getWinning());

        String status = user.getStatus();
        if ("deleted".equalsIgnoreCase(status)) {
            binding.tvUserStatusTag.setText("DELETED (TRASH)");
            binding.tvUserStatusTag.setTextColor(Color.parseColor("#9E9E9E"));
            binding.tvUserStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#269E9E9E")));

            binding.layoutActiveActions.setVisibility(View.GONE);
            binding.layoutDeletedActions.setVisibility(View.VISIBLE);

            binding.btnRestoreUser.setOnClickListener(v -> {
                if (listener != null) listener.onRestore(user);
            });
            binding.btnHardDeleteUser.setOnClickListener(v -> {
                if (listener != null) listener.onHardDelete(user);
            });
        } else {
            binding.layoutActiveActions.setVisibility(View.VISIBLE);
            binding.layoutDeletedActions.setVisibility(View.GONE);

            if ("banned".equalsIgnoreCase(status)) {
                binding.tvUserStatusTag.setText("BANNED");
                binding.tvUserStatusTag.setTextColor(ContextCompat.getColor(context, R.color.danger));
                binding.tvUserStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FF5E7A")));
                binding.btnToggleBan.setText("UNBAN");
                binding.btnToggleBan.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.btnToggleBan.setBackgroundResource(R.drawable.bg_button_success);
            } else {
                binding.tvUserStatusTag.setText("ACTIVE");
                binding.tvUserStatusTag.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.tvUserStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2600D4AA")));
                binding.btnToggleBan.setText("BAN");
                binding.btnToggleBan.setTextColor(ContextCompat.getColor(context, R.color.warning));
                binding.btnToggleBan.setBackgroundResource(R.drawable.bg_button_warning);
            }

            binding.btnEditBalance.setOnClickListener(v -> {
                if (listener != null) listener.onAdjustBalance(user);
            });
            binding.btnToggleBan.setOnClickListener(v -> {
                if (listener != null) listener.onToggleBan(user);
            });
            binding.btnSoftDelete.setOnClickListener(v -> {
                if (listener != null) listener.onSoftDelete(user);
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemUserBinding binding;

        UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
