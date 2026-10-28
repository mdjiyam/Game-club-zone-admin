package com.gameclub.zone.admin.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.gameclub.zone.admin.BuildConfig;
import com.gameclub.zone.admin.FirebaseManager;
import com.gameclub.zone.admin.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class NotificationRepository {

    public static final String RENDER_BASE_URL = "https://game-club-zone-notifications-server.onrender.com";
    public static final String HEALTH_API_URL = RENDER_BASE_URL + "/api/health";
    public static final String SEND_NOTIFICATION_API_URL = RENDER_BASE_URL + "/api/send-notification";

    public static final String PREFS_NAME = "gcz_admin_notification_prefs";
    public static final String PREF_KEY_SERVER_API_KEY = "notification_server_api_key";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private static NotificationRepository instance;

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    /**
     * Checks if an API key is stored locally or via BuildConfig without hardcoding.
     */
    public String getSavedApiKey(Context context) {
        // 1. Check BuildConfig dynamically if configured via Secrets / .env
        String buildConfigKey = getBuildConfigApiKey();
        if (buildConfigKey != null && !buildConfigKey.trim().isEmpty()) {
            return buildConfigKey.trim();
        }

        // 2. Check local private SharedPreferences
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedKey = prefs.getString(PREF_KEY_SERVER_API_KEY, "");
            if (savedKey != null && !savedKey.trim().isEmpty()) {
                return savedKey.trim();
            }
        }

        return "";
    }

    /**
     * Save API Key locally in private preferences and optionally in Firebase app_settings.
     */
    public void saveApiKey(Context context, String apiKey) {
        if (apiKey == null) apiKey = "";
        String trimmed = apiKey.trim();

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_KEY_SERVER_API_KEY, trimmed).apply();
        }

        // Also sync to app_settings if non-empty so all authorized admins share it
        if (!trimmed.isEmpty()) {
            FirebaseManager.getAppSettingsRef().child("notification_api_key").setValue(trimmed);
        }
    }

    /**
     * Retrieve the API key with fallback to Firebase app_settings.
     */
    public void fetchApiKey(Context context, Callback<String> callback) {
        String key = getSavedApiKey(context);
        if (!key.isEmpty()) {
            if (callback != null) callback.onSuccess(key);
            return;
        }

        // Check Firebase Realtime Database
        FirebaseManager.getAppSettingsRef().child("notification_api_key")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String fbKey = snapshot.getValue(String.class);
                        if (fbKey != null && !fbKey.trim().isEmpty()) {
                            if (context != null) {
                                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                                prefs.edit().putString(PREF_KEY_SERVER_API_KEY, fbKey.trim()).apply();
                            }
                            if (callback != null) callback.onSuccess(fbKey.trim());
                        } else {
                            if (callback != null) callback.onSuccess("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (callback != null) callback.onSuccess("");
                    }
                });
    }

    /**
     * Checks if the Render notification server is online and reachable.
     */
    public void checkServerHealth(Callback<Boolean> callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(HEALTH_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                boolean isHealthy = (code == 200);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(isHealthy);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(false);
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Sends push notification via the deployed Render notification server,
     * and tracks status ('pending' -> 'sent' or 'failed') in Firebase Realtime Database.
     */
    public void sendPushNotificationViaServer(
            Context context,
            String title,
            String message,
            String imageUrl,
            String targetType,
            String targetUserId,
            String createdBy,
            String apiKey,
            Callback<String> callback
    ) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Step 1: Save notification record in Firebase Realtime Database with status="pending"
        DatabaseReference pushRef = FirebaseManager.getPushNotificationsRef().push();
        String notificationId = pushRef.getKey();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", message);
        notificationData.put("body", message); // legacy compatibility
        notificationData.put("imageUrl", imageUrl != null ? imageUrl : "");
        notificationData.put("targetType", targetType != null ? targetType : "all");
        notificationData.put("targetUserId", targetUserId != null ? targetUserId : "");
        notificationData.put("createdAt", ServerValue.TIMESTAMP);
        notificationData.put("createdBy", createdBy != null ? createdBy : "Admin");
        notificationData.put("status", "pending");
        notificationData.put("sent", false);

        pushRef.setValue(notificationData);

        // Step 2: Make POST request to Render Notification Server in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SEND_NOTIFICATION_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    conn.setRequestProperty("x-api-key", apiKey.trim());
                }

                conn.setConnectTimeout(25000);
                conn.setReadTimeout(35000);
                conn.setDoOutput(true);

                // Build JSON payload according to Render server specification:
                // targetType = "all" or "user"
                // targetUserId = "" or UID
                // data = { "imageUrl": ... }
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("title", title);
                jsonBody.put("message", message);

                String apiTargetType = "all";
                String apiTargetUserId = "";
                if ("user".equalsIgnoreCase(targetType) || "specific".equalsIgnoreCase(targetType)) {
                    apiTargetType = "user";
                    apiTargetUserId = (targetUserId != null) ? targetUserId : "";
                }
                jsonBody.put("targetType", apiTargetType);
                jsonBody.put("targetUserId", apiTargetUserId);

                JSONObject dataObj = new JSONObject();
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    dataObj.put("imageUrl", imageUrl.trim());
                }
                jsonBody.put("data", dataObj);

                // Write body
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                String responseBody = "";
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        responseBody = sb.toString();
                    }
                }

                if (responseCode == 200 || responseCode == 201) {
                    // Update Firebase Realtime Database status to "sent"
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", "sent");
                    update.put("sent", true);
                    pushRef.updateChildren(update);

                    String successMsg = "Notification delivered successfully via Game Club Zone.";
                    try {
                        JSONObject respJson = new JSONObject(responseBody);
                        if (respJson.has("message")) {
                            successMsg = respJson.getString("message");
                        }
                    } catch (Exception ignored) {}

                    final String finalSuccess = successMsg;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(finalSuccess);
                    });
                } else {
                    // Update Firebase Realtime Database status to "failed"
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", "failed");
                    pushRef.updateChildren(update);

                    String errorMsg = "Server error (Code: " + responseCode + ")";
                    if (responseCode == 401) {
                        errorMsg = "Unauthorized: Invalid or missing API key. Please check your Game Club Zone API key (x-api-key).";
                    } else if (!responseBody.isEmpty()) {
                        try {
                            JSONObject respJson = new JSONObject(responseBody);
                            if (respJson.has("message")) {
                                errorMsg = respJson.getString("message");
                            } else if (respJson.has("error")) {
                                errorMsg = respJson.getString("error");
                            }
                        } catch (Exception ignored) {
                            errorMsg = responseBody;
                        }
                    }

                    final String finalError = errorMsg;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError(finalError);
                    });
                }

            } catch (Exception e) {
                // Update Firebase Realtime Database status to "failed"
                Map<String, Object> update = new HashMap<>();
                update.put("status", "failed");
                pushRef.updateChildren(update);

                String errorMsg = "Failed to connect to Game Club Zone server: " + e.getMessage();
                final String finalError = errorMsg;
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(finalError);
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Delete notification record from Realtime Database.
     */
    public void deletePushNotification(String notificationId, Callback<Void> callback) {
        if (notificationId == null || notificationId.isEmpty()) {
            if (callback != null) callback.onError("Invalid notification ID");
            return;
        }

        FirebaseManager.getPushNotificationsRef().child(notificationId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Loads registered users list from Realtime Database `users` node.
     */
    public void loadUsers(Callback<List<User>> callback) {
        FirebaseManager.getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> userList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        User user = child.getValue(User.class);
                        if (user != null) {
                            if (user.getUid() == null || user.getUid().isEmpty()) {
                                user.setUid(child.getKey());
                            }
                            userList.add(user);
                        }
                    } catch (Exception ignored) {}
                }
                if (callback != null) {
                    callback.onSuccess(userList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError(error.getMessage());
                }
            }
        });
    }

    /**
     * Observe push notifications history in real time.
     */
    public void observePushNotifications(ValueEventListener listener) {
        FirebaseManager.getPushNotificationsRef().addValueEventListener(listener);
    }

    /**
     * Remove real-time listener.
     */
    public void removePushNotificationsObserver(ValueEventListener listener) {
        if (listener != null) {
            FirebaseManager.getPushNotificationsRef().removeEventListener(listener);
        }
    }

    private static String getBuildConfigApiKey() {
        try {
            java.lang.reflect.Field field = BuildConfig.class.getField("NOTIFICATION_SERVER_API_KEY");
            Object val = field.get(null);
            return val != null ? val.toString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
