package com.frame.camera.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.frame.camera.R;
import com.frame.camera.activity.SettingsActivity;
import com.frame.camera.application.MyApplication;
import com.frame.camera.databinding.FragmentCameraBinding;
import com.frame.camera.location.LocationController;
import com.frame.camera.sound.ISoundPlayback;
import com.frame.camera.utils.CameraSoundUtils;
import com.frame.camera.utils.CameraUtil;
import com.frame.camera.utils.DecimalUtil;
import com.frame.camera.utils.FileUtils;
import com.frame.camera.utils.FocusUtils;
import com.frame.camera.utils.JoinVideoUtils;
import com.frame.camera.utils.StorageInfoUtil;
import com.frame.camera.utils.SystemProperties;
import com.frame.camera.utils.ThumbnailUtils;
import com.frame.camera.utils.TrimVideoUtils;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.markers.AutoFocusMarker;
import com.otaliastudios.cameraview.markers.AutoFocusTrigger;
import com.otaliastudios.cameraview.markers.DefaultAutoFocusMarker;
import com.otaliastudios.cameraview.size.Size;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CameraFragment extends Fragment implements View.OnClickListener, LocationController.OnLocationListener {
    private static final String TAG = "CameraFragment:CAMERA";
    private static final String VIDEO_KEY_DOWN_ACTION = "com.runbo.video.key.down";
    private static final String CAMERA_KEY_DOWN_ACTION = "com.runbo.camera.key.down";
    private static final int UPDATE_THUMB_UI = 0;
    private static boolean isVideoKeyDown = false;
    private static boolean isVideoRecording = false;
    private MyTimerTask mMyTimerTask;
    private Timer mTimer;

    private LocationController mLocationController;
    private FragmentCameraBinding binding;
    private CameraView mCameraView;
    private MyReceiver myReceiver;
    //private String mCurrentPath;
    private String mCurrentTitle;
    private long mCurrentTime;
    private Size mCurrentVideoSize;

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_THUMB_UI) {
                updateThumbBtnUI();
                updateGallery(true, null);
            }
        }
    };

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action: " + action);
            switch (action) {
                case VIDEO_KEY_DOWN_ACTION:
                    MyApplication.isModeBtnClick = false;
                    //???????????????????????????????????????????????????
                    if (mCameraView.getMode() == Mode.PICTURE) {
                        isVideoKeyDown = true;
                        onModeSwitch("video");
                        MyApplication.isPreRecording = false;
                        startVideoRecording();
                    } else {
                        if (getPreVideoValue() > 0) {//VIDEO?????????,getPreVideoValue() > 0,????????????????????????,???????????????
                            if (MyApplication.isPreRecording) {//?????????????????????????????????????????????
                                MyApplication.isPreRecording = false;
                                binding.shutImageView.setImageResource(R.drawable.ic_video_recording_background);
                                binding.shutImageView.invalidate();
                                CameraSoundUtils.playSound(getActivity(), ISoundPlayback.START_VIDEO_RECORDING);
                                showPreRecordingTips();
                                releasePreRecordingTimer();
                            } else {
                                if (isVideoRecording) {//???????????????????????????????????????
                                    stopVideoRecording();
                                }
                            }
                        } else {
                            MyApplication.isPreRecording = false;
                            if (isVideoRecording) {
                                stopVideoRecording();
                            } else {
                                startVideoRecording();
                            }
                        }
                    }
                    break;
                case CAMERA_KEY_DOWN_ACTION:
                    if (getPreVideoValue() > 0) {
                        if (MyApplication.isPreRecording) {
                            MyApplication.isModeBtnClick = true;
                            releasePreRecordingTimer();
                            stopVideoRecording();
                            MyApplication.isPreRecording = false;
                            showPreRecordingTips();
                            onModeSwitch("camera");
                        }
                    } else {
                        if (!isVideoRecording) {
                            //???????????????????????????????????????????????????
                            if (mCameraView.getMode() == Mode.VIDEO) {
                                onModeSwitch("camera");
                            }
                        }
                    }
                    takePicture();
                    break;
            }
        }
    }

    private void registerReceiver() {
        if (myReceiver != null) {
            unregisterReceiver();
        }
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VIDEO_KEY_DOWN_ACTION);
        filter.addAction(CAMERA_KEY_DOWN_ACTION);
        requireActivity().registerReceiver(myReceiver, filter);
    }

    private void unregisterReceiver() {
        if (myReceiver != null) {
            requireActivity().unregisterReceiver(myReceiver);
            myReceiver = null;
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private boolean isKeyDownStart() {
        boolean start = requireActivity().getIntent().getBooleanExtra("key_down_start", false);
        Log.d(TAG, "isKeyDownStart():key down start: " + start);
        return start;
    }

    private boolean isKeyStartVideo() {
        boolean isVideoMode = requireActivity().getIntent().getStringExtra("camera_mode").equals("video");
        Log.d(TAG, "isKeyStartVideo():key down start: " + isVideoMode);

        return isVideoMode;
    }

    private void resetKeyDownFlag() {
        requireActivity().getIntent().putExtra("key_down_start", false);
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onLocation(Location location) {
        if (mCameraView != null && location != null) {
            mCameraView.setLocation(location);
            String lng = DecimalUtil.saveDecimalDigit(String.valueOf(location.getLongitude()));
            String lat = DecimalUtil.saveDecimalDigit(String.valueOf(location.getLatitude()));
            binding.lngLatTv.setText(String.format(getString(R.string.lng_lat), lng, lat));
        }
    }

    private void setPoliceWaterMark() {
        String police_id = SystemProperties.get("persist.policeman.id", "");
        String police_name = SystemProperties.get("persist.policeman.name", "");
        String device_id = SystemProperties.get("persist.device.id", "");
        String company_id = SystemProperties.get("persist.company.id", "");
        String company_name = SystemProperties.get("persist.company.name", "");
        Log.d(TAG, "police_id: " + police_id);
        Log.d(TAG, "police_name: " + police_name);
        Log.d(TAG, "device_id: " + device_id);
        Log.d(TAG, "company_id: " + company_id);
        Log.d(TAG, "company_name: " + company_name);

        binding.policeIdTv.setText(String.format(getString(R.string.police_id), police_id));
        binding.policeNameTv.setText(String.format(getString(R.string.police_name), police_name));
        binding.deviceIdTv.setText(String.format(getString(R.string.device_id), device_id));
        binding.companyIdTv.setText(String.format(getString(R.string.company_id), company_id));
        binding.companyNameTv.setText(String.format(getString(R.string.company_name), company_name));
        binding.lngLatTv.setText(String.format(getString(R.string.lng_lat), "", ""));
    }

    private void showPreRecordingTips() {
        requireActivity().runOnUiThread(() -> {
            if (mCameraView.getMode() == Mode.VIDEO) {
                if (MyApplication.isPreRecording) {
                    binding.preVideoTv.setVisibility(View.VISIBLE);
                } else {
                    binding.preVideoTv.setVisibility(View.GONE);
                }
            } else {
                binding.preVideoTv.setVisibility(View.GONE);
            }
        });
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCameraView = binding.cameraView;
        if (isKeyDownStart()) {
            onModeSwitch(isKeyStartVideo()? "video" : "camera");
        }
        FocusUtils.initFocus(getActivity(), mCameraView);
        mCameraView.setAutoFocusMarker(autoFocusMarker);
        mCameraView.addCameraListener(cameraListener);
        mCameraView.addFrameProcessor(frameProcessor);
        binding.modeSwitchImageView.setOnClickListener(this);
        binding.shutImageView.setOnClickListener(this);
        binding.thumbImageView.setOnClickListener(this);
        binding.cameraSwitchImageView.setOnClickListener(this);
        binding.flashImageView.setOnClickListener(this);
        binding.settingsImageView.setOnClickListener(this);

        //??????DT951???RunboZ1??????????????????????????????
        String custom_version = SystemProperties.get("ro.custom.build.version", "");
        Log.d(TAG, "custom_version: " + custom_version);
        binding.watermarkInfoRl.setVisibility(View.VISIBLE);
        setPoliceWaterMark();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void savePictureData(PictureResult result) {
        byte[] bytes = result.getData();
        FileOutputStream output = null;
        mCurrentTime = System.currentTimeMillis();
        mCurrentTitle = CameraUtil.createFileTitle(false, mCurrentTime);
        String fileName = CameraUtil.createFileName(false, mCurrentTitle);
        File mPictureFile = FileUtils.createPictureFile(fileName);
        FileUtils.setCurrentPictureFile(mPictureFile);
        updateGallery(false, bytes);
        try {
            // save to file
            output = new FileOutputStream(mPictureFile);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                    updateThumbBtnUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateThumbBtnUI() {
        String filePath = ThumbnailUtils.getLastMediaFilePath();
        Log.d(TAG, "updateThumbBtnUI()-filePath: " + filePath);
        Bitmap lastThumb;
        if (ThumbnailUtils.getVideoThumbType()) {
            lastThumb = ThumbnailUtils.getVideoThumbnail(filePath);
        } else {
            lastThumb = ThumbnailUtils.getImageThumbnail(filePath);
        }
        ThumbnailUtils.updateThumbnail(lastThumb, binding.thumbImageView);
    }

    private void updateModeBtnUI(Mode mode) {
        if (Mode.PICTURE == mode) {
            binding.shutImageView.setImageResource(R.drawable.ic_photo_btn_background);
            binding.shutImageView.invalidate();
            binding.modeSwitchImageView.setImageResource(R.drawable.ic_video_mode_unselected);
            binding.modeSwitchImageView.invalidate();
            binding.thumbImageView.setEnabled(true);
        } else if (Mode.VIDEO == mode) {
            binding.shutImageView.setImageResource(R.drawable.ic_video_btn_background);
            binding.shutImageView.invalidate();
            binding.modeSwitchImageView.setImageResource(R.drawable.ic_photo_mode_unselected);
            binding.modeSwitchImageView.invalidate();
            binding.thumbImageView.setEnabled(!MyApplication.isPreRecording);
        }
    }

    FrameProcessor frameProcessor = frame -> {
        //byte[] bytes = frame.getData();
    };

    AutoFocusMarker autoFocusMarker = new DefaultAutoFocusMarker() {
        @Override
        public void onAutoFocusStart(@NonNull @NotNull AutoFocusTrigger trigger, @NonNull @NotNull PointF point) {
            FocusUtils.startFocus(point.x, point.y);
            mCameraView.setPlaySounds(true);
        }

        @Override
        public void onAutoFocusEnd(@NonNull @NotNull AutoFocusTrigger trigger, boolean successful, @NonNull @NotNull PointF point) {
            FocusUtils.focusSuccess();
        }

        @NotNull
        @Override
        public View onAttach(@NonNull @NotNull Context context, @NonNull @NotNull ViewGroup container) {
            return FocusUtils.getFocusView(getActivity());
        }
    };

    private int getPreVideoValue() {
        String preValue = MyApplication.mSharedPreferences.getString("pre_transcription_values", "0");
        Log.d(TAG, "getPreVideoValue-preValue: " + preValue);
        return Integer.parseInt(preValue);
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "run()!!!!");
            MyApplication.isPreRecording = true;
            stopVideoRecording();
        }
    }

    private void releasePreRecordingTimer() {
        if (mMyTimerTask != null) {
            mMyTimerTask.cancel();
            mMyTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private void startPreRecordingTimer() {
        releasePreRecordingTimer();
        mMyTimerTask = new MyTimerTask();
        mTimer = new Timer();
        mTimer.schedule(mMyTimerTask, 11*1000, (getPreVideoValue() + 2) * 1000L);
    }

    private final Runnable pictureRunnable = this::takePicture;

    CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            super.onCameraOpened(options);
            Log.d(TAG, "onCameraOpened()!!!!!");
            mLocationController = new LocationController(requireActivity());
            mLocationController.setOnLocationListener(CameraFragment.this);
            mLocationController.startLocation(requireActivity());
            SystemProperties.set("sys.camera.status", "1");//????????????
            Log.d(TAG, SystemProperties.get("sys.camera.status", "0"));
            CameraSoundUtils.initSound(getActivity());
            mCameraView.startAutoFocus((float) mCameraView.getWidth() / 2, (float) mCameraView.getHeight() / 2);
            mCameraView.setAutoFocusResetDelay(1000);
            if (isKeyDownStart()) {
                if (isKeyStartVideo()) {
                    MyApplication.isPreRecording = false;
                    startVideoRecording();
                } else {
                    mHandler.postDelayed(pictureRunnable, 500);
                }
                resetKeyDownFlag();
            } else {
                Log.d(TAG, "isVideoKeyDown: " + isVideoKeyDown);
                if (!isVideoKeyDown) {//??????????????????????????????key down??????????????????
                    if (getPreVideoValue() > 0) {//??????????????????????????????
                        if (mCameraView.getMode() == Mode.VIDEO) {//????????????????????????????????????
                            MyApplication.isPreRecording = true;
                            startVideoRecording();
                            startPreRecordingTimer();
                        }
                    }
                } else {
                    isVideoKeyDown = false;
                }
            }
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            Log.d(TAG, "onCameraClosed()!!!!!");
            SystemProperties.set("sys.camera.status", "0");//????????????
            Log.d(TAG, SystemProperties.get("sys.camera.status", "0"));
            FileUtils.setCurrentPreVideoFile(null);
            CameraSoundUtils.releaseSound();
            if (mLocationController != null) {
                mLocationController.stopLocation();
            }
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            savePictureData(result);
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            Log.d(TAG, "onVideoTaken()!!!");
            isVideoRecording = false;
            if (mCameraView.getMode() == Mode.VIDEO) {
                File mCurrentVideoFile = FileUtils.getCurrentVideoFile();
                String mCurrentVideoPath = mCurrentVideoFile.getAbsolutePath();
                Log.d(TAG, "mCurrentVideoPath: " + mCurrentVideoPath);
                if (!MyApplication.isModeBtnClick) {
                    binding.shutImageView.setImageResource(R.drawable.ic_video_btn_background);
                    binding.shutImageView.invalidate();
                    binding.thumbImageView.setEnabled(true);
                    Log.d(TAG, "onVideoTaken()-MyApplication.isPreRecording: " + MyApplication.isPreRecording);
                    if (getPreVideoValue() > 0) {//???????????????????????????
                        if (MyApplication.isPreRecording) {//???????????????????????????
                            if (FileUtils.isPreVideoExist()) {
                                boolean preVideoDelete = FileUtils.getCurrentPreVideoFile().delete();
                                Log.d(TAG, "1-preVideoDelete: " + preVideoDelete);
                            }
                            File newPreFile = new File(mCurrentVideoPath.replace(FileUtils.VIDEO_FORMAT, FileUtils.PRE_VIDEO_FORMAT));
                            if (new File(mCurrentVideoPath).renameTo(newPreFile)) {
                                FileUtils.setCurrentPreVideoFile(newPreFile);
                                Log.d(TAG, "1-a new pre video file!!!");
                            }
                            startVideoRecording();
                        } else {//?????????????????????,??????????????????
                            requireActivity().runOnUiThread(() -> {
                                MyApplication.isPreRecording = true;

                                if (FileUtils.isPreVideoExist()) {
                                    File preVideo = FileUtils.getCurrentPreVideoFile();
                                    File currentVideo = new File(mCurrentVideoPath.replace(FileUtils.VIDEO_FORMAT, FileUtils.JOIN_VIDEO_FORMAT));

                                    ArrayList<String> videoList = new ArrayList<>();
                                    //????????????2???????????????
                                    videoList.add(preVideo.getAbsolutePath());
                                    videoList.add(mCurrentVideoPath);
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            super.run();
                                            JoinVideoUtils joinVideoUtils = new JoinVideoUtils(getActivity(), videoList, currentVideo.getAbsolutePath());
                                            if (!joinVideoUtils.isRunning()) {
                                                joinVideoUtils.joinVideo(mHandler);
                                            }
                                        }
                                    }.start();
                                } else {
                                    FileUtils.setThumbnailFile(mCurrentVideoFile);
                                    mHandler.sendEmptyMessage(UPDATE_THUMB_UI);
                                }
                                FileUtils.setCurrentPreVideoFile(null);
                                startVideoRecording();
                                startPreRecordingTimer();
                            });
                        }
                    } else {
                        FileUtils.setThumbnailFile(mCurrentVideoFile);
                        mHandler.sendEmptyMessage(UPDATE_THUMB_UI);
                        //updateGallery(true, null);
                    }
                } else {
                    MyApplication.isModeBtnClick = false;
                    Log.d(TAG, "onVideoTaken()-MyApplication.isPreRecording: " + MyApplication.isPreRecording);
                    if (getPreVideoValue() > 0) {//???????????????????????????
                        if (FileUtils.isPreVideoExist()) {
                            boolean preVideoDelete = FileUtils.getCurrentPreVideoFile().delete();
                            Log.d(TAG, "1-preVideoDelete: " + preVideoDelete);
                        }
                        File newPreFile = new File(mCurrentVideoPath.replace(FileUtils.VIDEO_FORMAT, FileUtils.PRE_VIDEO_FORMAT));
                        if (new File(mCurrentVideoPath).renameTo(newPreFile)) {
                            //mCurrentPath = newPreFile.getAbsolutePath();
                            FileUtils.setCurrentPreVideoFile(newPreFile);
                            Log.d(TAG, "1-a new pre video file!!!");
                        }
                        MyApplication.isPreRecording = true;
                        startVideoRecording();
                    }
                }
            }
        }

        @Override
        public void onOrientationChanged(int orientation) {
            super.onOrientationChanged(orientation);
        }

        @Override
        public void onAutoFocusStart(@NonNull PointF point) {
            super.onAutoFocusStart(point);
        }

        @Override
        public void onAutoFocusEnd(boolean successful, @NonNull PointF point) {
            super.onAutoFocusEnd(successful, point);
        }

        @Override
        public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onZoomChanged(newValue, bounds, fingers);
        }

        @Override
        public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onExposureCorrectionChanged(newValue, bounds, fingers);
        }

        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            isVideoRecording = true;
            Log.d(TAG, "onVideoRecordingStart()!!!");
            // ?????????mVideoSize
            mCurrentVideoSize = mCameraView.getVideoSize();
            binding.thumbImageView.setEnabled(false);
            Log.d(TAG, "onVideoRecordingStart()-MyApplication.isPreRecording: " + MyApplication.isPreRecording);
            if (!MyApplication.isPreRecording) {//????????????????????????????????????
                binding.shutImageView.setImageResource(R.drawable.ic_video_recording_background);
                binding.shutImageView.invalidate();
                CameraSoundUtils.playSound(getActivity(), ISoundPlayback.START_VIDEO_RECORDING);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            Log.d(TAG, "onVideoRecordingEnd()!!!");
            if (getPreVideoValue() > 0) {
                if (!MyApplication.isPreRecording && !MyApplication.isModeBtnClick) {//????????????????????????????????????
                    CameraSoundUtils.playSound(getActivity(), ISoundPlayback.STOP_VIDEO_RECORDING);
                }
            } else {
                CameraSoundUtils.playSound(getActivity(), ISoundPlayback.STOP_VIDEO_RECORDING);
            }
        }

        @Override
        public void onPictureShutter() {
            super.onPictureShutter();
        }
    };

    private String getDestPath(String path) {
        String mPath = path.substring(0, path.lastIndexOf(".") - 1) + "_cut.mp4";
        Log.d(TAG, "mPath: " + mPath);

        return mPath;
    }

    private void videoCut() {
        String videoPath = FileUtils.getCurrentVideoFile().getAbsolutePath();
        if (!TextUtils.isEmpty(videoPath)) {
            final File sourceFile = new File(videoPath);
            final File destFile = new File(getDestPath(videoPath));
            final int start_S = 1;
            final int end_S = 10;
            new Thread(() -> {
                try {
                    TrimVideoUtils.getInstance().startTrim(true, start_S, end_S, sourceFile, destFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "e: " + e);
                    TrimVideoUtils.getInstance().setTrimCallBack(null);
                }
            }).start();
        } else {
            Log.d(TAG, "mCurrentPath is null!");
        }
    }

    private final TrimVideoUtils.TrimFileCallBack trimFileCallBack = new TrimVideoUtils.TrimFileCallBack() {
        @Override
        public void trimCallback(boolean isNew, int startS, int endS, int vTotal, File file, File trimFile) {
            /* *
             * ????????????
             * @param isNew ???????????????
             * @param starts ????????????(???)
             * @param ends ????????????(???)
             * @param vTime ????????????
             * @param file ???????????????????????????
             * @param trimFile ??????????????????????????????
             */
            // ===========
            Log.d(TAG, "isNew : " + isNew);
            Log.d(TAG, "startS : " + startS);
            Log.d(TAG, "endS : " + endS);
            Log.d(TAG, "vTotal : " + vTotal);
            Log.d(TAG, "file : " + file.getAbsolutePath());
            Log.d(TAG, "trimFile : " + trimFile.getAbsolutePath());
        }

        @Override
        public void trimError(int eType) {
            switch(eType){
                case TrimVideoUtils.FILE_NOT_EXISTS: // ???????????????
                    System.out.println("?????????????????????");
                    break;
                case TrimVideoUtils.TRIM_STOP: // ??????????????????
                    System.out.println("????????????");
                    break;
                case TrimVideoUtils.TRIM_FAIL:
                default: // ????????????
                    System.out.println("????????????");
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateGallery(boolean isVideo, byte[] bytes) {
        //insert database(??????????????????????????????)
        ContentValues mContentValues;
        Uri mUri;
        if (isVideo) {
            if (mCurrentVideoSize == null) {
                mCurrentVideoSize = mCameraView.getVideoSize();
                if (mCurrentVideoSize == null)
                    return;
            }
            String videoPath = FileUtils.getCurrentVideoFile().getAbsolutePath();
            Log.d(TAG, "updateGallery-videoPath: " + videoPath);
            mContentValues = CameraUtil.createVideoValues(mCurrentTitle, videoPath, mCurrentTime, mCurrentVideoSize.getWidth(), mCurrentVideoSize.getHeight());
            if (getActivity() != null) {
                mUri = getActivity().getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mContentValues);
                getActivity().getContentResolver().update(mUri, mContentValues, null, null);
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + videoPath)));
            }
        } else {
            Size mCurrentPictureSize = mCameraView.getPictureSize();
            int mPictureWidth = 0, mPictureHeight = 0;
            if (mCurrentPictureSize != null) {
                mPictureWidth = mCurrentPictureSize.getWidth();
                mPictureHeight = mCurrentPictureSize.getHeight();
            }
            String picturePath = FileUtils.getCurrentPictureFile().getAbsolutePath();
            Log.d(TAG, "updateGallery-picturePath: " + picturePath);
            mContentValues = CameraUtil.createPhotoValues(bytes, mCurrentTitle, mCurrentTime, picturePath, mPictureWidth, mPictureHeight);
            if (getActivity() != null) {
                /* ?????????S211????????????
                 * mUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mContentValues);
                 * getActivity().getContentResolver().update(mUri, mContentValues, null, null);
                 */
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + picturePath)));
            }
        }
    }

    private void onModeSwitch(String action) {
        Log.d(TAG, "onModeSwitch-action: " + action);
        if (action == null) {
            if (mCameraView.getMode() == Mode.PICTURE) {
                mCameraView.setMode(Mode.VIDEO);
                updateModeBtnUI(Mode.VIDEO);
            } else {
                mCameraView.setMode(Mode.PICTURE);
                updateModeBtnUI(Mode.PICTURE);
            }
        } else if (action.equals("video")) {
            mCameraView.setMode(Mode.VIDEO);
            updateModeBtnUI(Mode.VIDEO);
        } else if (action.equals("camera")) {
            mCameraView.setMode(Mode.PICTURE);
            updateModeBtnUI(Mode.PICTURE);
        }
    }

    private void takePicture() {
        // ???????????????
        FocusUtils.hideFocus();
        // ????????????????????????
        mCameraView.setPlaySounds(false);
        if (!mCameraView.isTakingVideo()) {
            CameraSoundUtils.playSound(getActivity(), ISoundPlayback.SHUTTER_CLICK);
        }
        mCameraView.takePictureSnapshot();
    }

    private void startVideoRecording() {
        Log.d(TAG, "startVideoRecording()!!!!");
        showPreRecordingTips();
        // ???????????????
        FocusUtils.hideFocus();
        // ????????????????????????
        mCameraView.setPlaySounds(false);

        mCurrentTime = System.currentTimeMillis();
        mCurrentTitle = CameraUtil.createFileTitle(true, mCurrentTime);
        String fileName = CameraUtil.createFileName(true, mCurrentTitle);
        File mVideoFile = FileUtils.createVideoFile(fileName);
        FileUtils.setCurrentVideoFile(mVideoFile);
        //mCurrentPath = mVideoFile.getAbsolutePath();
        mCameraView.takeVideoSnapshot(mVideoFile);
    }

    private void stopVideoRecording() {
        Log.d(TAG, "stopVideoRecording()!!!!");
        showPreRecordingTips();
        mCameraView.stopVideo();
    }

    @Override
    public void onClick(View v) {
        if (v == binding.modeSwitchImageView) {
            MyApplication.isModeBtnClick = true;
            if (getPreVideoValue() > 0) {
                if (MyApplication.isPreRecording && isVideoRecording) {
                    releasePreRecordingTimer();
                    stopVideoRecording();
                    MyApplication.isPreRecording = false;
                    showPreRecordingTips();
                }
            }
            onModeSwitch(null);
        } else if (v == binding.shutImageView) {
            MyApplication.isModeBtnClick = false;
            if (mCameraView.getMode() == Mode.PICTURE) {
                takePicture();
            } else if (mCameraView.getMode() == Mode.VIDEO) {
                if (getPreVideoValue() > 0) {//VIDEO?????????,getPreVideoValue() > 0,????????????????????????,???????????????
                    if (MyApplication.isPreRecording) {//?????????????????????????????????????????????
                        MyApplication.isPreRecording = false;
                        binding.shutImageView.setImageResource(R.drawable.ic_video_recording_background);
                        binding.shutImageView.invalidate();
                        CameraSoundUtils.playSound(getActivity(), ISoundPlayback.START_VIDEO_RECORDING);
                        showPreRecordingTips();
                        releasePreRecordingTimer();
                    } else {
                        if (isVideoRecording) {//???????????????????????????????????????
                            stopVideoRecording();
                        }
                    }
                } else {
                    MyApplication.isPreRecording = false;
                    if (isVideoRecording) {
                        stopVideoRecording();
                    } else {
                        startVideoRecording();
                    }
                }
            }
        } else if (v == binding.thumbImageView) {
            MyApplication.isAppBtnClick = true;
            ThumbnailUtils.gotoGallery(getActivity(), ThumbnailUtils.getVideoThumbType(), ThumbnailUtils.getLastMediaFilePath());
        } else if (v == binding.cameraSwitchImageView) {
            mCameraView.setFacing(mCameraView.getFacing() == Facing.BACK ? Facing.FRONT : Facing.BACK);
        } else if (v == binding.flashImageView) {
            if (mCameraView.getFlash() == Flash.OFF) {
                //mCameraView.setFlash(Flash.ON);
                mCameraView.setFlash(Flash.TORCH);
                binding.flashImageView.setImageLevel(1);
            } else if (mCameraView.getFlash() == Flash.ON || mCameraView.getFlash() == Flash.TORCH) {
                mCameraView.setFlash(Flash.AUTO);
                binding.flashImageView.setImageLevel(2);
            } else if (mCameraView.getFlash() == Flash.AUTO) {
                mCameraView.setFlash(Flash.OFF);
                binding.flashImageView.setImageLevel(0);
            }
        } else if (v == binding.settingsImageView) {
            if (getActivity() != null) {
                MyApplication.isAppBtnClick = true;
                getActivity().startActivity(new Intent(getActivity(), SettingsActivity.class));
            }
        }
    }

    private final Runnable thumbRunnable = () -> mHandler.sendEmptyMessage(UPDATE_THUMB_UI);

    private void showStorageInfo() {
        if (StorageInfoUtil.isExternalStorageExist()) {
            String exAvail = StorageInfoUtil.saveDecimalDigit(String.valueOf(StorageInfoUtil.getExternalStorageAvailable()));
            String exTotal = StorageInfoUtil.saveDecimalDigit(String.valueOf(StorageInfoUtil.getExternalStorageTotal()));
            String inAvail = StorageInfoUtil.saveDecimalDigit(String.valueOf(StorageInfoUtil.getInternalStorageAvailable()));
            String inTotal = StorageInfoUtil.saveDecimalDigit(String.valueOf(StorageInfoUtil.getInternalStorageTotal()));
            Log.d(TAG, "external exAvail/exTotal: " + exAvail + "/" + exTotal);
            Log.d(TAG, "internal inAvail/inTotal: " + inAvail + "/" + inTotal);
            binding.externalStorageTv.setText(String.format(getString(R.string.external_storage_info), exAvail, exTotal));
            binding.internalStorageTv.setText(String.format(getString(R.string.internal_storage_info), inAvail, inTotal));
        } else {
            String avail = StorageInfoUtil.saveDecimalDigit(String.valueOf(StorageInfoUtil.getInternalStorageAvailable()));
            String total = StorageInfoUtil.saveDecimalDigit(String.valueOf(StorageInfoUtil.getInternalStorageTotal()));
            Log.d(TAG, "internal avail/total: " + avail + "/" + total);
            binding.internalStorageTv.setText(String.format(getString(R.string.internal_storage_info), avail, total));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()!");
        showStorageInfo();
        MyApplication.isAppBtnClick = false;
        MyApplication.isModeBtnClick = false;
        TrimVideoUtils.getInstance().setTrimCallBack(trimFileCallBack);
        mCameraView.open();
        mHandler.postDelayed(thumbRunnable, 0);
        registerReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()!");
        SystemProperties.set("sys.camera.status", "0");//????????????
        unregisterReceiver();
        releasePreRecordingTimer();
        mCameraView.close();
        TrimVideoUtils.getInstance().setTrimCallBack(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()!");
        releasePreRecordingTimer();
        mCameraView.removeCameraListener(cameraListener);
        mCameraView.destroy();
        binding = null;
    }

}