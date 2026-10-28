package com.gameclub.zone.admin.model;

public class NotificationModel {
    private String id;
    private String title;
    private String body;
    private boolean active;
    private String target;

    public NotificationModel() {
        this.active = true;
        this.target = "all";
    }

    public NotificationModel(String id, String title, String body, boolean active, String target) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.active = active;
        this.target = target != null ? target : "all";
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

    public String getBody() {
        return body != null ? body : "";
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTarget() {
        return target != null ? target : "all";
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
