package com.gameclub.zone.admin.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PushNotificationModel {
    private String id;
    private String title;
    private String message;
    private String body; // for backward compatibility
    private String imageUrl;
    private String targetType; // "all" or "specific"
    private String targetUserId;
    private Long createdAt;
    private String createdBy;
    private String status; // "pending", "sent", "failed"
    private boolean sent; // for backward compatibility

    public PushNotificationModel() {
        this.targetType = "all";
        this.targetUserId = "";
        this.status = "pending";
    }

    public PushNotificationModel(String id, String title, String message, String imageUrl,
                                 String targetType, String targetUserId, Long createdAt,
                                 String createdBy, String status) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.body = message;
        this.imageUrl = imageUrl;
        this.targetType = targetType != null ? targetType : "all";
        this.targetUserId = targetUserId != null ? targetUserId : "";
        this.createdAt = createdAt;
        this.createdBy = createdBy != null ? createdBy : "Admin";
        this.status = status != null ? status : "pending";
        this.sent = "sent".equalsIgnoreCase(status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return body != null ? body : "";
    }

    public void setMessage(String message) {
        this.message = message;
        this.body = message;
    }

    public String getBody() {
        return getMessage();
    }

    public void setBody(String body) {
        setMessage(body);
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTargetType() {
        return targetType != null ? targetType : "all";
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetUserId() {
        return targetUserId != null ? targetUserId : "";
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy != null ? createdBy : "Admin";
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatus() {
        if (status != null && !status.isEmpty()) {
            return status;
        }
        return sent ? "sent" : "pending";
    }

    public void setStatus(String status) {
        this.status = status;
        this.sent = "sent".equalsIgnoreCase(status);
    }

    public boolean isSent() {
        return "sent".equalsIgnoreCase(getStatus());
    }

    public void setSent(boolean sent) {
        this.sent = sent;
        this.status = sent ? "sent" : "pending";
    }

    public String getFormattedDate() {
        if (createdAt == null || createdAt == 0L) {
            return "Just now";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(createdAt));
        } catch (Exception e) {
            return "Recently";
        }
    }

    public String getTargetDisplay() {
        if ("all".equalsIgnoreCase(targetType)) {
            return "All Users";
        } else {
            if (targetUserId != null && !targetUserId.isEmpty()) {
                return "User: " + targetUserId;
            }
            return "Specific User";
        }
    }
}
