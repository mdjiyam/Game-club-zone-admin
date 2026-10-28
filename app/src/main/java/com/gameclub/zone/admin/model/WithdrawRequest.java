package com.gameclub.zone.admin.model;

public class WithdrawRequest {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private long amount;
    private String method;
    private String mobileNumber;
    private String timestamp;
    private String status; // pending, approved, rejected
    private String processedAt;

    public WithdrawRequest() {
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
        if (userName != null && !userName.isEmpty()) return userName;
        if (userEmail != null && !userEmail.isEmpty()) return userEmail;
        return userId != null ? userId : "User";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method != null ? method : "bKash";
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMobileNumber() {
        return mobileNumber != null ? mobileNumber : "N/A";
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
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
