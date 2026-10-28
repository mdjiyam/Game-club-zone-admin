package com.gameclub.zone.admin;

import android.app.Application;

public class AdminApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseManager.init(this);
    }
}
