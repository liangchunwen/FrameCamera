package com.frame.camera.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

/**
 * Created by ${liangcw} on 2021/08/12.
 * <p>
 * 权限管理类
 */

public class PermissionsUtils {

    /**
     * 拍照需要的权限
     */
    public  static final String[] CAMERA_REQUEST = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    /**
     * 录像需要的权限
     */
    public  static final String[] VIDEO_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    /**
     * 录像定位的权限
     */
    public  static final String[] LOCATION_PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    /**
     * 拍照权限的请求code
     */
    public static final int CAMERA_REQUEST_CODE = 110;
    /**
     * 录像权限的请求code
     */
    public static final int VIDEO_REQUEST_CODE = 120;
    /**
     * 录像权限的请求code
     */
    public static final int LOCATION_REQUEST_CODE = 130;
    /**
     * 请求相机权限
     */
    private static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,CAMERA_REQUEST, CAMERA_REQUEST_CODE);
    }
    /**
     * 请求录像权限
     */
    private static void requestVideoRecordPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,VIDEO_PERMISSIONS, VIDEO_REQUEST_CODE);
    }
    /**
     * 请求定位权限
     */
    private static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,LOCATION_PERMISSIONS, LOCATION_REQUEST_CODE);
    }

    /**
     * 检查相机权限
     * @param activity
     * @return
     */
    public static boolean checkCameraPermission(Activity activity) {
        if (!hasPermissionGranted(activity,CAMERA_REQUEST)) {
            requestCameraPermission(activity);
            return false;
        } else {
            return true;
        }
    }
    /**
     * 检查录像权限
     * @param activity
     * @return
     */
    public static boolean checkVideoRecordPermission(Activity activity) {
        if (!hasPermissionGranted(activity,VIDEO_PERMISSIONS)) {
            requestVideoRecordPermission(activity);
            return false;
        } else {
            return true;
        }
    }
    /**
     * 检查定位权限
     * @param activity
     * @return
     */
    public static boolean checkLocationPermission(Activity activity) {
        if (!hasPermissionGranted(activity,LOCATION_PERMISSIONS)) {
            requestLocationPermission(activity);
            return false;
        } else {
            return true;
        }
    }

    /**
     * 检查权限组中的每个权限是否授权
     * @param activity
     * @param permissions
     * @return
     */
    private static  boolean hasPermissionGranted(Activity activity,String[] permissions){
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}
