package com.frame.camera.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ${liangcw} on 2021/08/12.
 */

public class FileUtils {
    public static final String TAG = FileUtils.class.getSimpleName() + ":CAMERA";
    /**
     * 相片格式
     */
    public static final String PICTURE_FORMAT_PNG = ".png";
    public static final String PICTURE_FORMAT_JPG = ".jpg";
    /**
     * 视频格式
     */
    public static final String VIDEO_FORMAT = ".mp4";

    private static final File storageDirectoryPath = Environment
            .getExternalStorageDirectory();
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String mediaFileDir = "DCIM/Camera" + File.separator + new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());

    public static String getMediaFileDir() {
        String mediaDir =  storageDirectoryPath.getAbsolutePath() + File.separator + mediaFileDir;
        File mediaFile = new File(mediaDir);
        if (!mediaFile.exists()) {
            if (!mediaFile.mkdirs()) {
                return null;
            }
        }

        return mediaDir;
    }

    public static String getMediaFileName(int type) {
        String fileName = null;
        if (type == MEDIA_TYPE_IMAGE) {
            fileName = "IMG_"
                            + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                            .format(new Date()) + ".jpg";
        } else if (type == MEDIA_TYPE_VIDEO) {
            fileName = "VID_"
                            + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                            .format(new Date()) + ".mp4";
        }

        return fileName;
    }

    /**
     * 创建制定目录下的图片文件
     *
     * @param fileName
     * @return filePathName
     */
    public static File createPictureFile(String fileName) {
        return createMediaFile(getMediaFileDir(), fileName);
    }

    /**
     * 创建制定目录下的视频文件
     *
     * @param
     * @return
     */
    public static File createVideoFile(String fileName) {
        return createMediaFile(getMediaFileDir(), fileName);
    }

    /**
     * getExternalFilesDir()提供的是私有的目录,不可见性，在app卸载后会被删除
     * <p>
     * getExternalCacheDir():提供外部缓存目录，是可见性的。
     *
     * @param dirName 目录
     * @param fileName 文件名
     * @return 返回文件路径
     */
    private static File createMediaFile(String dirName, String fileName) {
        File filePath = new File(dirName + File.separator + fileName);
        ThumbnailUtils.setLastMediaFilePath(filePath.getAbsolutePath());
        ThumbnailUtils.setVideoThumbType(!fileName.endsWith(PICTURE_FORMAT_PNG) && !fileName.endsWith(PICTURE_FORMAT_JPG));

        return filePath;
    }

    /**
     * 删除文件
     * 1. 单个文件
     * 2. 文件夹下的子文件夹和文件
     *
     * @param filePath 文件路径
     */
    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        //删除文件夹
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    if (childFile.isFile()) {
                        //删除文件
                        Log.d(TAG, "" + childFile.delete());
                    } else if (file.isDirectory()) {
                        //递归删除子文件夹
                        deleteFile(childFile.getAbsolutePath());
                    }
                }
            }
            //删除文件夹本身
            Log.d(TAG, "" + file.delete());
        } else {//删除文件
            if (file.isFile()) {
                Log.d(TAG, "" + file.delete());
            }
        }
    }
}
