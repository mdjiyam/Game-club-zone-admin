package com.gameclub.zone.admin.model;

public class AppSettings {
    private String app_name;
    private String app_logo;
    private String notice;
    private String popup_text;
    private boolean show_popup;
    private String bkash_number;
    private String nagad_number;
    private String rocket_number;
    private long min_deposit = 30;
    private long min_withdraw = 50;
    private boolean show_participants = true;
    private String latest_version;
    private String apk_link;
    private String update_title;
    private String update_features;
    private boolean force_update;
    private String support_link;
    private String shop_link;
    private String how_to_add_money_link;
    private String how_to_play_link;
    private String how_to_get_room_id_link;

    public AppSettings() {
    }

    public String getApp_name() {
        return app_name != null ? app_name : "";
    }

    public void setApp_name(String app_name) {
        this.app_name = app_name;
    }

    public String getApp_logo() {
        return app_logo != null ? app_logo : "";
    }

    public void setApp_logo(String app_logo) {
        this.app_logo = app_logo;
    }

    public String getNotice() {
        return notice != null ? notice : "";
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getPopup_text() {
        return popup_text != null ? popup_text : "";
    }

    public void setPopup_text(String popup_text) {
        this.popup_text = popup_text;
    }

    public boolean isShow_popup() {
        return show_popup;
    }

    public void setShow_popup(boolean show_popup) {
        this.show_popup = show_popup;
    }

    public String getBkash_number() {
        return bkash_number != null ? bkash_number : "";
    }

    public void setBkash_number(String bkash_number) {
        this.bkash_number = bkash_number;
    }

    public String getNagad_number() {
        return nagad_number != null ? nagad_number : "";
    }

    public void setNagad_number(String nagad_number) {
        this.nagad_number = nagad_number;
    }

    public String getRocket_number() {
        return rocket_number != null ? rocket_number : "";
    }

    public void setRocket_number(String rocket_number) {
        this.rocket_number = rocket_number;
    }

    public long getMin_deposit() {
        return min_deposit;
    }

    public void setMin_deposit(long min_deposit) {
        this.min_deposit = min_deposit;
    }

    public long getMin_withdraw() {
        return min_withdraw;
    }

    public void setMin_withdraw(long min_withdraw) {
        this.min_withdraw = min_withdraw;
    }

    public boolean isShow_participants() {
        return show_participants;
    }

    public void setShow_participants(boolean show_participants) {
        this.show_participants = show_participants;
    }

    public String getLatest_version() {
        return latest_version != null ? latest_version : "";
    }

    public void setLatest_version(String latest_version) {
        this.latest_version = latest_version;
    }

    public String getApk_link() {
        return apk_link != null ? apk_link : "";
    }

    public void setApk_link(String apk_link) {
        this.apk_link = apk_link;
    }

    public String getUpdate_title() {
        return update_title != null ? update_title : "";
    }

    public void setUpdate_title(String update_title) {
        this.update_title = update_title;
    }

    public String getUpdate_features() {
        return update_features != null ? update_features : "";
    }

    public void setUpdate_features(String update_features) {
        this.update_features = update_features;
    }

    public boolean isForce_update() {
        return force_update;
    }

    public void setForce_update(boolean force_update) {
        this.force_update = force_update;
    }

    public String getSupport_link() {
        return support_link != null ? support_link : "";
    }

    public void setSupport_link(String support_link) {
        this.support_link = support_link;
    }

    public String getShop_link() {
        return shop_link != null ? shop_link : "";
    }

    public void setShop_link(String shop_link) {
        this.shop_link = shop_link;
    }

    public String getHow_to_add_money_link() {
        return how_to_add_money_link != null ? how_to_add_money_link : "";
    }

    public void setHow_to_add_money_link(String how_to_add_money_link) {
        this.how_to_add_money_link = how_to_add_money_link;
    }

    public String getHow_to_play_link() {
        return how_to_play_link != null ? how_to_play_link : "";
    }

    public void setHow_to_play_link(String how_to_play_link) {
        this.how_to_play_link = how_to_play_link;
    }

    public String getHow_to_get_room_id_link() {
        return how_to_get_room_id_link != null ? how_to_get_room_id_link : "";
    }

    public void setHow_to_get_room_id_link(String how_to_get_room_id_link) {
        this.how_to_get_room_id_link = how_to_get_room_id_link;
    }
}
