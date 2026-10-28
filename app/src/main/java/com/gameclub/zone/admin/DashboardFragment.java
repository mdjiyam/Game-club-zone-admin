package com.gameclub.zone.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gameclub.zone.admin.databinding.FragmentDashboardBinding;
import com.gameclub.zone.admin.model.AdminUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ValueEventListener usersListener;
    private ValueEventListener withdrawListener;
    private ValueEventListener ordersListener;
    private ValueEventListener matchesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupPermissionsUI();
        setupClickListeners();
        loadDashboardStats();

        binding.swipeRefreshDashboard.setOnRefreshListener(() -> {
            loadDashboardStats();
            binding.swipeRefreshDashboard.setRefreshing(false);
        });
    }

    private void setupPermissionsUI() {
        boolean isOwner = FirebaseManager.isOwner();

        // Admin Management is Owner Only
        binding.btnModuleAdmins.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // Completely hide modules and cards based on permissions if not owner
        if (!isOwner) {
            updateModuleVisual(binding.btnModuleUsers, Constants.PERM_VIEW_USERS);
            updateModuleVisual(binding.btnModuleMatches, Constants.PERM_MANAGE_MATCHES);
            updateModuleVisual(binding.btnModuleCategories, Constants.PERM_MANAGE_CATEGORIES);
            updateModuleVisual(binding.btnModuleWithdraw, Constants.PERM_MANAGE_WITHDRAWALS);
            updateModuleVisual(binding.btnModuleTopUp, Constants.PERM_MANAGE_TOPUP);
            updateModuleVisual(binding.btnModuleOrders, Constants.PERM_MANAGE_ORDERS);
            updateModuleVisual(binding.btnModuleSettings, Constants.PERM_MANAGE_SETTINGS);
            updateModuleVisual(binding.btnModuleSupport, Constants.PERM_MANAGE_SUPPORT);
            updateModuleVisual(binding.btnModuleUpdate, Constants.PERM_MANAGE_UPDATE);
            updateModuleVisual(binding.btnModuleSliders, Constants.PERM_MANAGE_SLIDERS);
            updateModuleVisual(binding.btnModuleNotify, Constants.PERM_SEND_NOTIFICATIONS);
            updateModuleVisual(binding.btnModulePushNotify, Constants.PERM_SEND_NOTIFICATIONS);

            if (binding.cardStatUsers != null) {
                binding.cardStatUsers.setVisibility(FirebaseManager.hasPermission(Constants.PERM_VIEW_USERS) ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void updateModuleVisual(View view, String permKey) {
        if (view == null) return;
        boolean hasPerm = FirebaseManager.hasPermission(permKey);
        view.setVisibility(hasPerm ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        binding.cardStatUsers.setOnClickListener(v -> checkAndNavigate(Constants.PERM_VIEW_USERS, new UsersFragment()));
        binding.btnModuleUsers.setOnClickListener(v -> checkAndNavigate(Constants.PERM_VIEW_USERS, new UsersFragment()));
        binding.btnModuleMatches.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_MATCHES, new MatchesFragment()));
        binding.btnModuleCategories.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_CATEGORIES, new CategoriesFragment()));
        binding.btnModuleWithdraw.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_WITHDRAWALS, new WithdrawalsFragment()));
        binding.btnModuleTopUp.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_TOPUP, new TopUpFragment()));
        binding.btnModuleOrders.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_ORDERS, new OrdersFragment()));

        binding.btnModuleSettings.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_SETTINGS, new SettingsFragment()));
        binding.btnModuleSupport.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_SUPPORT, new SupportFragment()));
        binding.btnModuleUpdate.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_UPDATE, new UpdateFragment()));
        binding.btnModuleSliders.setOnClickListener(v -> checkAndNavigate(Constants.PERM_MANAGE_SLIDERS, new SlidersFragment()));
        binding.btnModuleNotify.setOnClickListener(v -> checkAndNavigate(Constants.PERM_SEND_NOTIFICATIONS, new NotificationsFragment()));
        binding.btnModulePushNotify.setOnClickListener(v -> checkAndNavigate(Constants.PERM_SEND_NOTIFICATIONS, new SendPushNotificationFragment()));

        binding.btnModuleAdmins.setOnClickListener(v -> {
            if (FirebaseManager.isOwner()) {
                activity.showFragment(new AdminManagementFragment(), true);
            } else {
                Toast.makeText(getContext(), "Owner permission required", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndNavigate(String permKey, Fragment fragment) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        if (FirebaseManager.hasPermission(permKey)) {
            activity.showFragment(fragment, true);
        } else {
            Toast.makeText(getContext(), "Access Denied: You do not have permission for this section.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDashboardStats() {
        // Total Users - check both "users" and "Users"
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                long count1 = snapshot.getChildrenCount();
                FirebaseManager.getDatabase().getReference("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot upperSnapshot) {
                        if (binding == null) return;
                        long total = Math.max(count1, upperSnapshot.getChildrenCount());
                        if (count1 > 0 && upperSnapshot.getChildrenCount() > 0) {
                            java.util.Set<String> uids = new java.util.HashSet<>();
                            for (DataSnapshot c : snapshot.getChildren()) uids.add(c.getKey());
                            for (DataSnapshot c : upperSnapshot.getChildren()) uids.add(c.getKey());
                            total = uids.size();
                        }
                        binding.tvTotalUsersCount.setText(String.valueOf(total));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding != null) binding.tvTotalUsersCount.setText(String.valueOf(count1));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    FirebaseManager.getDatabase().getReference("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot upperSnapshot) {
                            if (binding != null) binding.tvTotalUsersCount.setText(String.valueOf(upperSnapshot.getChildrenCount()));
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
        };
        FirebaseManager.getUsersRef().addValueEventListener(usersListener);

        // Active Matches Count
        matchesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                int activeCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.child("status").getValue(String.class);
                    if (status == null || "Upcoming".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status)) {
                        activeCount++;
                    }
                }
                binding.tvActiveMatchesCount.setText(String.valueOf(activeCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getMatchesRef().addValueEventListener(matchesListener);

        // Pending Withdrawals Badge
        withdrawListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                int pendingCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.child("status").getValue(String.class);
                    if ("pending".equalsIgnoreCase(status) || status == null) {
                        pendingCount++;
                    }
                }
                if (pendingCount > 0) {
                    binding.tvWithdrawBadge.setVisibility(View.VISIBLE);
                    binding.tvWithdrawBadge.setText(String.valueOf(pendingCount));
                } else {
                    binding.tvWithdrawBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getWithdrawRequestsRef().addValueEventListener(withdrawListener);

        // Pending Orders Badge
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                int pendingCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.child("status").getValue(String.class);
                    if ("pending".equalsIgnoreCase(status) || status == null) {
                        pendingCount++;
                    }
                }
                if (pendingCount > 0) {
                    binding.tvOrdersBadge.setVisibility(View.VISIBLE);
                    binding.tvOrdersBadge.setText(String.valueOf(pendingCount));
                } else {
                    binding.tvOrdersBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getTopUpOrdersRef().addValueEventListener(ordersListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersListener != null) FirebaseManager.getUsersRef().removeEventListener(usersListener);
        if (matchesListener != null) FirebaseManager.getMatchesRef().removeEventListener(matchesListener);
        if (withdrawListener != null) FirebaseManager.getWithdrawRequestsRef().removeEventListener(withdrawListener);
        if (ordersListener != null) FirebaseManager.getTopUpOrdersRef().removeEventListener(ordersListener);
        binding = null;
    }
}
