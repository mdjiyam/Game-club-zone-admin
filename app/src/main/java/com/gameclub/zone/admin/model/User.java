package com.gameclub.zone.admin.model;

import com.google.firebase.database.DataSnapshot;

public class User {
    private String uid;
    private String username;
    private String email;
    private String phone;
    private String number;
    private String status; // active, banned, deleted
    private long deposit;
    private long winning;

    public User() {
    }

    public User(String uid, String username, String email, String phone, String number, String status, long deposit, long winning) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.number = number;
        this.status = status;
        this.deposit = deposit;
        this.winning = winning;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username != null ? username : "No Name";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email != null ? email : "N/A";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayPhone() {
        if (number != null && !number.isEmpty()) return number;
        if (phone != null && !phone.isEmpty()) return phone;
        return "N/A";
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getStatus() {
        return status != null ? status : "active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDeposit() {
        return deposit;
    }

    public void setDeposit(long deposit) {
        this.deposit = deposit;
    }

    public long getWinning() {
        return winning;
    }

    public void setWinning(long winning) {
        this.winning = winning;
    }

    public static User fromSnapshot(DataSnapshot child) {
        if (child == null || !child.exists()) return null;
        User u = new User();
        u.setUid(child.getKey());

        // Username / Name
        String name = child.child("username").getValue(String.class);
        if (name == null || name.trim().isEmpty()) name = child.child("name").getValue(String.class);
        if (name == null || name.trim().isEmpty()) name = child.child("user_name").getValue(String.class);
        if (name == null || name.trim().isEmpty()) name = child.child("displayName").getValue(String.class);
        u.setUsername(name != null && !name.trim().isEmpty() ? name : "User_" + (child.getKey() != null && child.getKey().length() > 5 ? child.getKey().substring(0, 5) : ""));

        // Email
        String email = child.child("email").getValue(String.class);
        u.setEmail(email != null && !email.trim().isEmpty() ? email : "N/A");

        // Phone / Number / Mobile
        String phone = child.child("phone").getValue(String.class);
        if (phone == null || phone.trim().isEmpty()) phone = child.child("number").getValue(String.class);
        if (phone == null || phone.trim().isEmpty()) phone = child.child("phoneNumber").getValue(String.class);
        if (phone == null || phone.trim().isEmpty()) phone = child.child("mobile").getValue(String.class);
        u.setPhone(phone);
        u.setNumber(phone);

        // Status
        String status = child.child("status").getValue(String.class);
        if (status == null || status.trim().isEmpty()) {
            Boolean banned = child.child("banned").getValue(Boolean.class);
            if (banned == null) banned = child.child("is_banned").getValue(Boolean.class);
            if (Boolean.TRUE.equals(banned)) {
                status = "banned";
            } else {
                status = "active";
            }
        }
        u.setStatus(status.toLowerCase().trim());

        // Deposit / Balance
        long deposit = parseLongSafe(child.child("deposit").getValue());
        if (deposit == 0) {
            deposit = parseLongSafe(child.child("balance").getValue());
        }
        u.setDeposit(deposit);

        // Winning
        long winning = parseLongSafe(child.child("winning").getValue());
        if (winning == 0) {
            winning = parseLongSafe(child.child("won").getValue());
        }
        u.setWinning(winning);

        return u;
    }

    private static long parseLongSafe(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return (long) Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
