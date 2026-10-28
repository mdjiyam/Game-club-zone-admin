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
import com.gameclub.zone.admin.Constants;
import com.gameclub.zone.admin.R;
import com.gameclub.zone.admin.databinding.ItemAdminBinding;
import com.gameclub.zone.admin.model.AdminUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    public interface AdminActionListener {
        void onEditPermissions(AdminUser admin);
        void onToggleSuspend(AdminUser admin);
        void onRemoveAdmin(AdminUser admin);
    }

    private final Context context;
    private final List<AdminUser> admins = new ArrayList<>();
    private final AdminActionListener listener;

    public AdminAdapter(Context context, AdminActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setAdmins(List<AdminUser> list) {
        this.admins.clear();
        if (list != null) {
            this.admins.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminBinding binding = ItemAdminBinding.inflate(LayoutInflater.from(context), parent, false);
        return new AdminViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        AdminUser admin = admins.get(position);
        ItemAdminBinding binding = holder.binding;

        binding.tvAdminItemName.setText(admin.getName());
        binding.tvAdminItemUid.setText("UID: " + admin.getUid());

        boolean isOwner = "owner".equalsIgnoreCase(admin.getRole()) || Constants.OWNER_UID.equals(admin.getUid());
        if (isOwner) {
            binding.tvAdminRoleTag.setText("OWNER");
            binding.tvAdminRoleTag.setTextColor(ContextCompat.getColor(context, R.color.warning));
            binding.tvAdminRoleTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FFD93D")));

            binding.tvAdminStatusTag.setText("ACTIVE");
            binding.tvAdminStatusTag.setTextColor(ContextCompat.getColor(context, R.color.success));
            binding.tvAdminStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2600D4AA")));

            binding.tvAdminPermissionsSummary.setText("👑 Full Owner Access (All permissions)");
            binding.layoutAdminActions.setVisibility(View.GONE);
        } else {
            binding.tvAdminRoleTag.setText("ADMIN");
            binding.tvAdminRoleTag.setTextColor(ContextCompat.getColor(context, R.color.info));
            binding.tvAdminRoleTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#264FC3F7")));

            boolean isActive = !"suspended".equalsIgnoreCase(admin.getStatus());
            if (isActive) {
                binding.tvAdminStatusTag.setText("ACTIVE");
                binding.tvAdminStatusTag.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.tvAdminStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2600D4AA")));
                binding.btnToggleAdminSuspend.setText("SUSPEND");
                binding.btnToggleAdminSuspend.setTextColor(ContextCompat.getColor(context, R.color.warning));
                binding.btnToggleAdminSuspend.setBackgroundResource(R.drawable.bg_button_warning);
            } else {
                binding.tvAdminStatusTag.setText("SUSPENDED");
                binding.tvAdminStatusTag.setTextColor(ContextCompat.getColor(context, R.color.danger));
                binding.tvAdminStatusTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FF5E7A")));
                binding.btnToggleAdminSuspend.setText("ACTIVATE");
                binding.btnToggleAdminSuspend.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.btnToggleAdminSuspend.setBackgroundResource(R.drawable.bg_button_success);
            }

            // Permissions summary
            Map<String, Boolean> perms = admin.getPermissions();
            StringBuilder sb = new StringBuilder("Permissions: ");
            int count = 0;
            for (Map.Entry<String, String> entry : Constants.ALL_PERMISSIONS.entrySet()) {
                if (Boolean.TRUE.equals(perms.get(entry.getKey()))) {
                    if (count > 0) sb.append(", ");
                    String permTitle = entry.getValue();
                    if (Constants.PERM_SEND_NOTIFICATIONS.equals(entry.getKey())) {
                        permTitle = "Push Notifications";
                    }
                    sb.append(permTitle);
                    if (Constants.PERM_MANAGE_MATCHES.equals(entry.getKey())) {
                        Map<String, Boolean> cats = admin.getAssignedCategories();
                        if (cats == null || cats.isEmpty()) {
                            cats = admin.getAssignedMatches();
                        }
                        if (cats != null && !cats.isEmpty() && !Boolean.TRUE.equals(cats.get("all"))) {
                            sb.append(" (").append(cats.size()).append(" Categories Assigned)");
                        } else {
                            sb.append(" (All Categories)");
                        }
                    }
                    count++;
                }
            }
            if (count == 0) {
                sb.append("None");
            }
            binding.tvAdminPermissionsSummary.setText(sb.toString());
            binding.layoutAdminActions.setVisibility(View.VISIBLE);

            binding.btnEditAdminPermissions.setOnClickListener(v -> {
                if (listener != null) listener.onEditPermissions(admin);
            });
            binding.btnToggleAdminSuspend.setOnClickListener(v -> {
                if (listener != null) listener.onToggleSuspend(admin);
            });
            binding.btnRemoveAdmin.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveAdmin(admin);
            });
        }
    }

    @Override
    public int getItemCount() {
        return admins.size();
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminBinding binding;

        AdminViewHolder(ItemAdminBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
