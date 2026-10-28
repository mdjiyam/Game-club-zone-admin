package com.gameclub.zone.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.WithdrawAdapter;
import com.gameclub.zone.admin.databinding.FragmentWithdrawalsBinding;
import com.gameclub.zone.admin.model.WithdrawRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class WithdrawalsFragment extends Fragment implements WithdrawAdapter.WithdrawActionListener {

    private FragmentWithdrawalsBinding binding;
    private WithdrawAdapter adapter;
    private final List<WithdrawRequest> allRequests = new ArrayList<>();
    private String currentTab = "pending";
    private ValueEventListener withdrawListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWithdrawalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackWithdrawals.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new WithdrawAdapter(requireContext(), this);
        binding.rvWithdrawals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWithdrawals.setAdapter(adapter);

        setupTabs();

        binding.swipeRefreshWithdrawals.setOnRefreshListener(this::loadWithdrawals);
        loadWithdrawals();
    }

    private void setupTabs() {
        binding.tabWithdrawPending.setOnClickListener(v -> switchTab("pending"));
        binding.tabWithdrawApproved.setOnClickListener(v -> switchTab("approved"));
        binding.tabWithdrawRejected.setOnClickListener(v -> switchTab("rejected"));
    }

    private void switchTab(String tab) {
        currentTab = tab;
        binding.tabWithdrawPending.setBackgroundResource("pending".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabWithdrawPending.setTextColor(requireContext().getColor("pending".equals(tab) ? R.color.text_main : R.color.text_muted));

        binding.tabWithdrawApproved.setBackgroundResource("approved".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabWithdrawApproved.setTextColor(requireContext().getColor("approved".equals(tab) ? R.color.text_main : R.color.text_muted));

        binding.tabWithdrawRejected.setBackgroundResource("rejected".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabWithdrawRejected.setTextColor(requireContext().getColor("rejected".equals(tab) ? R.color.text_main : R.color.text_muted));

        filterAndDisplay();
    }

    private void loadWithdrawals() {
        binding.swipeRefreshWithdrawals.setRefreshing(true);
        if (withdrawListener != null) {
            FirebaseManager.getWithdrawRequestsRef().removeEventListener(withdrawListener);
        }

        withdrawListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRequests.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    WithdrawRequest req = child.getValue(WithdrawRequest.class);
                    if (req != null) {
                        req.setId(child.getKey());
                        allRequests.add(req);
                    }
                }
                if (binding != null) {
                    binding.swipeRefreshWithdrawals.setRefreshing(false);
                    filterAndDisplay();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.swipeRefreshWithdrawals.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load withdrawals: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getWithdrawRequestsRef().addValueEventListener(withdrawListener);
    }

    private void filterAndDisplay() {
        List<WithdrawRequest> filtered = new ArrayList<>();
        for (WithdrawRequest req : allRequests) {
            String status = req.getStatus() != null ? req.getStatus().toLowerCase() : "pending";
            if (currentTab.equalsIgnoreCase(status)) {
                filtered.add(req);
            }
        }

        adapter.setRequests(filtered);
        binding.tvEmptyWithdrawals.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onApprove(WithdrawRequest req) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_WITHDRAWALS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Withdrawals", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Approve Withdrawal?")
                .setMessage("Confirm payout of ৳" + req.getAmount() + " to " + req.getMobileNumber() + " (" + req.getMethod() + ")?")
                .setPositiveButton("Approve", (dialog, which) -> {
                    FirebaseManager.getWithdrawRequestsRef().child(req.getId()).child("status").setValue("approved")
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Withdrawal approved!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onReject(WithdrawRequest req) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_WITHDRAWALS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Withdrawals", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Reject and Refund?")
                .setMessage("Reject request and refund ৳" + req.getAmount() + " back to user's winning balance?")
                .setPositiveButton("Reject & Refund", (dialog, which) -> {
                    // Update request status
                    FirebaseManager.getWithdrawRequestsRef().child(req.getId()).child("status").setValue("rejected");

                    // Refund to user winning
                    if (req.getUserId() != null) {
                        FirebaseManager.getUsersRef().child(req.getUserId()).child("winning").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                long current = 0;
                                if (snapshot.exists() && snapshot.getValue(Long.class) != null) {
                                    current = snapshot.getValue(Long.class);
                                }
                                FirebaseManager.getUsersRef().child(req.getUserId()).child("winning").setValue(current + req.getAmount());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    Toast.makeText(getContext(), "Withdrawal rejected and refunded!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (withdrawListener != null) {
            FirebaseManager.getWithdrawRequestsRef().removeEventListener(withdrawListener);
        }
        binding = null;
    }
}
