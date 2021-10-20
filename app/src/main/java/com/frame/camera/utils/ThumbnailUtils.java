package com.frame.camera.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.frame.camera.application.MyApplication;
import com.frame.camera.widgets.CircularDrawable;

import java.io.File;

public class ThumbnailUtils {
    public static final String TAG = ThumbnailUtils.class.getSimpleName() + ":CAMERA";

    public static void updateThumbnail(Bitmap thumbnailBitmap, ImageView thumbnailView) {
        if (thumbnailBitmap != null && !thumbnailBitmap.isRecycled()) {
            if (thumbnailView != null) {
                Drawable mThumbnailDrawable = new CircularDrawable(thumbnailBitmap, thumbnailView);
                thumbnailView.setEnabled(true);
                thumbnailView.setImageDrawable(mThumbnailDrawable);
                thumbnailView.setBackgroundColor(0x0000);
                thumbnailView.setVisibility(View.VISIBLE);
                thumbnailView.invalidate();
            }
            thumbnailBitmap.recycle();
        }
    }

    public static void gotoGallery(Activity activity, boolean isVideo, String filePath) {
        Uri uri;
        try {
            Log.i(TAG, "gotoGallery-filePath:" + filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", new File(filePath));
            } else {
                uri = Uri.fromFile(new File(filePath));
            }
            if (isVideo) {
                intent.setDataAndType(uri, "video/*");
            } else {
                intent.setDataAndType(uri, "image/*");
            }
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            activity.startActivity(intent);
        } catch (Exception ex) {
            Log.i(TAG, "gotoGallery-ex:" + ex);
            ex.printStackTrace();
        }
    }

    public static Bitmap getImageThumbnail(String imagePath) {
        Log.i(TAG, "getImageThumbnail-imagePath:" + imagePath);
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inJustDecodeBounds = false;
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / 50;
        int beHeight = h / 50;
        int be;
        be = Math.min(beWidth, beHeight);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = android.media.ThumbnailUtils.extractThumbnail(bitmap, 45, 45,
                android.media.ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        return bitmap;
    }

    public static Bitmap getVideoThumbnail(String videoPath) {
        Log.i(TAG, "getVideoThumbnail-videoPath:" + videoPath);
        Bitmap bitmap;
        bitmap = android.media.ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MICRO_KIND);
        bitmap = android.media.ThumbnailUtils.extractThumbnail(bitmap, 45, 45,
                android.media.ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    public static void setVideoThumbType(boolean isVideo) {
        MyApplication.mEditor.putBoolean("video_thumb_type", isVideo);
        MyApplication.mEditor.apply();
    }

    public static boolean getVideoThumbType() {
        return MyApplication.mSharedPreferences.getBoolean("video_thumb_type", false);
    }

    public static void setLastMediaFilePath(String path) {
        MyApplication.mEditor.putString("last_media_file_path", path);
        MyApplication.mEditor.apply();
    }

    public static String getLastMediaFilePath() {
        return MyApplication.mSharedPreferences.getString("last_media_file_path", null);
    }
}
