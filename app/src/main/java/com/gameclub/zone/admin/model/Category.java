package com.gameclub.zone.admin.model;

public class Category {
    private String id;
    private String name;
    private String img;
    private String rules;

    public Category() {
    }

    public Category(String id, String name, String img, String rules) {
        this.id = id;
        this.name = name;
        this.img = img;
        this.rules = rules;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "Unnamed Category";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg() {
        return img != null ? img : "";
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getRules() {
        return rules != null ? rules : "";
    }

    public void setRules(String rules) {
        this.rules = rules;
    }
}
