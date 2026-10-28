package com.gameclub.zone.admin.model;

public class TopUpPackage {
    private String id;
    private String name;
    private long price;

    public TopUpPackage() {
    }

    public TopUpPackage(String id, String name, long price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "Package";
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }
}
