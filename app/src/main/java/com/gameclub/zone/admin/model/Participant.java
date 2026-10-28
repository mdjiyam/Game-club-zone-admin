package com.gameclub.zone.admin.model;

public class Participant {
    private String id;
    private String ign;
    private String joinedBy;
    private long rewardAmount;

    public Participant() {
    }

    public Participant(String id, String ign, String joinedBy, long rewardAmount) {
        this.id = id;
        this.ign = ign;
        this.joinedBy = joinedBy;
        this.rewardAmount = rewardAmount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIgn() {
        return ign != null ? ign : "Unknown";
    }

    public void setIgn(String ign) {
        this.ign = ign;
    }

    public String getJoinedBy() {
        return joinedBy != null ? joinedBy : "";
    }

    public void setJoinedBy(String joinedBy) {
        this.joinedBy = joinedBy;
    }

    public long getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(long rewardAmount) {
        this.rewardAmount = rewardAmount;
    }
}
