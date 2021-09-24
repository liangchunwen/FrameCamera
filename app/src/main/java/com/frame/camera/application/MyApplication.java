package com.frame.camera.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.frame.camera.R;

import java.io.File;

public class MyApplication extends Application {
    private static final String TAG = MyApplication.class.getSimpleName() + ":CAMERA";
    private static MyApplication mInstance;
    public static File pictureDir;
    public static File videoDir;
    public static File audioDir;
    public static File documentDir;
    public static SharedPreferences mSharedPreferences;
    public static SharedPreferences.Editor mEditor;
    public static boolean isAppBtnClick = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        initDirs();
        initSharedPreferences();
    }

    private void initDirs() {
        String cameraDir = Environment.getExternalStorageDirectory() + File.separator + "DCIM/Camera";
        pictureDir = new File(cameraDir + File.separator + mInstance.getString(R.string.picture));
        if (!pictureDir.exists()) {
            Log.d(TAG, "create picture dir: " + pictureDir.mkdirs());
        }
        videoDir = new File(cameraDir + File.separator + mInstance.getString(R.string.video));
        if (!videoDir.exists()) {
            Log.d(TAG, "create video dir: " + videoDir.mkdirs());
        }
        audioDir = new File(cameraDir + File.separator + mInstance.getString(R.string.audio));
        if (!audioDir.exists()) {
            Log.d(TAG, "create audio dir: " + audioDir.mkdirs());
        }
        documentDir = new File(cameraDir + File.separator + mInstance.getString(R.string.document));
        if (!documentDir.exists()) {
            Log.d(TAG, "create document dir: " + documentDir.mkdirs());
        }
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
