package com.frame.camera.utils;

import android.os.Environment;
import android.util.Log;

import com.frame.camera.application.MyApplication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ${liangcw} on 2021/08/12.
 */

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName() + ":CAMERA";
    private static File currentPreVideoFile;
    private static File currentVideoFile;
    private static File currentPictureFile;
    /**
     * 相片格式
     */
    public static final String PICTURE_FORMAT_PNG = ".png";
    public static final String PICTURE_FORMAT_JPG = ".jpg";
    /**
     * 视频格式
     */
    public static final String VIDEO_FORMAT = ".mp4";
    public static final String PRE_VIDEO_FORMAT = "_pre.mp4";
    public static final String JOIN_VIDEO_FORMAT = "_join.mp4";

    public static final int FILE_TYPE_IMAGE = 1;
    public static final int FILE_TYPE_VIDEO = 2;
    public static final int FILE_TYPE_AUDIO = 3;
    public static final int FILE_TYPE_DOCUMENT = 4;

    public static void setCurrentVideoFile(File currentVideoFile) {
        FileUtils.currentVideoFile = currentVideoFile;
    }

    public static File getCurrentVideoFile() {
        return currentVideoFile;
    }

    public static void setCurrentPictureFile(File currentPictureFile) {
        FileUtils.currentPictureFile = currentPictureFile;
    }

    public static File getCurrentPictureFile() {
        return currentPictureFile;
    }

    public static boolean isPreVideoExist() {
        return (currentPreVideoFile != null && currentPreVideoFile.exists());
    }

    public static void setCurrentPreVideoFile(File file) {
        currentPreVideoFile = file;
    }

    public static File getCurrentPreVideoFile() {
        return currentPreVideoFile;
    }

    // type:0 -> 内置SD, type:1 -> 外置SD
    public static String getRootStorageDir(int type) {
        String internalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d(TAG, "internalPath: " + internalPath);
        String rootPath = null;
        if (type == 1) {
            File[] files = MyApplication.getInstance().getExternalMediaDirs();
            if (files != null && files.length > 1) {
                for (File file : files) {
                    String path = file.getAbsolutePath();
                    Log.d(TAG, "path: " + path);
                    if (!path.startsWith(internalPath)) {
                        String externalPath = path.substring(0, path.indexOf("/Android"));
                        Log.d(TAG, "externalPath: " + externalPath);
                        rootPath = externalPath;
                        break;
                    }
                }
            }
        } else {
            rootPath = internalPath;
        }

        return rootPath;
    }

    public static String getFileDir(int type) {
        String currentDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (type == FILE_TYPE_IMAGE) {
            currentDir = MyApplication.getRootDir(MyApplication.pictureDir).getAbsolutePath();
        } else if (type == FILE_TYPE_VIDEO) {
            currentDir = MyApplication.getRootDir(MyApplication.videoDir).getAbsolutePath();
        } else if (type == FILE_TYPE_AUDIO) {
            currentDir = MyApplication.getRootDir(MyApplication.audioDir).getAbsolutePath();
        } else if (type == FILE_TYPE_DOCUMENT){
            currentDir = MyApplication.getRootDir(MyApplication.documentDir).getAbsolutePath();
        }
        String fileDir = currentDir + File.separator + new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }

        return fileDir;
    }

    public static String getMediaFileName(int type) {
        String fileName = null;
        if (type == FILE_TYPE_IMAGE) {
            fileName = "IMG_"
                            + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                            .format(new Date()) + ".jpg";
        } else if (type == FILE_TYPE_VIDEO) {
            fileName = "VID_"
                            + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                            .format(new Date()) + ".mp4";
        }

        return fileName;
    }

    /**
     * 创建制定目录下的图片文件
     *
     * @param fileName file
     * @return filePathName
     */
    public static File createPictureFile(String fileName) {
        return createMediaFile(getFileDir(FILE_TYPE_IMAGE), fileName);
    }

    /**
     * 创建制定目录下的视频文件
     *
     * @param fileName file
     * @return result
     */
    public static File createVideoFile(String fileName) {
        return createMediaFile(getFileDir(FILE_TYPE_VIDEO), fileName);
    }

    public static void setThumbnailFile(File file) {
        ThumbnailUtils.setLastMediaFilePath(file.getAbsolutePath());
        ThumbnailUtils.setVideoThumbType(!file.getName().endsWith(PICTURE_FORMAT_PNG)
                && !file.getName().endsWith(PICTURE_FORMAT_JPG));
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
        setThumbnailFile(filePath);

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
