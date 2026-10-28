package com.gameclub.zone.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gameclub.zone.admin.databinding.FragmentUpdateBinding;
import com.gameclub.zone.admin.model.AppSettings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class UpdateFragment extends Fragment {

    private FragmentUpdateBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUpdateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackUpdate.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        binding.btnPublishUpdate.setOnClickListener(v -> publishUpdate());

        loadUpdateSettings();
    }

    private void loadUpdateSettings() {
        FirebaseManager.getAppSettingsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                AppSettings settings = snapshot.getValue(AppSettings.class);
                if (settings != null) {
                    binding.etUpdateVersion.setText(settings.getLatest_version());
                    binding.etUpdateApkLink.setText(settings.getApk_link());
                    binding.etUpdateTitle.setText(settings.getUpdate_title());
                    binding.etUpdateFeatures.setText(settings.getUpdate_features());
                    binding.switchForceUpdate.setChecked(settings.isForce_update());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    Toast.makeText(getContext(), "Failed to load update settings: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void publishUpdate() {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_UPDATE)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Update", Toast.LENGTH_SHORT).show();
            return;
        }

        String version = binding.etUpdateVersion.getText().toString().trim();
        String apkLink = binding.etUpdateApkLink.getText().toString().trim();
        String title = binding.etUpdateTitle.getText().toString().trim();
        String features = binding.etUpdateFeatures.getText().toString().trim();
        boolean force = binding.switchForceUpdate.isChecked();

        if (version.isEmpty()) {
            binding.etUpdateVersion.setError("Version required");
            binding.etUpdateVersion.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("latest_version", version);
        updates.put("apk_link", apkLink);
        updates.put("update_title", title);
        updates.put("update_features", features);
        updates.put("force_update", force);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "App update published to users!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
