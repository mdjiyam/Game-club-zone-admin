package com.gameclub.zone.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.gameclub.zone.admin.databinding.FragmentSupportBinding;
import com.gameclub.zone.admin.model.AppSettings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class SupportFragment extends Fragment {

    private FragmentSupportBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackSupport.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        binding.btnSaveSupport.setOnClickListener(v -> saveSupportLinks());

        loadSupportLinks();
    }

    private void loadSupportLinks() {
        FirebaseManager.getAppSettingsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                AppSettings settings = snapshot.getValue(AppSettings.class);
                if (settings != null) {
                    binding.etSupportLink.setText(settings.getSupport_link());
                    binding.etShopLink.setText(settings.getShop_link());
                    binding.etHowToAddMoneyLink.setText(settings.getHow_to_add_money_link());
                    binding.etHowToPlayLink.setText(settings.getHow_to_play_link());
                    binding.etHowToGetRoomIdLink.setText(settings.getHow_to_get_room_id_link());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    Toast.makeText(getContext(), "Failed to load support links: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveSupportLinks() {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_SUPPORT)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Support", Toast.LENGTH_SHORT).show();
            return;
        }

        String support = binding.etSupportLink.getText().toString().trim();
        String shop = binding.etShopLink.getText().toString().trim();
        String addMoney = binding.etHowToAddMoneyLink.getText().toString().trim();
        String howToPlay = binding.etHowToPlayLink.getText().toString().trim();
        String roomId = binding.etHowToGetRoomIdLink.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("support_link", support);
        updates.put("shop_link", shop);
        updates.put("how_to_add_money_link", addMoney);
        updates.put("how_to_play_link", howToPlay);
        updates.put("how_to_get_room_id_link", roomId);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Support links saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
