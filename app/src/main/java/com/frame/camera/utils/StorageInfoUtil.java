package com.frame.camera.utils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.frame.camera.application.MyApplication;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by liangcw on 2021/4/25 - 17:11
 */
public class StorageInfoUtil {
    private static final String TAG = "StorageInfoUtil_TAG";
    private static final String storageState = Environment.getExternalStorageState();
    private static final String mediaMounted = Environment.MEDIA_MOUNTED;
    private static final String path = Environment.getExternalStorageDirectory().getPath();

    //保留2位小数，不够在后边补0
    public static String saveDecimalDigit(String number) {
        return new DecimalFormat("#,##0.00").format(Double.valueOf(number));
    }

    public static double getInternalStorageAvailable() {
        double availableSize = 35.0;
        if (mediaMounted.equals(storageState)) {
            StatFs statFs = new StatFs(path);
            long blockSize = statFs.getBlockSizeLong();
            long availableCount = statFs.getAvailableBlocksLong();
            availableSize = (double) availableCount * blockSize / 1024 / 1024 / 1024;
        }

        return availableSize;//单位GB
    }

    public static double getInternalStorageTotal() {
        double totalSize = 50.0;
        if (mediaMounted.equals(storageState)) {
            StatFs statFs = new StatFs(path);
            long blockSize = statFs.getBlockSizeLong();
            long totalCount = statFs.getBlockCountLong();
            totalSize = (double) totalCount * blockSize / 1024 / 1024 / 1024;
        }

        return totalSize;//单位GB
    }

    public static double getExternalStorageAvailable() {
        double availableSize;
        String externalPath = FileUtils.getRootStorageDir(1);
        Log.d(TAG, "getExternalStorageAvailable-externalPath: " + externalPath);
        StatFs statFs = new StatFs(externalPath);
        long blockSize = statFs.getBlockSizeLong();
        long availableCount = statFs.getAvailableBlocksLong();
        availableSize = (double) availableCount * blockSize / 1024 / 1024 / 1024;

        return availableSize;//单位GB
    }

    public static double getExternalStorageTotal() {
        double totalSize;
        String externalPath = FileUtils.getRootStorageDir(1);
        Log.d(TAG, "getExternalStorageTotal-externalPath: " + externalPath);
        StatFs statFs = new StatFs(externalPath);
        long blockSize = statFs.getBlockSizeLong();
        long totalCount = statFs.getBlockCountLong();
        totalSize = (double) totalCount * blockSize / 1024 / 1024 / 1024;

        return totalSize;//单位GB
    }

    public static boolean isExternalStorageExist() {
        File[] files = MyApplication.getInstance().getExternalMediaDirs();
        if (files == null) {
            return false;
        } else {
            return files.length > 1;
        }
    }
}
