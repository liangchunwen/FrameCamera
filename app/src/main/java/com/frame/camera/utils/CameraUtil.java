package com.frame.camera.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.WindowManager;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by liangcw on 2020/11/18 - 21:07
 */
public class CameraUtil {
    private static final String TAG = "CameraUtil:Intelli";
    public static final String IMAGE_FORMAT = "'IMG'_yyyyMMdd_HHmmss_S";
    public static final String VIDEO_FORMAT = "'VID'_yyyyMMdd_HHmmss_S";
    private static final String KEY_STEREO_REFOCUS_PICTURE = "camera_refocus";
    private static final int IS_STEREO_PICTURE = 1;
    private static final int INVALID_DURATION = -1;
    private static final int FILE_ERROR = -2;

    /**
     * create a content values
     * @param pictureWidth the width of content values.
     * @param pictureHeight the height of content valuse.
     * @return the content values from the data.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static ContentValues createPhotoValues(byte[] data, String title, long dateTaken, String path, int pictureWidth, int pictureHeight) {
        ContentValues values = new ContentValues();
        String fileName = title + ".jpg";
        String mime = "image/jpeg";

        values.put(MediaStore.Images.ImageColumns.DATE_ADDED, dateTaken);
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateTaken / 1000);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, mime);
        values.put(MediaStore.Images.ImageColumns.WIDTH, pictureWidth);
        values.put(MediaStore.Images.ImageColumns.HEIGHT, pictureHeight);
        Log.d("Camera2BasicFragment", "data.length: " + data.length/1000);
        values.put(MediaStore.Images.ImageColumns.SIZE, data.length);

        values.put(KEY_STEREO_REFOCUS_PICTURE, IS_STEREO_PICTURE);

        //values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        values.put(MediaStore.Images.ImageColumns.DATA, path);

        /*
        Location location = mICameraContext.getLocation();
        if (location != null) {
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location.getLongitude());
        }
        */

        Log.d(TAG, "createContentValues, width : " + pictureWidth + ",height = " +
                pictureHeight/* + ",orientation = " + orientation*/);

        return values;
    }

    public static String createFileName(boolean isVideo, String  title) {
        String fileName = title + ".jpg";
        if (isVideo) {
            fileName = title + ".mp4";
        }
        Log.d(TAG, "[createFileName] + fileName = " + fileName);
        return fileName;
    }

    public static String createFileTitle(boolean isVideo, long dateTaken) {
        SimpleDateFormat format;
        Date date = new Date(dateTaken);
        if (isVideo) {
            format = new SimpleDateFormat("'VID'_yyyyMMdd_HHmmss", Locale.CHINESE);
        } else {
            format = new  SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss", Locale.CHINESE);
        }
        return format.format(date);
    }

    public static long getDuration(String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(fileName);
            return Long.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (IllegalArgumentException e) {
            return INVALID_DURATION;
        } catch (RuntimeException e) {
            return FILE_ERROR;
        } finally {
            retriever.release();
        }
    }

    /*
     * 1: 宽
     * 2：高
     */
    public static String getVideoWH(int type, String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(fileName);
            if (type == 1) {
                return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); //宽
            } else if (type == 2) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); //高
            }
        } catch (IllegalArgumentException e) {
            return "0";
        } catch (RuntimeException e) {
            return "-1";
        } finally {
            retriever.release();
        }

        return "";
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static ContentValues createVideoValues(String mTitle, String mFilePath, long mDateTaken, int width, int height) {
        ContentValues values = new ContentValues();
        String mFileName = CameraUtil.createFileName(true, mTitle);
        long duration = getDuration(mFilePath);
        String mime = "video/mp4";
        values.put(MediaStore.Video.Media.DURATION, duration);
        values.put(MediaStore.Video.Media.TITLE, mTitle);
        values.put(MediaStore.Video.Media.DISPLAY_NAME, mFileName);
        values.put(MediaStore.Video.Media.DATE_TAKEN, mDateTaken);
        values.put(MediaStore.Video.Media.MIME_TYPE, mime);
        values.put(MediaStore.Video.Media.DATA, mFilePath);
        values.put(MediaStore.Video.Media.WIDTH, width);
        values.put(MediaStore.Video.Media.HEIGHT, height);
        values.put(MediaStore.Video.Media.SIZE, new File(mFilePath).length());

        return values;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
                                                int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels < 0) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength < 0) ? 128 :
                (int) Math.min(Math.floor(w / minSideLength),
                        Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if (maxNumOfPixels < 0 && minSideLength < 0) {
            return 1;
        } else if (minSideLength < 0) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }


    /**
     * Compute the sample size as a function of minSideLength and
     * maxNumOfPixels. minSideLength is used to specify that minimal width or
     * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
     * pixels that is tolerable in terms of memory usage. The function returns a
     * sample size based on the constraints.
     * <p>
     * Both size and minSideLength can be passed in as -1 which indicates no
     * care of the corresponding constraint. The functions prefers returning a
     * sample size that generates a smaller bitmap, unless minSideLength = -1.
     * <p>
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way. For example,
     * BitmapFactory downsamples an image by 2 even though the request is 3. So
     * we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(BitmapFactory.Options options,
                                        int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    public static Point getDisplaySize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        return point;
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
                    options);
            if (options.mCancel || options.outWidth == -1
                    || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = computeSampleSize(
                    options, -1, maxNumOfPixels);
            options.inJustDecodeBounds = false;

            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
                    options);
        } catch (OutOfMemoryError ex) {
            Log.d(TAG, "Got oom exception " + ex);
            return null;
        }
    }

    /**
     * 保证预览方向正确
     *
     * @param activity activity
     * @param cameraId cameraId
     * @param camera   camera
     */
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        //设置角度
        camera.setDisplayOrientation(result);
    }

    /**
     * 降序
     */
    private static class CameraDropSizeComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width < rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    /**
     * 升序
     */
    private static class CameraAscendSizeComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    /**
     * 根据 宽度和高度找到是否有相等的 尺寸  如果没有 就获取最小的 值
     * @param list list
     * @param th 高度
     * @param minWidth 宽度
     * @return size
     */
    public static Camera.Size getPicPreviewSize(List<Camera.Size> list, int th, int minWidth){
        Collections.sort(list, new CameraAscendSizeComparator());

        int i = 0;
        for(int x=0;x<list.size();x++){
            Camera.Size s = list.get(x);
            // camera 中的宽度和高度 相反 因为测试板子原因 这里暂时 替换 && 为 ||
            if((s.width == th) && (s.height == minWidth)){
                i = x;
                break;
            }
        }
        //如果没找到，就选最小的size 0
        return list.get(i);
    }

    private static boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        return Math.abs(r - rate) <= 0.03;
    }

    public static Camera.Size getPropPictureSize(List<Camera.Size> list, float th, int minWidth){
        Collections.sort(list, new CameraAscendSizeComparator());

        int i = 0;
        for(Camera.Size s:list){
            if((s.width >= minWidth) && equalRate(s, th)){
                Log.d(TAG, "PictureSize : w = " + s.width + "h = " + s.height);
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;//如果没找到，就选最小的size
        }
        return list.get(i);
    }

    /**
     * Rotates the bitmap by the specified degree. If a new bitmap is created,
     * the original bitmap is recycled.
     */
    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }


    /**
     * Rotates and/or mirrors the bitmap. If a new bitmap is created, the
     * original bitmap is recycled.
     */
    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees != 0 || mirror) && b != null) {
            Matrix m = new Matrix();
            // Mirror first.
            // horizontal flip + rotation = -rotation + horizontal flip
            if (mirror) {
                m.postScale(-1, 1);
                degrees = (degrees + 360) % 360;
                if (degrees == 0 || degrees == 180) {
                    m.postTranslate(b.getWidth(), 0);
                } else if (degrees == 90 || degrees == 270) {
                    m.postTranslate(b.getHeight(), 0);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + degrees);
                }
            }
            if (degrees != 0) {
                // clockwise
                m.postRotate(degrees,
                        (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            }

            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }
}
