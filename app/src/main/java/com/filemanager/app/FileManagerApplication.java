package com.filemanager.app;

import android.app.Application;

import com.filemanager.app.network.RetrofitClient;

public class FileManagerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.initialize(this);
    }
}