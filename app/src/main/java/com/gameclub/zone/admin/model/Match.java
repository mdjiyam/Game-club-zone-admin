package com.gameclub.zone.admin.model;

public class Match {
    private String dbKey;
    private String title;
    private String time;
    private long timestamp;
    private String type;
    private String map;
    private long entry;
    private long total_prize;
    private long per_kill;
    private int total;
    private String category;
    private String categoryId;
    private String status; // Upcoming, Ongoing, Finished
    private String prize_desc;
    private int joined;
    private String room_id;
    private String room_pass;
    private String winner;
    private String adminUid;
    private String adminName;

    public Match() {
    }

    public String getDbKey() {
        return dbKey;
    }

    public void setDbKey(String dbKey) {
        this.dbKey = dbKey;
    }

    public String getTitle() {
        return title != null ? title : "Match";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time != null ? time : "";
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type != null ? type : "Solo";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMap() {
        return map != null ? map : "Bermuda";
    }

    public void setMap(String map) {
        this.map = map;
    }

    public long getEntry() {
        return entry;
    }

    public void setEntry(long entry) {
        this.entry = entry;
    }

    public long getTotal_prize() {
        return total_prize;
    }

    public void setTotal_prize(long total_prize) {
        this.total_prize = total_prize;
    }

    public long getPer_kill() {
        return per_kill;
    }

    public void setPer_kill(long per_kill) {
        this.per_kill = per_kill;
    }

    public int getTotal() {
        return total > 0 ? total : 48;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getCategory() {
        if (category != null && !category.isEmpty()) return category;
        return categoryId != null ? categoryId : "";
    }

    public void setCategory(String category) {
        this.category = category;
        this.categoryId = category;
    }

    public String getCategoryId() {
        return getCategory();
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        this.category = categoryId;
    }

    public String getStatus() {
        return status != null ? status : "Upcoming";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrize_desc() {
        return prize_desc != null ? prize_desc : "";
    }

    public void setPrize_desc(String prize_desc) {
        this.prize_desc = prize_desc;
    }

    public int getJoined() {
        return joined;
    }

    public void setJoined(int joined) {
        this.joined = joined;
    }

    public String getRoom_id() {
        return room_id != null ? room_id : "";
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public String getRoom_pass() {
        return room_pass != null ? room_pass : "";
    }

    public void setRoom_pass(String room_pass) {
        this.room_pass = room_pass;
    }

    public String getWinner() {
        return winner != null ? winner : "";
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getAdminUid() {
        return adminUid != null ? adminUid : "";
    }

    public void setAdminUid(String adminUid) {
        this.adminUid = adminUid;
    }

    public String getAdminName() {
        return adminName != null && !adminName.trim().isEmpty() ? adminName : "All Admins";
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
}
