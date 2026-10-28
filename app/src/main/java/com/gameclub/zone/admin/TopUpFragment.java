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
import com.gameclub.zone.admin.adapter.TopUpPackageAdapter;
import com.gameclub.zone.admin.databinding.DialogTopupPackageBinding;
import com.gameclub.zone.admin.databinding.FragmentTopupBinding;
import com.gameclub.zone.admin.model.TopUpPackage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopUpFragment extends Fragment implements TopUpPackageAdapter.TopUpPackageActionListener {

    private FragmentTopupBinding binding;
    private TopUpPackageAdapter adapter;
    private final List<TopUpPackage> packages = new ArrayList<>();
    private ValueEventListener packagesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTopupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackTopUp.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new TopUpPackageAdapter(requireContext(), this);
        binding.rvTopUpPackages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTopUpPackages.setAdapter(adapter);

        binding.fabAddTopUpPackage.setOnClickListener(v -> showPackageDialog(null));
        binding.swipeRefreshTopUp.setOnRefreshListener(this::loadPackages);

        loadPackages();
    }

    private void loadPackages() {
        binding.swipeRefreshTopUp.setRefreshing(true);
        if (packagesListener != null) {
            FirebaseManager.getTopUpPackagesRef().removeEventListener(packagesListener);
        }

        packagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                packages.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TopUpPackage pkg = child.getValue(TopUpPackage.class);
                    if (pkg != null) {
                        pkg.setId(child.getKey());
                        packages.add(pkg);
                    }
                }
                if (binding != null) {
                    binding.swipeRefreshTopUp.setRefreshing(false);
                    adapter.setPackages(packages);
                    binding.tvEmptyTopUp.setVisibility(packages.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.swipeRefreshTopUp.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load packages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getTopUpPackagesRef().addValueEventListener(packagesListener);
    }

    private void showPackageDialog(TopUpPackage packageToEdit) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_TOPUP)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Top-Up", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog);
        DialogTopupPackageBinding dialogBinding = DialogTopupPackageBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());
        AlertDialog dialog = builder.create();

        boolean isEdit = packageToEdit != null;
        dialogBinding.tvDialogPackageTitle.setText(isEdit ? "💎 EDIT PACKAGE" : "💎 ADD NEW PACKAGE");

        if (isEdit) {
            dialogBinding.etDialogPackageName.setText(packageToEdit.getName());
            dialogBinding.etDialogPackagePrice.setText(String.valueOf(packageToEdit.getPrice()));
        }

        dialogBinding.btnCancelPackage.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSavePackage.setOnClickListener(v -> {
            String name = dialogBinding.etDialogPackageName.getText().toString().trim();
            String priceStr = dialogBinding.etDialogPackagePrice.getText().toString().trim();

            if (name.isEmpty()) {
                dialogBinding.etDialogPackageName.setError("Name required");
                dialogBinding.etDialogPackageName.requestFocus();
                return;
            }

            long price = 0;
            try {
                price = Long.parseLong(priceStr);
            } catch (Exception e) {
                dialogBinding.etDialogPackagePrice.setError("Valid price required");
                dialogBinding.etDialogPackagePrice.requestFocus();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);

            if (isEdit) {
                FirebaseManager.getTopUpPackagesRef().child(packageToEdit.getId()).updateChildren(data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Package updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                String key = FirebaseManager.getTopUpPackagesRef().push().getKey();
                if (key != null) {
                    FirebaseManager.getTopUpPackagesRef().child(key).setValue(data)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Package added!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });

        dialog.show();
    }

    @Override
    public void onEditPackage(TopUpPackage pkg) {
        showPackageDialog(pkg);
    }

    @Override
    public void onDeletePackage(TopUpPackage pkg) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_TOPUP)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Top-Up", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Delete Package?")
                .setMessage("Are you sure you want to delete \"" + pkg.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseManager.getTopUpPackagesRef().child(pkg.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Package deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (packagesListener != null) {
            FirebaseManager.getTopUpPackagesRef().removeEventListener(packagesListener);
        }
        binding = null;
    }
}
