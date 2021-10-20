package com.frame.camera.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.frame.camera.R;
import com.frame.camera.activity.CameraActivity;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService:CAMERA";
    private NotificationCompat.Builder builder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ForegroundService-onCreate()");
        String NOTIFICATION_CHANNEL_ID = "com.frame.camera";
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "CameraForegroundService";
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
            builder.setSmallIcon(R.drawable.ic_camera_app)  // the status icon
                    .setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentText(getString(R.string.app_name))  // the contents of the entry
                    .setAutoCancel(true)
                    .build();

            Intent mIntent = new Intent(this, CameraActivity.class);
            mIntent.setAction(Intent.ACTION_MAIN);
            mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

        } else {
            builder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
            builder.setSmallIcon(R.drawable.ic_camera_app)  // the status icon
                    .setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentText(getString(R.string.app_name))  // the contents of the entry
                    .setAutoCancel(true)
                    .build();

            Intent mIntent = new Intent(this, CameraActivity.class);
            mIntent.setAction(Intent.ACTION_MAIN);
            mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

        }
        notification = builder.build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ForegroundService-onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ForegroundService-onDestroy()");
        if (builder != null) {
            builder.clearActions();
        }
    }
}
