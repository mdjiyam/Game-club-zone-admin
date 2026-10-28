package com.gameclub.zone.admin;

import java.util.LinkedHashMap;
import java.util.Map;

public class Constants {
    public static final String OWNER_UID = "nx508odmoJQ9i7lWzXj47f5CV2S2";
    public static final String FIREBASE_DB_URL = "https://game-club-zone-default-rtdb.firebaseio.com";

    // Permission Keys
    public static final String PERM_VIEW_USERS = "view_users";
    public static final String PERM_MANAGE_USERS = "manage_users";
    public static final String PERM_EDIT_BALANCES = "manage_users";
    public static final String PERM_BAN_USERS = "manage_users";
    public static final String PERM_DELETE_USERS = "manage_users";

    public static final String PERM_MANAGE_MATCHES = "manage_matches";
    public static final String PERM_MANAGE_CATEGORIES = "manage_categories";
    public static final String PERM_MANAGE_RESULTS = "manage_results";
    public static final String PERM_MANAGE_WITHDRAWALS = "manage_withdrawals";
    public static final String PERM_MANAGE_TOPUP = "manage_topup";
    public static final String PERM_MANAGE_ORDERS = "manage_orders";
    public static final String PERM_MANAGE_SETTINGS = "manage_settings";
    public static final String PERM_MANAGE_SUPPORT = "manage_support";
    public static final String PERM_MANAGE_UPDATE = "manage_update";
    public static final String PERM_MANAGE_SLIDERS = "manage_sliders";
    public static final String PERM_SEND_NOTIFICATIONS = "send_notifications";

    public static final Map<String, String> ALL_PERMISSIONS = new LinkedHashMap<>();

    static {
        ALL_PERMISSIONS.put(PERM_VIEW_USERS, "View Users");
        ALL_PERMISSIONS.put(PERM_MANAGE_USERS, "Manage Users & Balances");
        ALL_PERMISSIONS.put(PERM_MANAGE_MATCHES, "Manage Matches");
        ALL_PERMISSIONS.put(PERM_MANAGE_CATEGORIES, "Manage Categories");
        ALL_PERMISSIONS.put(PERM_MANAGE_RESULTS, "Manage Results & Payouts");
        ALL_PERMISSIONS.put(PERM_MANAGE_WITHDRAWALS, "Manage Withdrawals");
        ALL_PERMISSIONS.put(PERM_MANAGE_TOPUP, "Manage Top-Up");
        ALL_PERMISSIONS.put(PERM_MANAGE_ORDERS, "Manage Orders");
        ALL_PERMISSIONS.put(PERM_MANAGE_SETTINGS, "Manage App Settings");
        ALL_PERMISSIONS.put(PERM_MANAGE_SUPPORT, "Manage Support & Links");
        ALL_PERMISSIONS.put(PERM_MANAGE_UPDATE, "Manage App Update");
        ALL_PERMISSIONS.put(PERM_MANAGE_SLIDERS, "Manage Sliders");
        ALL_PERMISSIONS.put(PERM_SEND_NOTIFICATIONS, "Push Notifications");
    }
}
