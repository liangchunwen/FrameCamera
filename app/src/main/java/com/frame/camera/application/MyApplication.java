package com.frame.camera.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.frame.camera.R;
import com.frame.camera.utils.FileUtils;

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
    public static boolean isModeBtnClick = false;
    public static boolean isPreRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        initSharedPreferences();
        String internalPath = FileUtils.getRootStorageDir(0);
        Log.d(TAG, "internalPath: " + internalPath);
        initDirs(internalPath);
        String externalPath = FileUtils.getRootStorageDir(1);
        Log.d(TAG, "externalPath: " + externalPath);
        if (externalPath != null) {
            initDirs(externalPath);
            //有外置存储的时候文件默认保存在外置存储
            MyApplication.mEditor.putString("file_path_values", "1").apply();
        } else {
            //没有外置存储的时候文件默认保存在内部存储
            MyApplication.mEditor.putString("file_path_values", "0").apply();
        }
    }

    public static void initDirs(String rootPath) {
        String cameraDir = rootPath + File.separator + "DCIM/Camera";
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

    public static File getRootDir(File file) {
        String rootType = MyApplication.mSharedPreferences.getString("file_path_values", "0");
        Log.d(TAG, "rootType: " + rootType);
        String rootPath = null;
        if (rootType.equals("0")) {//内部存储
            rootPath = FileUtils.getRootStorageDir(0);
        } else {//外部存储
            rootPath = FileUtils.getRootStorageDir(1);
        }
        Log.d(TAG, "rootPath: " + rootPath);
        String currentPath = file.getAbsolutePath();
        Log.d(TAG, "currentPath-1: " + currentPath);
        currentPath = currentPath.replace(currentPath.substring(0, currentPath.indexOf("/DCIM/Camera")), rootPath);
        Log.d(TAG, "currentPath-2: " + currentPath);

        return new File(currentPath);
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
