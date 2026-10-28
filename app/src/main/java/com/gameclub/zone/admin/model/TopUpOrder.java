package com.gameclub.zone.admin.model;

public class TopUpOrder {
    private String id;
    private String userId;
    private String userName;
    private String uid; // in-game player ID
    private String packageName;
    private long amount;
    private String timestamp;
    private String status; // pending, approved, rejected
    private String processedAt;

    public TopUpOrder() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId != null ? userId : "";
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName != null ? userName : "Unknown";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUid() {
        return uid != null ? uid : "N/A";
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPackageName() {
        return packageName != null ? packageName : "Package";
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getTimestamp() {
        return timestamp != null ? timestamp : "";
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status != null ? status : "pending";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}
