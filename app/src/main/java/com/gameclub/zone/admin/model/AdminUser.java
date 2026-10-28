package com.gameclub.zone.admin.model;

import java.util.HashMap;
import java.util.Map;

public class AdminUser {
    private String uid;
    private String name;
    private String role; // "owner", "admin"
    private String status; // "active", "suspended"
    private Map<String, Boolean> permissions = new HashMap<>();
    private Map<String, Boolean> assignedCategories = new HashMap<>(); // categoryId -> true, or "all" -> true
    private Map<String, Boolean> assignedMatches = new HashMap<>(); // matchDbKey -> true, or "all" -> true
    private String createdBy;
    private long createdAt;
    private boolean removed;

    public AdminUser() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name != null ? name : "Admin";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role != null ? role : "admin";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status != null ? status : "active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Boolean> getPermissions() {
        if (permissions == null) permissions = new HashMap<>();
        return permissions;
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    public Map<String, Boolean> getAssignedCategories() {
        if (assignedCategories == null) assignedCategories = new HashMap<>();
        return assignedCategories;
    }

    public void setAssignedCategories(Map<String, Boolean> assignedCategories) {
        this.assignedCategories = assignedCategories;
    }

    public Map<String, Boolean> getAssignedMatches() {
        if (assignedMatches == null) assignedMatches = new HashMap<>();
        return assignedMatches;
    }

    public void setAssignedMatches(Map<String, Boolean> assignedMatches) {
        this.assignedMatches = assignedMatches;
    }

    public boolean canManageCategory(String categoryId) {
        return canManageCategory(categoryId, null);
    }

    public boolean canManageCategory(String categoryId, String categoryName) {
        if ("owner".equalsIgnoreCase(role)) return true;
        if (!"active".equalsIgnoreCase(status)) return false;
        if (!hasPermission("manage_matches") && !hasPermission("manage_categories")) return false;
        if (assignedCategories == null || assignedCategories.isEmpty() || Boolean.TRUE.equals(assignedCategories.get("all"))) {
            return true;
        }
        if (categoryId != null && Boolean.TRUE.equals(assignedCategories.get(categoryId))) {
            return true;
        }
        if (categoryName != null && Boolean.TRUE.equals(assignedCategories.get(categoryName))) {
            return true;
        }
        return false;
    }

    public boolean canManageMatch(String matchKey, String matchCategoryId, String matchAdminUid) {
        if ("owner".equalsIgnoreCase(role)) return true;
        if (!"active".equalsIgnoreCase(status)) return false;
        if (!hasPermission("manage_matches")) return false;

        // Specific match assignment by UID
        if (matchAdminUid != null && !matchAdminUid.isEmpty() && matchAdminUid.equals(uid)) {
            return true;
        }

        // Specific match key assignment
        if (assignedMatches != null && matchKey != null && Boolean.TRUE.equals(assignedMatches.get(matchKey))) {
            return true;
        }

        // Category-level access
        if (matchCategoryId != null && !matchCategoryId.isEmpty()) {
            return canManageCategory(matchCategoryId);
        }

        // If category is unrestricted
        if (assignedCategories == null || assignedCategories.isEmpty() || Boolean.TRUE.equals(assignedCategories.get("all"))) {
            return true;
        }

        return false;
    }

    public boolean canManageMatch(String matchKey, String matchAdminUid) {
        return canManageMatch(matchKey, null, matchAdminUid);
    }

    public boolean hasPermission(String permKey) {
        if ("owner".equalsIgnoreCase(role)) return true;
        if (!"active".equalsIgnoreCase(status)) return false;
        return permissions != null && Boolean.TRUE.equals(permissions.get(permKey));
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
}
