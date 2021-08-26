package com.frame.camera.utils;

import android.annotation.SuppressLint;

import java.lang.reflect.Method;

/**
 * Created by liangcw on 2021/4/15 - 17:31
 */
public class SystemProperties {

    public static String get(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }

    public static void set(String key, String value) {
        setProperty(key, value);
    }

    private static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            @SuppressLint("PrivateApi")
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String)(get.invoke(c, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    private static void setProperty(String key, String val) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method set = c.getMethod("set", String.class, String.class);
            set.invoke(c, key, val);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
