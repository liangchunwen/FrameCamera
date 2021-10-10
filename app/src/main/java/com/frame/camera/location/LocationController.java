package com.frame.camera.location;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;

/**
 * Created by liangcw on 2021/3/25 - 9:05
 */
public class LocationController {
    private final static String TAG = "LocationController:CAMERA";
    private final static int TWO_MINUTES = 2 * 60 * 1000;//2分钟
    private final static int MIN_TIME = 2*1000;//2秒钟
    private final static int MIN_DISTANCE = 0;//1米
    private LocationManager mLocationManager = null;
    private OnLocationListener mOnLocationListener;
    private final Context mContext;
    private float mSnr = 0;
    private int mCount = 0;


    public interface OnLocationListener {
        public void onLocation(Location location);
    }

    public void setOnLocationListener(OnLocationListener onLocationListener) {
        mOnLocationListener = onLocationListener;
    }

    public LocationController(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        setupLocationAcquired();
    }

    private final GpsStatus.Listener gpsStatusListener = event -> {
        Log.d(TAG, "===================================");
        Log.d(TAG, "event: " + event);
        //卫星状态改变
        //获取卫星颗数的默认最大值
        //获取所有的卫星
        //卫星颗数统计
        //卫星的信噪比
        if (mLocationManager == null) {
            return;
        }

        if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {//获取当前状态
            @SuppressLint("MissingPermission")
            GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
            //获取卫星颗数的默认最大值
            int maxSatellites = gpsStatus.getMaxSatellites();
            //获取所有的卫星
            Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
            //卫星颗数统计
            mCount = 0;
            //统计总信噪比前先清零
            mSnr = 0;
            StringBuilder sb = new StringBuilder();
            while (iters.hasNext() && mCount <= maxSatellites) {
                mCount++;
                GpsSatellite s = iters.next();
                //卫星的信噪比
                float snr = s.getSnr();
                //统计所有卫星的总信噪比
                mSnr += snr;
                sb.append("第").append(mCount).append("颗").append("：").append(snr).append("\n");
            }
            Log.d(TAG, sb.toString());
        } else {
            mSnr = 0;
        }
        Log.d(TAG, "mCount: " + mCount + "   mSnr: " + mSnr);
        //设置可以获取到的卫星总信噪比
        setGpsSnr(mSnr);
    };


    @SuppressLint("MissingPermission")
    private void setupLocationAcquired() {
        mLocationManager.removeGpsStatusListener(gpsStatusListener);
        mLocationManager.addGpsStatusListener(gpsStatusListener);

        getLastKnownLocation();

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, gpsLocationListener);
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        criteria.setAltitudeRequired(false);//无海拔要求
        // criteria.setBearingRequired(false);//无方位要求
        criteria.setCostAllowed(false);//允许产生资费
        // criteria.setPowerRequirement(Criteria.POWER_LOW);//低功耗
        // 获取最佳服务对象
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider != null) {
            mLocationManager.getLastKnownLocation(provider);
        }
    }

    public void setGpsSnr(float snr) {
        mSnr = snr;
    }

    public float getGpsSnr() {
        return mSnr;
    }

    public void startLocation(Activity activity) {
        addListener(activity);
    }

    public void stopLocation() {
        removeListener();
    }

    private void addListener(Activity activity) {
        if (mLocationManager != null) {
            setupLocationAcquired();
        }
    }

    private void removeListener() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(gpsLocationListener);
        }
    }

    LocationListener gpsLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "gps-lat: " + location.getLatitude() + "   lng: " + location.getLongitude());
            if (mOnLocationListener != null) {
                mOnLocationListener.onLocation(location);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    /**
     * 判断哪一种位置读取方式比当前的位置修复更加的准确
     *
     * @param location            新位置
     * @param currentBestLocation 当前的位置，此位置需要和新位置进行比较
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        //检查最新的位置是比较新还是比较旧
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        //如果当前的位置信息来源于两分钟前，使用最新位置，
        // 因为用户可能移动了
        if (isSignificantlyNewer) {
            return true;
            //如果最新的位置也来源于两分钟前，那么此位置会更加的不准确。
        } else if (isSignificantlyOlder) {
            return false;
        }

        //检查最新的位置信息是更加的准确还是不准确
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        //检查旧的位置和新的位置是否来自同一个provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        //结合及时性和精确度，决定位置信息的质量
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else {
            return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
        }
    }

    /*** 检查两个提供者是否是同一个*/
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
