package com.gameclub.zone.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gameclub.zone.admin.databinding.FragmentSettingsBinding;
import com.gameclub.zone.admin.model.AppSettings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private ValueEventListener settingsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackSettings.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void loadSettings() {
        settingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                AppSettings settings = snapshot.getValue(AppSettings.class);
                if (settings != null) {
                    binding.etSettingsAppName.setText(settings.getApp_name());
                    binding.etSettingsAppLogo.setText(settings.getApp_logo());
                    binding.etSettingsNotice.setText(settings.getNotice());
                    binding.etSettingsPopupText.setText(settings.getPopup_text());
                    binding.switchShowPopup.setChecked(settings.isShow_popup());

                    binding.etBkashNumber.setText(settings.getBkash_number());
                    binding.etNagadNumber.setText(settings.getNagad_number());
                    binding.etRocketNumber.setText(settings.getRocket_number());

                    binding.etMinDeposit.setText(String.valueOf(settings.getMin_deposit()));
                    binding.etMinWithdraw.setText(String.valueOf(settings.getMin_withdraw()));

                    binding.switchShowParticipants.setChecked(settings.isShow_participants());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    Toast.makeText(getContext(), "Failed to load settings: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getAppSettingsRef().addListenerForSingleValueEvent(settingsListener);
    }

    private void saveSettings() {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_SETTINGS)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Settings", Toast.LENGTH_SHORT).show();
            return;
        }

        String appName = binding.etSettingsAppName.getText().toString().trim();
        String appLogo = binding.etSettingsAppLogo.getText().toString().trim();
        String notice = binding.etSettingsNotice.getText().toString().trim();
        String popupText = binding.etSettingsPopupText.getText().toString().trim();
        boolean showPopup = binding.switchShowPopup.isChecked();

        String bkash = binding.etBkashNumber.getText().toString().trim();
        String nagad = binding.etNagadNumber.getText().toString().trim();
        String rocket = binding.etRocketNumber.getText().toString().trim();

        long minDeposit = parseLong(binding.etMinDeposit.getText().toString().trim(), 30);
        long minWithdraw = parseLong(binding.etMinWithdraw.getText().toString().trim(), 50);

        boolean showParticipants = binding.switchShowParticipants.isChecked();

        Map<String, Object> updates = new HashMap<>();
        updates.put("app_name", appName);
        updates.put("app_logo", appLogo);
        updates.put("notice", notice);
        updates.put("slider_notice", notice);
        updates.put("marquee", notice);
        updates.put("notice_text", notice);
        updates.put("popup_text", popupText);
        updates.put("popup", popupText);
        updates.put("show_popup", showPopup);

        Map<String, Object> popupObj = new HashMap<>();
        popupObj.put("show", showPopup);
        popupObj.put("message", popupText);
        popupObj.put("text", popupText);
        popupObj.put("timestamp", System.currentTimeMillis());
        updates.put("popup_notice", popupObj);

        updates.put("bkash_number", bkash);
        updates.put("nagad_number", nagad);
        updates.put("rocket_number", rocket);

        updates.put("min_deposit", minDeposit);
        updates.put("min_withdraw", minWithdraw);

        updates.put("show_participants", showParticipants);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Sync to root nodes for older/hybrid client apps
                    FirebaseManager.getDatabase().getReference("notice").setValue(notice);
                    FirebaseManager.getDatabase().getReference("slider_notice").setValue(notice);
                    FirebaseManager.getDatabase().getReference("popup_notice").setValue(popupObj);
                    FirebaseManager.getDatabase().getReference("popup").setValue(popupObj);
                    Toast.makeText(getContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private long parseLong(String val, long defaultVal) {
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
