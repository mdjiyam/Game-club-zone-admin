package com.gameclub.zone.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.NotificationAdapter;
import com.gameclub.zone.admin.databinding.FragmentNotificationsBinding;
import com.gameclub.zone.admin.model.NotificationModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment implements NotificationAdapter.NotificationActionListener {

    private FragmentNotificationsBinding binding;
    private NotificationAdapter adapter;
    private final List<NotificationModel> notices = new ArrayList<>();
    private ValueEventListener noticesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackNotifications.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        adapter = new NotificationAdapter(requireContext(), this);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnOpenPushNotificationPage.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showFragment(new SendPushNotificationFragment(), true);
            }
        });

        binding.btnSavePopUp.setOnClickListener(v -> savePopUpNotice());
        binding.btnTurnOffPopUp.setOnClickListener(v -> turnOffPopUpNotice());
        binding.btnSaveSliderTicker.setOnClickListener(v -> saveSliderTickerNotice());
        binding.btnPublishNotice.setOnClickListener(v -> publishBroadcastNotice());

        loadNotices();
        loadPopUpAndSliderSettings();
    }

    private void loadPopUpAndSliderSettings() {
        FirebaseManager.getAppSettingsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                Boolean showPopup = snapshot.child("show_popup").getValue(Boolean.class);
                String popupTitle = snapshot.child("popup_title").getValue(String.class);
                String popupText = snapshot.child("popup_text").getValue(String.class);
                if (popupText == null || popupText.isEmpty()) {
                    popupText = snapshot.child("popup").getValue(String.class);
                }
                String notice = snapshot.child("notice").getValue(String.class);

                binding.switchPopUp.setChecked(Boolean.TRUE.equals(showPopup));
                if (popupTitle != null) binding.etPopUpTitle.setText(popupTitle);
                if (popupText != null) binding.etPopUpText.setText(popupText);
                if (notice != null) binding.etSliderTickerNotice.setText(notice);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void savePopUpNotice() {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS) && !FirebaseManager.hasPermission(Constants.PERM_MANAGE_SETTINGS)) {
            Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.etPopUpTitle.getText().toString().trim();
        String text = binding.etPopUpText.getText().toString().trim();

        if (text.isEmpty()) {
            binding.etPopUpText.setError("Pop-up message required");
            binding.etPopUpText.requestFocus();
            return;
        }

        boolean show = binding.switchPopUp.isChecked();
        if (!show) {
            binding.switchPopUp.setChecked(true);
            show = true;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("show_popup", show);
        updates.put("popup_title", title);
        updates.put("popup_text", text);
        updates.put("popup", text);

        Map<String, Object> popupObj = new HashMap<>();
        popupObj.put("show", show);
        popupObj.put("title", title);
        popupObj.put("message", text);
        popupObj.put("text", text);
        popupObj.put("timestamp", System.currentTimeMillis());
        updates.put("popup_notice", popupObj);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also sync to root popup_notice and popup for compatibility
                    FirebaseManager.getDatabase().getReference("popup_notice").setValue(popupObj);
                    FirebaseManager.getDatabase().getReference("popup").setValue(popupObj);
                    Toast.makeText(getContext(), "Pop-up notice saved and activated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving pop-up: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void turnOffPopUpNotice() {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS) && !FirebaseManager.hasPermission(Constants.PERM_MANAGE_SETTINGS)) {
            Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.switchPopUp.setChecked(false);
        Map<String, Object> updates = new HashMap<>();
        updates.put("show_popup", false);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    FirebaseManager.getDatabase().getReference("popup_notice").child("show").setValue(false);
                    FirebaseManager.getDatabase().getReference("popup").child("show").setValue(false);
                    Toast.makeText(getContext(), "Pop-up notice disabled!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveSliderTickerNotice() {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS) && !FirebaseManager.hasPermission(Constants.PERM_MANAGE_SLIDERS) && !FirebaseManager.hasPermission(Constants.PERM_MANAGE_SETTINGS)) {
            Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        String notice = binding.etSliderTickerNotice.getText().toString().trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("notice", notice);
        updates.put("slider_notice", notice);
        updates.put("marquee", notice);
        updates.put("notice_text", notice);

        FirebaseManager.getAppSettingsRef().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    FirebaseManager.getDatabase().getReference("notice").setValue(notice);
                    FirebaseManager.getDatabase().getReference("slider_notice").setValue(notice);
                    Toast.makeText(getContext(), "Slider notice updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadNotices() {
        if (noticesListener != null) {
            FirebaseManager.getNotificationsRef().removeEventListener(noticesListener);
        }

        noticesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notices.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String title = child.child("title").getValue(String.class);
                        String body = child.child("body").getValue(String.class);
                        if (body == null || body.isEmpty()) body = child.child("message").getValue(String.class);
                        if (body == null || body.isEmpty()) body = child.child("text").getValue(String.class);
                        Boolean active = child.child("active").getValue(Boolean.class);
                        String target = child.child("target").getValue(String.class);

                        NotificationModel n = new NotificationModel();
                        n.setId(child.getKey());
                        n.setTitle(title != null ? title : "");
                        n.setBody(body != null ? body : "");
                        n.setActive(active != null ? active : true);
                        n.setTarget(target != null ? target : "all");

                        if (n.isActive()) {
                            notices.add(n);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (binding != null) {
                    adapter.setNotices(notices);
                    binding.tvEmptyNotifications.setVisibility(notices.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    Toast.makeText(getContext(), "Failed to load notices: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        FirebaseManager.getNotificationsRef().addValueEventListener(noticesListener);
    }

    private void publishBroadcastNotice() {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS)) {
            Toast.makeText(getContext(), "Permission Denied: Send Notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.etNoticeTitle.getText().toString().trim();
        String body = binding.etNoticeBody.getText().toString().trim();

        if (title.isEmpty()) {
            binding.etNoticeTitle.setError("Title required");
            binding.etNoticeTitle.requestFocus();
            return;
        }

        if (body.isEmpty()) {
            binding.etNoticeBody.setError("Body required");
            binding.etNoticeBody.requestFocus();
            return;
        }

        String dateStr = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("message", body);
        data.put("text", body);
        data.put("target", "all");
        data.put("active", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("date", dateStr);

        String key = FirebaseManager.getNotificationsRef().push().getKey();
        if (key != null) {
            FirebaseManager.getNotificationsRef().child(key).setValue(data)
                    .addOnSuccessListener(aVoid -> {
                        FirebaseManager.getDatabase().getReference("latest_notification").setValue(data);
                        FirebaseManager.getDatabase().getReference("notice_broadcast").setValue(data);
                        Toast.makeText(getContext(), "Broadcast notification sent!", Toast.LENGTH_SHORT).show();
                        binding.etNoticeTitle.setText("");
                        binding.etNoticeBody.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDeactivateNotice(NotificationModel notice) {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS)) {
            Toast.makeText(getContext(), "Permission Denied: Send Notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.getNotificationsRef().child(notice.getId()).child("active").setValue(false)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Notice deactivated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (noticesListener != null) {
            FirebaseManager.getNotificationsRef().removeEventListener(noticesListener);
        }
        binding = null;
    }
}
