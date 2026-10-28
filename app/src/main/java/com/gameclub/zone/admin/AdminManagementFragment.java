package com.gameclub.zone.admin;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.AdminAdapter;
import com.gameclub.zone.admin.databinding.DialogEditAdminPermissionsBinding;
import com.gameclub.zone.admin.databinding.FragmentAdminManagementBinding;
import com.gameclub.zone.admin.model.AdminUser;
import com.gameclub.zone.admin.model.Category;
import com.gameclub.zone.admin.model.Match;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminManagementFragment extends Fragment implements AdminAdapter.AdminActionListener {

    private FragmentAdminManagementBinding binding;
    private AdminAdapter adapter;
    private final List<AdminUser> admins = new ArrayList<>();
    private final Map<String, CheckBox> addAdminCheckBoxes = new HashMap<>();
    private final Map<String, CheckBox> newAdminMatchCheckBoxes = new HashMap<>();
    private ValueEventListener adminsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Security check
        if (!FirebaseManager.isOwner()) {
            Toast.makeText(getContext(), "Owner permission strictly required.", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
            return;
        }

        binding.btnBackAdmins.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new AdminAdapter(requireContext(), this);
        binding.rvAdmins.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdmins.setAdapter(adapter);

        setupPermissionCheckboxes();
        setupNewAdminMatchScope();

        binding.btnGrantAdmin.setOnClickListener(v -> grantNewAdmin());

        loadAdmins();
    }

    private void setupPermissionCheckboxes() {
        binding.layoutAdminPermCheckboxes.removeAllViews();
        addAdminCheckBoxes.clear();

        for (Map.Entry<String, String> entry : Constants.ALL_PERMISSIONS.entrySet()) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(entry.getValue());
            cb.setTextColor(requireContext().getColor(R.color.text_main));
            cb.setButtonTintList(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
            cb.setPadding(8, 8, 8, 8);
            cb.setChecked(true); // default selected
            binding.layoutAdminPermCheckboxes.addView(cb);
            addAdminCheckBoxes.put(entry.getKey(), cb);

            if (Constants.PERM_MANAGE_MATCHES.equals(entry.getKey())) {
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    binding.layoutNewAdminMatchScope.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                });
            }
        }
    }

    private void setupNewAdminMatchScope() {
        binding.rgNewAdminMatchScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbNewAdminMatchScopeSpecific) {
                binding.layoutNewAdminSpecificMatchesContainer.setVisibility(View.VISIBLE);
                loadNewAdminMatches();
            } else {
                binding.layoutNewAdminSpecificMatchesContainer.setVisibility(View.GONE);
            }
        });
    }

    private void loadNewAdminMatches() {
        if (!newAdminMatchCheckBoxes.isEmpty()) return;

        binding.progressBarNewAdminMatches.setVisibility(View.VISIBLE);
        binding.tvNewAdminNoMatches.setVisibility(View.GONE);

        FirebaseManager.getCategoriesRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null || binding == null) return;
                binding.progressBarNewAdminMatches.setVisibility(View.GONE);
                binding.layoutNewAdminMatchCheckboxes.removeAllViews();
                newAdminMatchCheckBoxes.clear();

                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Category cat = child.getValue(Category.class);
                    if (cat != null) {
                        cat.setId(child.getKey());
                        CheckBox cb = new CheckBox(requireContext());
                        String label = cat.getName();
                        cb.setText(label != null && !label.isEmpty() ? label : "Category (" + cat.getId() + ")");
                        cb.setTextColor(requireContext().getColor(R.color.text_main));
                        cb.setButtonTintList(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
                        cb.setPadding(8, 6, 8, 6);
                        binding.layoutNewAdminMatchCheckboxes.addView(cb);
                        newAdminMatchCheckBoxes.put(cat.getId(), cb);
                        count++;
                    }
                }

                if (count == 0) {
                    binding.tvNewAdminNoMatches.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.progressBarNewAdminMatches.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadAdmins() {
        if (adminsListener != null) {
            FirebaseManager.getAdminsRef().removeEventListener(adminsListener);
        }

        adminsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                admins.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AdminUser admin = child.getValue(AdminUser.class);
                    if (admin == null) {
                        admin = new AdminUser();
                    }
                    admin.setUid(child.getKey());

                    if (child.hasChild("name")) {
                        admin.setName(child.child("name").getValue(String.class));
                    }
                    if (child.hasChild("role")) {
                        admin.setRole(child.child("role").getValue(String.class));
                    }
                    if (child.hasChild("status")) {
                        admin.setStatus(child.child("status").getValue(String.class));
                    }
                    if (child.hasChild("permissions")) {
                        Map<String, Boolean> perms = new HashMap<>();
                        for (DataSnapshot pChild : child.child("permissions").getChildren()) {
                            perms.put(pChild.getKey(), Boolean.TRUE.equals(pChild.getValue(Boolean.class)));
                        }
                        admin.setPermissions(perms);
                    }
                    if (child.hasChild("assignedCategories")) {
                        Map<String, Boolean> cats = new HashMap<>();
                        for (DataSnapshot cChild : child.child("assignedCategories").getChildren()) {
                            cats.put(cChild.getKey(), Boolean.TRUE.equals(cChild.getValue(Boolean.class)));
                        }
                        admin.setAssignedCategories(cats);
                    }
                    if (child.hasChild("assignedMatches")) {
                        Map<String, Boolean> matches = new HashMap<>();
                        for (DataSnapshot mChild : child.child("assignedMatches").getChildren()) {
                            matches.put(mChild.getKey(), Boolean.TRUE.equals(mChild.getValue(Boolean.class)));
                        }
                        admin.setAssignedMatches(matches);
                    }

                    if (!admin.isRemoved()) {
                        admins.add(admin);
                    }
                }

                if (binding != null) {
                    adapter.setAdmins(admins);
                    binding.tvEmptyAdmins.setVisibility(admins.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    Toast.makeText(getContext(), "Failed to load admins: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getAdminsRef().addValueEventListener(adminsListener);
    }

    private void grantNewAdmin() {
        String uid = binding.etNewAdminUid.getText().toString().trim();
        String name = binding.etNewAdminName.getText().toString().trim();

        if (uid.isEmpty()) {
            binding.etNewAdminUid.setError("UID required");
            binding.etNewAdminUid.requestFocus();
            return;
        }

        if (name.isEmpty()) {
            binding.etNewAdminName.setError("Name required");
            binding.etNewAdminName.requestFocus();
            return;
        }

        if (Constants.OWNER_UID.equals(uid)) {
            Toast.makeText(getContext(), "This UID is already the Owner.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> perms = new HashMap<>();
        for (Map.Entry<String, CheckBox> entry : addAdminCheckBoxes.entrySet()) {
            perms.put(entry.getKey(), entry.getValue().isChecked());
        }

        Map<String, Object> assignedCategories = new HashMap<>();
        boolean canManageMatches = Boolean.TRUE.equals(perms.get(Constants.PERM_MANAGE_MATCHES));
        if (canManageMatches) {
            if (binding.rbNewAdminMatchScopeSpecific.isChecked()) {
                boolean anyChecked = false;
                for (Map.Entry<String, CheckBox> entry : newAdminMatchCheckBoxes.entrySet()) {
                    if (entry.getValue().isChecked()) {
                        assignedCategories.put(entry.getKey(), true);
                        anyChecked = true;
                    }
                }
                if (!anyChecked) {
                    assignedCategories.put("all", true);
                }
            } else {
                assignedCategories.put("all", true);
            }
        }

        Map<String, Object> adminData = new HashMap<>();
        adminData.put("name", name);
        adminData.put("role", "admin");
        adminData.put("status", "active");
        adminData.put("permissions", perms);
        adminData.put("assignedCategories", assignedCategories);
        adminData.put("assignedMatches", assignedCategories);

        FirebaseManager.getAdminsRef().child(uid).setValue(adminData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Admin access granted for " + name + "!", Toast.LENGTH_SHORT).show();
                    binding.etNewAdminUid.setText("");
                    binding.etNewAdminName.setText("");
                    binding.rbNewAdminMatchScopeAll.setChecked(true);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onEditPermissions(AdminUser admin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog);
        DialogEditAdminPermissionsBinding dialogBinding = DialogEditAdminPermissionsBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());
        AlertDialog dialog = builder.create();

        dialogBinding.tvDialogAdminName.setText("Admin: " + admin.getName() + " (" + admin.getUid() + ")");

        Map<String, CheckBox> dialogBoxes = new HashMap<>();
        Map<String, Boolean> existingPerms = admin.getPermissions();

        for (Map.Entry<String, String> entry : Constants.ALL_PERMISSIONS.entrySet()) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(entry.getValue());
            cb.setTextColor(requireContext().getColor(R.color.text_main));
            cb.setButtonTintList(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
            cb.setPadding(8, 8, 8, 8);

            boolean isPermitted = Boolean.TRUE.equals(existingPerms.get(entry.getKey()));
            cb.setChecked(isPermitted);

            dialogBinding.layoutDialogPermCheckboxes.addView(cb);
            dialogBoxes.put(entry.getKey(), cb);

            if (Constants.PERM_MANAGE_MATCHES.equals(entry.getKey())) {
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    dialogBinding.layoutMatchScopeSection.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                });
            }
        }

        // Setup Match Category Assignment Scope in Dialog
        Map<String, CheckBox> dialogCategoryBoxes = new HashMap<>();
        Map<String, Boolean> existingAssignedCategories = admin.getAssignedCategories();
        if (existingAssignedCategories == null || existingAssignedCategories.isEmpty()) {
            existingAssignedCategories = admin.getAssignedMatches();
        }
        boolean hasSpecificCategories = existingAssignedCategories != null && !existingAssignedCategories.isEmpty() && !Boolean.TRUE.equals(existingAssignedCategories.get("all"));

        if (hasSpecificCategories) {
            dialogBinding.rbDialogMatchScopeSpecific.setChecked(true);
            dialogBinding.layoutDialogSpecificMatchesContainer.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.rbDialogMatchScopeAll.setChecked(true);
            dialogBinding.layoutDialogSpecificMatchesContainer.setVisibility(View.GONE);
        }

        dialogBinding.rgDialogMatchScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDialogMatchScopeSpecific) {
                dialogBinding.layoutDialogSpecificMatchesContainer.setVisibility(View.VISIBLE);
            } else {
                dialogBinding.layoutDialogSpecificMatchesContainer.setVisibility(View.GONE);
            }
        });

        dialogBinding.progressBarDialogMatches.setVisibility(View.VISIBLE);
        dialogBinding.tvDialogNoMatches.setVisibility(View.GONE);

        final Map<String, Boolean> finalExistingCats = existingAssignedCategories;
        FirebaseManager.getCategoriesRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                dialogBinding.progressBarDialogMatches.setVisibility(View.GONE);
                dialogBinding.layoutDialogMatchCheckboxes.removeAllViews();
                dialogCategoryBoxes.clear();

                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Category cat = child.getValue(Category.class);
                    if (cat != null) {
                        cat.setId(child.getKey());
                        CheckBox cb = new CheckBox(requireContext());
                        String label = cat.getName();
                        cb.setText(label != null && !label.isEmpty() ? label : "Category (" + cat.getId() + ")");
                        cb.setTextColor(requireContext().getColor(R.color.text_main));
                        cb.setButtonTintList(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
                        cb.setPadding(8, 6, 8, 6);

                        if (finalExistingCats != null) {
                            if (Boolean.TRUE.equals(finalExistingCats.get(cat.getId())) ||
                                Boolean.TRUE.equals(finalExistingCats.get(cat.getName()))) {
                                cb.setChecked(true);
                            }
                        }

                        dialogBinding.layoutDialogMatchCheckboxes.addView(cb);
                        dialogCategoryBoxes.put(cat.getId(), cb);
                        count++;
                    }
                }

                if (count == 0) {
                    dialogBinding.tvDialogNoMatches.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dialogBinding.progressBarDialogMatches.setVisibility(View.GONE);
            }
        });

        dialogBinding.btnCancelEditPerms.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSaveEditPerms.setOnClickListener(v -> {
            Map<String, Object> updatedPerms = new HashMap<>();
            for (Map.Entry<String, CheckBox> entry : dialogBoxes.entrySet()) {
                updatedPerms.put(entry.getKey(), entry.getValue().isChecked());
            }

            Map<String, Object> updatedAssignedCategories = new HashMap<>();
            boolean canManage = Boolean.TRUE.equals(updatedPerms.get(Constants.PERM_MANAGE_MATCHES));
            if (canManage) {
                if (dialogBinding.rbDialogMatchScopeSpecific.isChecked()) {
                    boolean anyChecked = false;
                    for (Map.Entry<String, CheckBox> entry : dialogCategoryBoxes.entrySet()) {
                        if (entry.getValue().isChecked()) {
                            updatedAssignedCategories.put(entry.getKey(), true);
                            anyChecked = true;
                        }
                    }
                    if (!anyChecked) {
                        updatedAssignedCategories.put("all", true);
                    }
                } else {
                    updatedAssignedCategories.put("all", true);
                }
            }

            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("permissions", updatedPerms);
            updatePayload.put("assignedCategories", updatedAssignedCategories);
            updatePayload.put("assignedMatches", updatedAssignedCategories);

            FirebaseManager.getAdminsRef().child(admin.getUid()).updateChildren(updatePayload)
                    .addOnSuccessListener(aVoid -> {
                        admin.setPermissions((Map) updatedPerms);
                        admin.setAssignedCategories((Map) updatedAssignedCategories);
                        admin.setAssignedMatches((Map) updatedAssignedCategories);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Permissions & Category Scope updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    @Override
    public void onToggleSuspend(AdminUser admin) {
        boolean isCurrentlySuspended = "suspended".equalsIgnoreCase(admin.getStatus());
        String newStatus = isCurrentlySuspended ? "active" : "suspended";
        String actionMsg = isCurrentlySuspended ? "activated" : "suspended";

        FirebaseManager.getAdminsRef().child(admin.getUid()).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Admin " + actionMsg + "!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRemoveAdmin(AdminUser admin) {
        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Revoke Admin Access?")
                .setMessage("Are you sure you want to completely revoke admin access for " + admin.getName() + "?")
                .setPositiveButton("Revoke Access", (dialog, which) -> {
                    FirebaseManager.getAdminsRef().child(admin.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Admin access revoked!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adminsListener != null) {
            FirebaseManager.getAdminsRef().removeEventListener(adminsListener);
        }
        binding = null;
    }
}
