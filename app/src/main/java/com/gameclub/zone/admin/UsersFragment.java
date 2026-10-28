package com.gameclub.zone.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.UserAdapter;
import com.gameclub.zone.admin.databinding.DialogAdjustBalanceBinding;
import com.gameclub.zone.admin.databinding.FragmentUsersBinding;
import com.gameclub.zone.admin.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersFragment extends Fragment implements UserAdapter.UserActionListener {

    private FragmentUsersBinding binding;
    private UserAdapter adapter;
    private final List<User> allUsers = new ArrayList<>();
    private String currentTab = "active"; // "active", "banned", "deleted"
    private String searchQuery = "";
    private ValueEventListener usersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackUsers.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new UserAdapter(requireContext(), this);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUsers.setAdapter(adapter);

        setupTabs();
        setupSearch();

        binding.swipeRefreshUsers.setOnRefreshListener(this::loadUsers);
        loadUsers();
    }

    private void setupTabs() {
        binding.tabUserActive.setOnClickListener(v -> switchTab("active"));
        binding.tabUserBanned.setOnClickListener(v -> switchTab("banned"));
        binding.tabUserDeleted.setOnClickListener(v -> switchTab("deleted"));
    }

    private void switchTab(String tab) {
        currentTab = tab;
        binding.tabUserActive.setBackgroundResource("active".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabUserActive.setTextColor(requireContext().getColor("active".equals(tab) ? R.color.text_main : R.color.text_muted));

        binding.tabUserBanned.setBackgroundResource("banned".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabUserBanned.setTextColor(requireContext().getColor("banned".equals(tab) ? R.color.text_main : R.color.text_muted));

        binding.tabUserDeleted.setBackgroundResource("deleted".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabUserDeleted.setTextColor(requireContext().getColor("deleted".equals(tab) ? R.color.text_main : R.color.text_muted));

        filterAndDisplay();
    }

    private void setupSearch() {
        binding.etUserSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                filterAndDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        binding.swipeRefreshUsers.setRefreshing(true);
        if (usersListener != null) {
            FirebaseManager.getUsersRef().removeEventListener(usersListener);
        }

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear();
                Map<String, User> userMap = new HashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        User user = User.fromSnapshot(child);
                        if (user != null && user.getUid() != null) {
                            userMap.put(user.getUid(), user);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // If lowercase users has items or if we should also check "Users"
                FirebaseManager.getDatabase().getReference("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot upperSnapshot) {
                        for (DataSnapshot child : upperSnapshot.getChildren()) {
                            try {
                                User user = User.fromSnapshot(child);
                                if (user != null && user.getUid() != null && !userMap.containsKey(user.getUid())) {
                                    userMap.put(user.getUid(), user);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        allUsers.clear();
                        allUsers.addAll(userMap.values());
                        if (binding != null) {
                            binding.swipeRefreshUsers.setRefreshing(false);
                            updateUserStats();
                            filterAndDisplay();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        allUsers.clear();
                        allUsers.addAll(userMap.values());
                        if (binding != null) {
                            binding.swipeRefreshUsers.setRefreshing(false);
                            updateUserStats();
                            filterAndDisplay();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.swipeRefreshUsers.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getUsersRef().addValueEventListener(usersListener);
    }

    private void updateUserStats() {
        if (binding == null) return;
        int active = 0;
        int banned = 0;
        int deleted = 0;

        for (User u : allUsers) {
            String status = u.getStatus() != null ? u.getStatus().toLowerCase() : "active";
            if ("banned".equals(status)) {
                banned++;
            } else if ("deleted".equals(status)) {
                deleted++;
            } else {
                active++;
            }
        }

        binding.tvTotalUsersHeader.setText("TOTAL USERS: " + allUsers.size());
        binding.tvUsersStatsDetail.setText("Active: " + active + "  |  Banned: " + banned + "  |  Trash: " + deleted);
        binding.tabUserActive.setText("Active (" + active + ")");
        binding.tabUserBanned.setText("Banned (" + banned + ")");
        binding.tabUserDeleted.setText("Trash (" + deleted + ")");
    }

    private void filterAndDisplay() {
        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            String status = u.getStatus() != null ? u.getStatus().toLowerCase() : "active";

            boolean matchesTab = false;
            if ("active".equals(currentTab)) {
                matchesTab = "active".equals(status) || "".equals(status);
            } else if ("banned".equals(currentTab)) {
                matchesTab = "banned".equals(status);
            } else if ("deleted".equals(currentTab)) {
                matchesTab = "deleted".equals(status);
            }

            if (!matchesTab) continue;

            if (!searchQuery.isEmpty()) {
                boolean matchesSearch = (u.getUsername() != null && u.getUsername().toLowerCase().contains(searchQuery))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(searchQuery))
                        || (u.getDisplayPhone() != null && u.getDisplayPhone().toLowerCase().contains(searchQuery))
                        || (u.getUid() != null && u.getUid().toLowerCase().contains(searchQuery));
                if (!matchesSearch) continue;
            }

            filtered.add(u);
        }

        adapter.setUsers(filtered, currentTab);
        binding.tvEmptyUsers.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAdjustBalance(User user) {
        if (!FirebaseManager.hasPermission(Constants.PERM_EDIT_BALANCES)) {
            Toast.makeText(getContext(), "Permission Denied: Edit Balances", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog);
        DialogAdjustBalanceBinding dialogBinding = DialogAdjustBalanceBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());
        AlertDialog dialog = builder.create();

        dialogBinding.tvDialogBalanceUserName.setText(user.getUsername() + " (" + user.getDisplayPhone() + ")");
        dialogBinding.etDialogDeposit.setText(String.valueOf(user.getDeposit()));
        dialogBinding.etDialogWinning.setText(String.valueOf(user.getWinning()));

        dialogBinding.btnCancelBalance.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSaveBalance.setOnClickListener(v -> {
            try {
                String depStr = dialogBinding.etDialogDeposit.getText().toString().trim();
                String winStr = dialogBinding.etDialogWinning.getText().toString().trim();

                long newDep = depStr.isEmpty() ? 0 : Long.parseLong(depStr);
                long newWin = winStr.isEmpty() ? 0 : Long.parseLong(winStr);

                if (user.getUid() != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("deposit", newDep);
                    updates.put("winning", newWin);
                    FirebaseManager.getUsersRef().child(user.getUid()).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Balances updated!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onToggleBan(User user) {
        if (!FirebaseManager.hasPermission(Constants.PERM_BAN_USERS)) {
            Toast.makeText(getContext(), "Permission Denied: Ban/Unban Users", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isCurrentlyBanned = "banned".equalsIgnoreCase(user.getStatus());
        String newStatus = isCurrentlyBanned ? "active" : "banned";
        String actionMsg = isCurrentlyBanned ? "unbanned" : "banned";

        FirebaseManager.getUsersRef().child(user.getUid()).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "User " + actionMsg + "!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSoftDelete(User user) {
        if (!FirebaseManager.hasPermission(Constants.PERM_DELETE_USERS)) {
            Toast.makeText(getContext(), "Permission Denied: Delete Users", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Move to Trash?")
                .setMessage("Are you sure you want to move " + user.getUsername() + " to Trash?")
                .setPositiveButton("Move to Trash", (dialog, which) -> {
                    FirebaseManager.getUsersRef().child(user.getUid()).child("status").setValue("deleted")
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "User moved to Trash", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRestore(User user) {
        FirebaseManager.getUsersRef().child(user.getUid()).child("status").setValue("active")
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "User restored!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onHardDelete(User user) {
        if (!FirebaseManager.hasPermission(Constants.PERM_DELETE_USERS)) {
            Toast.makeText(getContext(), "Permission Denied: Delete Users", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Permanently Delete?")
                .setMessage("Warning: This cannot be undone. All data for " + user.getUsername() + " will be permanently removed.")
                .setPositiveButton("Permanently Delete", (dialog, which) -> {
                    FirebaseManager.getUsersRef().child(user.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "User permanently deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersListener != null) {
            FirebaseManager.getUsersRef().removeEventListener(usersListener);
        }
        binding = null;
    }
}
