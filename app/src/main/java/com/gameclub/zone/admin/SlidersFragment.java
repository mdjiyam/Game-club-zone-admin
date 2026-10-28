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
import com.gameclub.zone.admin.adapter.SliderAdapter;
import com.gameclub.zone.admin.databinding.FragmentSlidersBinding;
import com.gameclub.zone.admin.model.BannerModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlidersFragment extends Fragment implements SliderAdapter.SliderActionListener {

    private FragmentSlidersBinding binding;
    private SliderAdapter adapter;
    private final List<BannerModel> banners = new ArrayList<>();
    private ValueEventListener slidersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSlidersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackSliders.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new SliderAdapter(requireContext(), this);
        binding.rvSliders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSliders.setAdapter(adapter);

        binding.btnAddBanner.setOnClickListener(v -> addBanner());
        binding.btnSaveSliderUnderNotice.setOnClickListener(v -> saveSliderNotice());

        loadBanners();
        loadSliderNotice();
    }

    private void loadSliderNotice() {
        FirebaseManager.getAppSettingsRef().child("notice").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                String notice = snapshot.getValue(String.class);
                if (notice != null && !notice.isEmpty()) {
                    binding.etSliderUnderNotice.setText(notice);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveSliderNotice() {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_SLIDERS) && !FirebaseManager.hasPermission(Constants.PERM_MANAGE_SETTINGS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Sliders / Settings", Toast.LENGTH_SHORT).show();
            return;
        }

        String notice = binding.etSliderUnderNotice.getText().toString().trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("notice", notice);
        updates.put("slider_notice", notice);
        updates.put("marquee", notice);
        updates.put("notice_text", notice);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update root notice for legacy clients
                    FirebaseManager.getDatabase().getReference("notice").setValue(notice);
                    FirebaseManager.getDatabase().getReference("slider_notice").setValue(notice);
                    Toast.makeText(getContext(), "Slider notice updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating notice: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadBanners() {
        if (slidersListener != null) {
            FirebaseManager.getAppSettingsRef().child("banners").removeEventListener(slidersListener);
        }

        slidersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                banners.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    BannerModel b = child.getValue(BannerModel.class);
                    if (b != null) {
                        b.setId(child.getKey());
                        banners.add(b);
                    }
                }
                if (binding != null) {
                    adapter.setBanners(banners);
                    binding.tvEmptySliders.setVisibility(banners.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    Toast.makeText(getContext(), "Failed to load banners: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getAppSettingsRef().child("banners").addValueEventListener(slidersListener);
    }

    private void addBanner() {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_SLIDERS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Sliders", Toast.LENGTH_SHORT).show();
            return;
        }

        String img = binding.etBannerImageUrl.getText().toString().trim();
        String link = binding.etBannerLink.getText().toString().trim();

        if (img.isEmpty()) {
            binding.etBannerImageUrl.setError("Image URL required");
            binding.etBannerImageUrl.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("img", img);
        data.put("link", link);

        String key = FirebaseManager.getAppSettingsRef().child("banners").push().getKey();
        if (key != null) {
            FirebaseManager.getAppSettingsRef().child("banners").child(key).setValue(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Banner added!", Toast.LENGTH_SHORT).show();
                        binding.etBannerImageUrl.setText("");
                        binding.etBannerLink.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDeleteBanner(BannerModel banner) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_SLIDERS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Sliders", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Delete Banner?")
                .setMessage("Are you sure you want to remove this slider banner?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseManager.getAppSettingsRef().child("banners").child(banner.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Banner removed!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (slidersListener != null) {
            FirebaseManager.getAppSettingsRef().child("banners").removeEventListener(slidersListener);
        }
        binding = null;
    }
}
