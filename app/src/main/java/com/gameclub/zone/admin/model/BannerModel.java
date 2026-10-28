package com.gameclub.zone.admin.model;

public class BannerModel {
    private String id;
    private String img;
    private String link;

    public BannerModel() {
    }

    public BannerModel(String id, String img, String link) {
        this.id = id;
        this.img = img;
        this.link = link;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImg() {
        return img != null ? img : "";
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getLink() {
        return link != null ? link : "";
    }

    public void setLink(String link) {
        this.link = link;
    }
}
