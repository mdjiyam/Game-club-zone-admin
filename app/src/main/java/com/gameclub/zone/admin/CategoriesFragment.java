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
import com.gameclub.zone.admin.adapter.CategoryAdapter;
import com.gameclub.zone.admin.databinding.DialogCategoryEditorBinding;
import com.gameclub.zone.admin.databinding.FragmentCategoriesBinding;
import com.gameclub.zone.admin.model.Category;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesFragment extends Fragment implements CategoryAdapter.CategoryActionListener {

    private FragmentCategoriesBinding binding;
    private CategoryAdapter adapter;
    private final List<Category> categories = new ArrayList<>();
    private ValueEventListener categoriesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackCategories.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new CategoryAdapter(requireContext(), this);
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);

        binding.fabAddCategory.setOnClickListener(v -> showCategoryEditorDialog(null));
        binding.swipeRefreshCategories.setOnRefreshListener(this::loadCategories);

        loadCategories();
    }

    private void loadCategories() {
        binding.swipeRefreshCategories.setRefreshing(true);
        if (categoriesListener != null) {
            FirebaseManager.getCategoriesRef().removeEventListener(categoriesListener);
        }

        categoriesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                com.gameclub.zone.admin.model.AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
                boolean isOwner = FirebaseManager.isOwner();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Category cat = child.getValue(Category.class);
                    if (cat != null) {
                        cat.setId(child.getKey());
                        if (isOwner || currentAdmin == null || currentAdmin.canManageCategory(cat.getId(), cat.getName())) {
                            categories.add(cat);
                        }
                    }
                }
                if (binding != null) {
                    binding.swipeRefreshCategories.setRefreshing(false);
                    adapter.setCategories(categories);
                    binding.tvEmptyCategories.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.swipeRefreshCategories.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load categories: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getCategoriesRef().addValueEventListener(categoriesListener);
    }

    private void showCategoryEditorDialog(Category categoryToEdit) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_CATEGORIES)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Categories", Toast.LENGTH_SHORT).show();
            return;
        }

        com.gameclub.zone.admin.model.AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
        if (categoryToEdit != null && !FirebaseManager.isOwner() && currentAdmin != null) {
            if (!currentAdmin.canManageCategory(categoryToEdit.getId(), categoryToEdit.getName())) {
                Toast.makeText(getContext(), "Permission Denied: You are not assigned to this category.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog);
        DialogCategoryEditorBinding dialogBinding = DialogCategoryEditorBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());
        AlertDialog dialog = builder.create();

        boolean isEdit = categoryToEdit != null;
        dialogBinding.tvDialogCategoryTitle.setText(isEdit ? "📁 EDIT CATEGORY" : "📁 ADD NEW CATEGORY");

        if (isEdit) {
            dialogBinding.etDialogCategoryName.setText(categoryToEdit.getName());
            dialogBinding.etDialogCategoryImg.setText(categoryToEdit.getImg());
            dialogBinding.etDialogCategoryRules.setText(categoryToEdit.getRules());
        }

        dialogBinding.btnCancelCategory.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSaveCategory.setOnClickListener(v -> {
            String name = dialogBinding.etDialogCategoryName.getText().toString().trim();
            String img = dialogBinding.etDialogCategoryImg.getText().toString().trim();
            String rules = dialogBinding.etDialogCategoryRules.getText().toString().trim();

            if (name.isEmpty()) {
                dialogBinding.etDialogCategoryName.setError("Name required");
                dialogBinding.etDialogCategoryName.requestFocus();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("img", img);
            data.put("rules", rules);

            if (isEdit) {
                FirebaseManager.getCategoriesRef().child(categoryToEdit.getId()).updateChildren(data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Category updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                String key = FirebaseManager.getCategoriesRef().push().getKey();
                if (key != null) {
                    FirebaseManager.getCategoriesRef().child(key).setValue(data)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Category created!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });

        dialog.show();
    }

    @Override
    public void onEditCategory(Category category) {
        showCategoryEditorDialog(category);
    }

    @Override
    public void onDeleteCategory(Category category) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_CATEGORIES)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Categories", Toast.LENGTH_SHORT).show();
            return;
        }

        com.gameclub.zone.admin.model.AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
        if (!FirebaseManager.isOwner() && currentAdmin != null) {
            if (!currentAdmin.canManageCategory(category.getId(), category.getName())) {
                Toast.makeText(getContext(), "Permission Denied: You are not assigned to this category.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Delete Category?")
                .setMessage("Are you sure you want to delete category \"" + category.getName() + "\"? Matches linked to this category may be affected.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseManager.getCategoriesRef().child(category.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Category deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (categoriesListener != null) {
            FirebaseManager.getCategoriesRef().removeEventListener(categoriesListener);
        }
        binding = null;
    }
}
