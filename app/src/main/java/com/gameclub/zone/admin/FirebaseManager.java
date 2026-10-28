package com.gameclub.zone.admin;

import android.content.Context;
import com.gameclub.zone.admin.model.AdminUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseManager {
    private static AdminUser currentAdmin;
    private static FirebaseDatabase databaseInstance;

    public static synchronized void init(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:842418289880:android:2d0d876403249a875cebad")
                    .setApiKey("AIzaSyCFSj-4zZOSGohpv5rS8E_D-wG_K24est0")
                    .setDatabaseUrl(Constants.FIREBASE_DB_URL)
                    .setProjectId("game-club-zone")
                    .setStorageBucket("game-club-zone.firebasestorage.app")
                    .build();
            FirebaseApp.initializeApp(context, options);
        }
    }

    public static FirebaseDatabase getDatabase() {
        if (databaseInstance == null) {
            try {
                databaseInstance = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL);
            } catch (Exception e) {
                databaseInstance = FirebaseDatabase.getInstance();
            }
        }
        return databaseInstance;
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseUser getCurrentUser() {
        return getAuth().getCurrentUser();
    }

    public static AdminUser getCurrentAdmin() {
        return currentAdmin;
    }

    public static void setCurrentAdmin(AdminUser admin) {
        currentAdmin = admin;
    }

    public static boolean isOwner() {
        FirebaseUser user = getCurrentUser();
        if (user != null && Constants.OWNER_UID.equals(user.getUid())) {
            return true;
        }
        return currentAdmin != null && "owner".equalsIgnoreCase(currentAdmin.getRole());
    }

    public static boolean hasPermission(String permKey) {
        if (isOwner()) return true;
        return currentAdmin != null && currentAdmin.hasPermission(permKey);
    }

    public static void logout() {
        currentAdmin = null;
        getAuth().signOut();
    }

    // Database node references
    public static DatabaseReference getUsersRef() {
        return getDatabase().getReference("users");
    }

    public static DatabaseReference getCategoriesRef() {
        return getDatabase().getReference("categories");
    }

    public static DatabaseReference getMatchesRef() {
        return getDatabase().getReference("matches");
    }

    public static DatabaseReference getMatchParticipantsRef() {
        return getDatabase().getReference("match_participants");
    }

    public static DatabaseReference getMatchParticipantsRef(String matchKey) {
        return getDatabase().getReference("match_participants").child(matchKey);
    }

    public static DatabaseReference getAllMatchParticipantsRef() {
        return getDatabase().getReference("match_participants");
    }

    public static DatabaseReference getTransactionsRef(String userId) {
        return getDatabase().getReference("transactions").child(userId);
    }

    public static DatabaseReference getWithdrawRequestsRef() {
        return getDatabase().getReference("withdraw_requests");
    }

    public static DatabaseReference getTopUpPackagesRef() {
        return getDatabase().getReference("topup_packages");
    }

    public static DatabaseReference getTopUpOrdersRef() {
        return getDatabase().getReference("topup_orders");
    }

    public static DatabaseReference getAppSettingsRef() {
        return getDatabase().getReference("app_settings");
    }

    public static DatabaseReference getBannersRef() {
        return getDatabase().getReference("app_settings/banners");
    }

    public static DatabaseReference getNotificationsRef() {
        return getDatabase().getReference("notifications");
    }

    public static DatabaseReference getPushNotificationsRef() {
        return getDatabase().getReference("push_notifications");
    }

    public static DatabaseReference getAdminsRef() {
        return getDatabase().getReference("admins");
    }

    public static DatabaseReference getAdminLogsRef() {
        return getDatabase().getReference("admin_logs");
    }
}
