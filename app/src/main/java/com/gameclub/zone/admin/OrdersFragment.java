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
import com.gameclub.zone.admin.adapter.OrderAdapter;
import com.gameclub.zone.admin.databinding.FragmentOrdersBinding;
import com.gameclub.zone.admin.model.TopUpOrder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment implements OrderAdapter.OrderActionListener {

    private FragmentOrdersBinding binding;
    private OrderAdapter adapter;
    private final List<TopUpOrder> allOrders = new ArrayList<>();
    private String currentTab = "pending";
    private ValueEventListener ordersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackOrders.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new OrderAdapter(requireContext(), this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);

        setupTabs();

        binding.swipeRefreshOrders.setOnRefreshListener(this::loadOrders);
        loadOrders();
    }

    private void setupTabs() {
        binding.tabOrderPending.setOnClickListener(v -> switchTab("pending"));
        binding.tabOrderApproved.setOnClickListener(v -> switchTab("approved"));
        binding.tabOrderRejected.setOnClickListener(v -> switchTab("rejected"));
    }

    private void switchTab(String tab) {
        currentTab = tab;
        binding.tabOrderPending.setBackgroundResource("pending".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabOrderPending.setTextColor(requireContext().getColor("pending".equals(tab) ? R.color.text_main : R.color.text_muted));

        binding.tabOrderApproved.setBackgroundResource("approved".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabOrderApproved.setTextColor(requireContext().getColor("approved".equals(tab) ? R.color.text_main : R.color.text_muted));

        binding.tabOrderRejected.setBackgroundResource("rejected".equals(tab) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        binding.tabOrderRejected.setTextColor(requireContext().getColor("rejected".equals(tab) ? R.color.text_main : R.color.text_muted));

        filterAndDisplay();
    }

    private void loadOrders() {
        binding.swipeRefreshOrders.setRefreshing(true);
        if (ordersListener != null) {
            FirebaseManager.getTopUpOrdersRef().removeEventListener(ordersListener);
        }

        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrders.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TopUpOrder order = child.getValue(TopUpOrder.class);
                    if (order != null) {
                        order.setId(child.getKey());
                        allOrders.add(order);
                    }
                }
                if (binding != null) {
                    binding.swipeRefreshOrders.setRefreshing(false);
                    filterAndDisplay();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.swipeRefreshOrders.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getTopUpOrdersRef().addValueEventListener(ordersListener);
    }

    private void filterAndDisplay() {
        List<TopUpOrder> filtered = new ArrayList<>();
        for (TopUpOrder order : allOrders) {
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "pending";
            if (currentTab.equalsIgnoreCase(status)) {
                filtered.add(order);
            }
        }

        adapter.setOrders(filtered);
        binding.tvEmptyOrders.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAccept(TopUpOrder order) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_ORDERS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Orders", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Accept Order?")
                .setMessage("Confirm order for " + order.getPackageName() + " for Player UID " + order.getUid() + "?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    FirebaseManager.getTopUpOrdersRef().child(order.getId()).child("status").setValue("approved")
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Order accepted!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onReject(TopUpOrder order) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_ORDERS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Orders", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Reject and Refund?")
                .setMessage("Reject order and refund ৳" + order.getAmount() + " back to user's winning balance?")
                .setPositiveButton("Reject & Refund", (dialog, which) -> {
                    FirebaseManager.getTopUpOrdersRef().child(order.getId()).child("status").setValue("rejected");

                    if (order.getUserId() != null) {
                        FirebaseManager.getUsersRef().child(order.getUserId()).child("winning").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                long current = 0;
                                if (snapshot.exists() && snapshot.getValue(Long.class) != null) {
                                    current = snapshot.getValue(Long.class);
                                }
                                FirebaseManager.getUsersRef().child(order.getUserId()).child("winning").setValue(current + order.getAmount());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    Toast.makeText(getContext(), "Order rejected and refunded!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) {
            FirebaseManager.getTopUpOrdersRef().removeEventListener(ordersListener);
        }
        binding = null;
    }
}
