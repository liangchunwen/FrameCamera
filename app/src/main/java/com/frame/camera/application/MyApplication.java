package com.frame.camera.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class MyApplication extends Application {
    private static final String TAG = MyApplication.class.getSimpleName() + ":CAMERA";
    private static MyApplication mInstance;
    public static SharedPreferences mSharedPreferences;
    public static SharedPreferences.Editor mEditor;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        initSharedPreferences();
    }

    private void initSharedPreferences() {
        mSharedPreferences = mInstance.getSharedPreferences("com.frame.camera", Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        mEditor.apply();
    }

    public static MyApplication getInstance() {
        return mInstance;
    }
}
