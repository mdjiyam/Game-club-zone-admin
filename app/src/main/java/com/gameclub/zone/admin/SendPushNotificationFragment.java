package com.gameclub.zone.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.PushNotificationAdapter;
import com.gameclub.zone.admin.databinding.FragmentSendPushNotificationBinding;
import com.gameclub.zone.admin.model.AdminUser;
import com.gameclub.zone.admin.model.PushNotificationModel;
import com.gameclub.zone.admin.model.User;
import com.gameclub.zone.admin.repository.NotificationRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SendPushNotificationFragment extends Fragment {

    private FragmentSendPushNotificationBinding binding;
    private PushNotificationAdapter adapter;
    private final List<PushNotificationModel> pushList = new ArrayList<>();
    private final List<User> loadedUsers = new ArrayList<>();
    private ValueEventListener pushHistoryListener;
    private String selectedTargetUserId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSendPushNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackPush.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        setupTargetTypeSelection();
        setupActionButtons();
        setupRecyclerView();
        loadPushNotifications();
        checkServerStatus();

        binding.btnConfigureApiKey.setOnClickListener(v -> showApiKeyDialog(null));
    }

    private void checkServerStatus() {
        if (binding == null) return;
        binding.tvServerTitle.setText("GAME CLUB ZONE: Checking");
        binding.tvServerStatusBadge.setText("Checking server health...");
        NotificationRepository.getInstance().checkServerHealth(new NotificationRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isHealthy) {
                if (binding == null || getContext() == null) return;
                if (Boolean.TRUE.equals(isHealthy)) {
                    binding.tvServerTitle.setText("GAME CLUB ZONE: Connected");
                    binding.tvServerStatusBadge.setText("Checking server health...");
                    binding.viewOnlineDot.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.success)));
                } else {
                    binding.tvServerTitle.setText("GAME CLUB ZONE: Standby");
                    binding.tvServerStatusBadge.setText("Server warming up / offline");
                    binding.viewOnlineDot.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.warning)));
                }
            }

            @Override
            public void onError(String error) {
                if (binding == null || getContext() == null) return;
                binding.tvServerTitle.setText("GAME CLUB ZONE: Offline");
                binding.tvServerStatusBadge.setText("Connection error");
                binding.viewOnlineDot.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.danger)));
            }
        });
    }

    private void setupTargetTypeSelection() {
        binding.rgTargetType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTargetSpecific) {
                binding.layoutSpecificUser.setVisibility(View.VISIBLE);
                if (loadedUsers.isEmpty()) {
                    loadUsersList();
                }
            } else {
                binding.layoutSpecificUser.setVisibility(View.GONE);
                selectedTargetUserId = "";
            }
        });
    }

    private void loadUsersList() {
        binding.progressBarUsers.setVisibility(View.VISIBLE);
        NotificationRepository.getInstance().loadUsers(new NotificationRepository.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                if (binding == null) return;
                binding.progressBarUsers.setVisibility(View.GONE);
                loadedUsers.clear();
                if (result != null) {
                    loadedUsers.addAll(result);
                }

                List<String> userDisplayList = new ArrayList<>();
                if (loadedUsers.isEmpty()) {
                    userDisplayList.add("No users found");
                } else {
                    for (User u : loadedUsers) {
                        String name = u.getUsername();
                        String uid = u.getUid() != null ? u.getUid() : "N/A";
                        String shortUid = uid.length() > 8 ? uid.substring(0, 8) + "..." : uid;
                        userDisplayList.add(name + " (" + shortUid + ")");
                    }
                }

                if (getContext() != null) {
                    ArrayAdapter<String> userSpinnerAdapter = new ArrayAdapter<>(
                            requireContext(),
                            R.layout.item_spinner_selected,
                            userDisplayList
                    );
                    userSpinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                    binding.spinnerTargetUser.setAdapter(userSpinnerAdapter);

                    binding.spinnerTargetUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < loadedUsers.size()) {
                                User selected = loadedUsers.get(position);
                                selectedTargetUserId = selected.getUid();
                                binding.tvSelectedUserUid.setText("Selected UID: " + selected.getUid() + " (" + selected.getUsername() + ")");
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedTargetUserId = "";
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (binding == null) return;
                binding.progressBarUsers.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load users: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActionButtons() {
        binding.btnSendNotification.setOnClickListener(v -> sendNotification());
        binding.btnClearForm.setOnClickListener(v -> clearForm());
    }

    private void clearForm() {
        binding.etPushTitle.setText("");
        binding.etPushMessage.setText("");
        binding.etPushImageUrl.setText("");
        binding.rbTargetAll.setChecked(true);
        binding.layoutSpecificUser.setVisibility(View.GONE);
        selectedTargetUserId = "";
        binding.tvSelectedUserUid.setText("Target UID: None selected");
    }

    private void sendNotification() {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS)) {
            Toast.makeText(getContext(), "Permission Denied: Send Notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.etPushTitle.getText().toString().trim();
        String message = binding.etPushMessage.getText().toString().trim();
        String imageUrl = binding.etPushImageUrl.getText().toString().trim();
        boolean isSpecific = binding.rbTargetSpecific.isChecked();

        // 1. Form validation
        if (title.isEmpty()) {
            binding.etPushTitle.setError("Title is required");
            binding.etPushTitle.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            binding.etPushMessage.setError("Message is required");
            binding.etPushMessage.requestFocus();
            return;
        }

        String targetType = "all";
        String targetUserId = "";

        if (isSpecific) {
            targetType = "user";
            targetUserId = selectedTargetUserId;
            if (targetUserId == null || targetUserId.isEmpty()) {
                Toast.makeText(getContext(), "Please select a specific user from the list", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Determine creator info
        AdminUser admin = FirebaseManager.getCurrentAdmin();
        FirebaseUser user = FirebaseManager.getCurrentUser();
        String createdBy = "Admin";
        if (admin != null && admin.getName() != null && !admin.getName().isEmpty()) {
            createdBy = admin.getName();
        } else if (user != null && user.getEmail() != null) {
            createdBy = user.getEmail();
        }

        final String finalTargetType = targetType;
        final String finalTargetUserId = targetUserId;
        final String finalCreatedBy = createdBy;

        // 2. Fetch API Key securely (from BuildConfig, local prefs, or Firebase settings)
        NotificationRepository.getInstance().fetchApiKey(requireContext(), new NotificationRepository.Callback<String>() {
            @Override
            public void onSuccess(String apiKey) {
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    // Prompt admin to provide API key securely
                    showApiKeyDialog(() -> executeSend(title, message, imageUrl, finalTargetType, finalTargetUserId, finalCreatedBy));
                    return;
                }

                executeSend(title, message, imageUrl, finalTargetType, finalTargetUserId, finalCreatedBy);
            }

            @Override
            public void onError(String error) {
                showApiKeyDialog(() -> executeSend(title, message, imageUrl, finalTargetType, finalTargetUserId, finalCreatedBy));
            }
        });
    }

    private void executeSend(String title, String message, String imageUrl, String targetType, String targetUserId, String createdBy) {
        String apiKey = NotificationRepository.getInstance().getSavedApiKey(requireContext());

        // Show loading state and prevent duplicate clicks
        binding.btnSendNotification.setEnabled(false);
        binding.btnSendNotification.setText("SENDING VIA SERVER...");
        binding.btnClearForm.setEnabled(false);
        binding.progressBarSending.setVisibility(View.VISIBLE);

        NotificationRepository.getInstance().sendPushNotificationViaServer(
                requireContext(),
                title,
                message,
                imageUrl,
                targetType,
                targetUserId,
                createdBy,
                apiKey,
                new NotificationRepository.Callback<String>() {
                    @Override
                    public void onSuccess(String resultMessage) {
                        if (binding == null) return;
                        binding.btnSendNotification.setEnabled(true);
                        binding.btnSendNotification.setText("SEND NOTIFICATION");
                        binding.btnClearForm.setEnabled(true);
                        binding.progressBarSending.setVisibility(View.GONE);

                        clearForm();

                        if (getContext() != null) {
                            new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                                     .setTitle("Notification Sent!")
                                     .setMessage(resultMessage != null && !resultMessage.isEmpty() ? resultMessage : "Push notification sent successfully via Game Club Zone!")
                                     .setPositiveButton("OK", null)
                                     .show();
                            Toast.makeText(getContext(), "Notification sent successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (binding == null) return;
                        binding.btnSendNotification.setEnabled(true);
                        binding.btnSendNotification.setText("SEND NOTIFICATION");
                        binding.btnClearForm.setEnabled(true);
                        binding.progressBarSending.setVisibility(View.GONE);

                        if (getContext() != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                                    .setTitle("Sending Failed")
                                    .setMessage(error)
                                    .setPositiveButton("OK", null);

                            if (error != null && error.contains("API key")) {
                                builder.setNeutralButton("Configure Key", (dialog, which) -> showApiKeyDialog(null));
                            }
                            builder.show();
                        }
                    }
                }
        );
    }

    private void showApiKeyDialog(@Nullable Runnable onSavedCallback) {
        Context context = requireContext();
        String currentKey = NotificationRepository.getInstance().getSavedApiKey(context);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter x-api-key");
        input.setTextColor(getResources().getColor(R.color.text_main));
        input.setHintTextColor(getResources().getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input_field);
        if (!currentKey.isEmpty()) {
            input.setText(currentKey);
        }

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin / 2, margin, margin / 2);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(context, R.style.GCZ_DarkDialog)
                .setTitle("GAME CLUB ZONE API Key")
                .setMessage("Enter the secret API key (x-api-key) for the Game Club Zone notification server:")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newKey = input.getText().toString().trim();
                    NotificationRepository.getInstance().saveApiKey(context, newKey);
                    Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show();
                    if (onSavedCallback != null) {
                        onSavedCallback.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupRecyclerView() {
        adapter = new PushNotificationAdapter(requireContext(), this::confirmDeletePush);
        binding.rvPushHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPushHistory.setAdapter(adapter);
    }

    private void loadPushNotifications() {
        pushHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pushList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String id = child.getKey();
                        String title = child.child("title").getValue(String.class);
                        String message = child.child("message").getValue(String.class);
                        if (message == null || message.isEmpty()) {
                            message = child.child("body").getValue(String.class);
                        }
                        String imageUrl = child.child("imageUrl").getValue(String.class);
                        String targetType = child.child("targetType").getValue(String.class);
                        String targetUserId = child.child("targetUserId").getValue(String.class);
                        String createdBy = child.child("createdBy").getValue(String.class);
                        String status = child.child("status").getValue(String.class);
                        Long createdAt = child.child("createdAt").getValue(Long.class);

                        PushNotificationModel model = new PushNotificationModel();
                        model.setId(id);
                        model.setTitle(title != null ? title : "");
                        model.setMessage(message != null ? message : "");
                        model.setImageUrl(imageUrl != null ? imageUrl : "");
                        model.setTargetType(targetType != null ? targetType : "all");
                        model.setTargetUserId(targetUserId != null ? targetUserId : "");
                        model.setCreatedBy(createdBy != null ? createdBy : "Admin");
                        model.setStatus(status != null ? status : "pending");
                        model.setCreatedAt(createdAt != null ? createdAt : 0L);

                        pushList.add(model);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Show newest notifications first
                Collections.reverse(pushList);

                if (binding != null) {
                    adapter.setItems(pushList);
                    binding.tvHistoryCount.setText(pushList.size() + " Total");
                    binding.tvNoPushHistory.setVisibility(pushList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        NotificationRepository.getInstance().observePushNotifications(pushHistoryListener);
    }

    private void confirmDeletePush(PushNotificationModel item) {
        if (!FirebaseManager.hasPermission(Constants.PERM_SEND_NOTIFICATIONS)) {
            Toast.makeText(getContext(), "Permission Denied: Delete Notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog)
                .setTitle("Delete Push Notification")
                .setMessage("Are you sure you want to delete this push notification record from history?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (item.getId() != null) {
                        NotificationRepository.getInstance().deletePushNotification(item.getId(), new NotificationRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Notification removed", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pushHistoryListener != null) {
            NotificationRepository.getInstance().removePushNotificationsObserver(pushHistoryListener);
        }
        binding = null;
    }
}
