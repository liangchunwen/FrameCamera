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
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.markers.AutoFocusMarker;
import com.otaliastudios.cameraview.markers.AutoFocusTrigger;
import com.otaliastudios.cameraview.markers.DefaultAutoFocusMarker;
import com.otaliastudios.cameraview.size.Size;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class CameraFragment extends Fragment implements View.OnClickListener, LocationController.OnLocationListener {
    private static final String TAG = "CameraFragment:CAMERA";
    private static final String VIDEO_KEY_DOWN_ACTION = "com.runbo.video.key.down";
    private static final String CAMERA_KEY_DOWN_ACTION = "com.runbo.camera.key.down";
    private static final int UPDATE_THUMB_UI = 0;
    private static boolean isVideoKeyDown = false;
    private static boolean isVideoRecording = true;
    private MyTimerTask mMyTimerTask;
    private Timer mTimer;

    private LocationController mLocationController;
    private FragmentCameraBinding binding;
    private CameraView mCameraView;
    private MyReceiver myReceiver;
    private String mCurrentPath;
    private String mCurrentTitle;
    private long mCurrentTime;
    private Size mCurrentVideoSize;

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_THUMB_UI) {
                updateThumbBtnUI();
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
                    //如果当前是拍照模式则切换为录像模式
                    if (mCameraView.getMode() == Mode.PICTURE) {
                        isVideoKeyDown = true;
                        onModeSwitch("video");
                        MyApplication.isPreRecording = false;
                        startVideoRecording();
                    } else {
                        if (getPreVideoValue() > 0) {//VIDEO模式下,getPreVideoValue() > 0,要么正在常规录制,要么在预录
                            if (MyApplication.isPreRecording) {//当前正在预录则直接转为正常录制
                                MyApplication.isPreRecording = false;
                                binding.shutImageView.setImageResource(R.drawable.ic_video_recording_background);
                                binding.shutImageView.invalidate();
                                CameraSoundUtils.playSound(getActivity(), ISoundPlayback.START_VIDEO_RECORDING);
                                showPreRecordingTips();
                                releasePreRecordingTimer();
                            } else {
                                if (isVideoRecording) {//当前正在常规录像则停止录像
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
                            //如果当前是录像模式则切换为拍照模式
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
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCameraView.getMode() == Mode.VIDEO) {
                    if (MyApplication.isPreRecording) {
                        binding.preVideoTv.setVisibility(View.VISIBLE);
                    } else {
                        binding.preVideoTv.setVisibility(View.GONE);
                    }
                } else {
                    binding.preVideoTv.setVisibility(View.GONE);
                }
            }
        });
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCameraView = binding.cameraView;
        if (isKeyDownStart()) {
            onModeSwitch(requireActivity().getIntent().getStringExtra("camera_mode"));
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

        //针对DT951的RunboZ1版本显示警员信息水印
        String custom_version = SystemProperties.get("ro.custom.build.version", "");
        Log.d(TAG, "custom_version: " + custom_version);
        /*
        if (custom_version.startsWith("ZF2020_DT951_RunboZ1")) {
            binding.watermarkInfoRl.setVisibility(View.VISIBLE);
            setPoliceWaterMark();
        } else {
            binding.watermarkInfoRl.setVisibility(View.GONE);
        }
        */
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
        //File mPictureFile = FileUtils.createPictureFile(FileUtils.getMediaFileName(FileUtils.MEDIA_TYPE_IMAGE));
        File mPictureFile = FileUtils.createPictureFile(fileName);
        mCurrentPath = mPictureFile.getAbsolutePath();
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
        } else if (Mode.VIDEO == mode) {
            binding.shutImageView.setImageResource(R.drawable.ic_video_btn_background);
            binding.shutImageView.invalidate();
            binding.modeSwitchImageView.setImageResource(R.drawable.ic_photo_mode_unselected);
            binding.modeSwitchImageView.invalidate();
        }
    }

    FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull Frame frame) {
            byte[] bytes = frame.getData();
        }
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

    CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            super.onCameraOpened(options);
            Log.d(TAG, "onCameraOpened()!!!!!");
            mLocationController = new LocationController(requireActivity());
            mLocationController.setOnLocationListener(CameraFragment.this);
            mLocationController.startLocation(requireActivity());
            SystemProperties.set("sys.camera.status", "1");//相机打开
            Log.d(TAG, SystemProperties.get("sys.camera.status", "0"));
            CameraSoundUtils.initSound(getActivity());
            mCameraView.startAutoFocus((float) mCameraView.getWidth() / 2, (float) mCameraView.getHeight() / 2);
            mCameraView.setAutoFocusResetDelay(1000);
            if (isKeyDownStart()) {
                MyApplication.isPreRecording = false;
                startVideoRecording();
                resetKeyDownFlag();
            } else {
                Log.d(TAG, "isVideoKeyDown: " + isVideoKeyDown);
                if (!isVideoKeyDown) {//此类本地广播接收到的key down不在这里相应
                    if (getPreVideoValue() > 0) {//预录模式打开的状态下
                        if (mCameraView.getMode() == Mode.VIDEO) {//当前是录像模式则进入预录
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
            SystemProperties.set("sys.camera.status", "0");//相机关闭
            Log.d(TAG, SystemProperties.get("sys.camera.status", "0"));
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
            if (!MyApplication.isModeBtnClick) {
                binding.shutImageView.setImageResource(R.drawable.ic_video_btn_background);
                binding.shutImageView.invalidate();
                binding.thumbImageView.setEnabled(true);
                Log.d(TAG, "onVideoTaken()-MyApplication.isPreRecording: " + MyApplication.isPreRecording);
                if (getPreVideoValue() > 0) {//预录模式打开状态下
                    if (MyApplication.isPreRecording) {//预录结束后继续预录
                        startVideoRecording();
                    } else {//常规录制结束后,启动预录线程
                        mHandler.sendEmptyMessage(UPDATE_THUMB_UI);
                        updateGallery(true, null);
                        MyApplication.isPreRecording = true;
                        startVideoRecording();
                        startPreRecordingTimer();
                    }
                } else {
                    mHandler.sendEmptyMessage(UPDATE_THUMB_UI);
                    updateGallery(true, null);
                }
            } else {
                MyApplication.isModeBtnClick = false;
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
            // 初始化mVideoSize
            mCurrentVideoSize = mCameraView.getVideoSize();
            binding.thumbImageView.setEnabled(false);
            Log.d(TAG, "onVideoRecordingStart()-MyApplication.isPreRecording: " + MyApplication.isPreRecording);
            if (!MyApplication.isPreRecording) {//预录的过程中不播放提示音
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
                if (!MyApplication.isPreRecording && !MyApplication.isModeBtnClick) {//预录的过程中不播放提示音
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
        if (mCurrentPath !=null && !TextUtils.isEmpty(mCurrentPath)) {
            final File sourceFile = new File(mCurrentPath);
            final File destFile = new File(getDestPath(mCurrentPath));
            final int start_S = 1;
            final int end_S = 10;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        TrimVideoUtils.getInstance().startTrim(true, start_S, end_S, sourceFile, destFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "e: " + e);
                        TrimVideoUtils.getInstance().setTrimCallBack(null);
                    }
                }
            }).start();
        } else {
            Log.d(TAG, "mCurrentPath is null!");
        }
    }

    private TrimVideoUtils.TrimFileCallBack trimFileCallBack = new TrimVideoUtils.TrimFileCallBack() {
        @Override
        public void trimCallback(boolean isNew, int startS, int endS, int vTotal, File file, File trimFile) {
            /**
             * 裁剪回调
             * @param isNew 是否新剪辑
             * @param starts 开始时间(秒)
             * @param ends 结束时间(秒)
             * @param vTime 视频长度
             * @param file 需要裁剪的文件路径
             * @param trimFile 裁剪后保存的文件路径
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
                case TrimVideoUtils.FILE_NOT_EXISTS: // 文件不存在
                    System.out.println("视频文件不存在");
                    break;
                case TrimVideoUtils.TRIM_STOP: // 手动停止裁剪
                    System.out.println("停止裁剪");
                    break;
                case TrimVideoUtils.TRIM_FAIL:
                default: // 裁剪失败
                    System.out.println("裁剪失败");
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateGallery(boolean isVideo, byte[] bytes) {
        //insert database(插入到系统相册数据库)
        ContentValues mContentValues;
        Uri mUri;
        if (isVideo) {
            mContentValues = CameraUtil.createVideoValues(mCurrentTitle, mCurrentPath, mCurrentTime, mCurrentVideoSize.getWidth(), mCurrentVideoSize.getHeight());
            if (getActivity() != null) {
                mUri = getActivity().getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mContentValues);
                getActivity().getContentResolver().update(mUri, mContentValues, null, null);
            }
        } else {
            Size mCurrentPictureSize = mCameraView.getPictureSize();
            int mPictureWidth = 0, mPictureHeight = 0;
            if (mCurrentPictureSize != null) {
                mPictureWidth = mCurrentPictureSize.getWidth();
                mPictureHeight = mCurrentPictureSize.getHeight();
            }
            mContentValues = CameraUtil.createPhotoValues(bytes, mCurrentTitle, mCurrentTime, mCurrentPath, mPictureWidth, mPictureHeight);
            if (getActivity() != null) {
                //这行在S211上会报错
                //mUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mContentValues);
                //getActivity().getContentResolver().update(mUri, mContentValues, null, null);
            }
        }
        if (getActivity() != null) {
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mCurrentPath)));
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
        // 隐藏对焦框
        FocusUtils.hideFocus();
        // 停止播放对焦声音
        mCameraView.setPlaySounds(false);
        if (!mCameraView.isTakingVideo()) {
            CameraSoundUtils.playSound(getActivity(), ISoundPlayback.SHUTTER_CLICK);
        }
        mCameraView.takePictureSnapshot();
    }

    private void startVideoRecording() {
        Log.d(TAG, "startVideoRecording()!!!!");
        showPreRecordingTips();
        // 隐藏对焦框
        FocusUtils.hideFocus();
        // 停止播放对焦声音
        mCameraView.setPlaySounds(false);

        mCurrentTime = System.currentTimeMillis();
        mCurrentTitle = CameraUtil.createFileTitle(true, mCurrentTime);
        String fileName = CameraUtil.createFileName(true, mCurrentTitle);
        //File mVideoFile = FileUtils.createVideoFile(FileUtils.getMediaFileName(FileUtils.MEDIA_TYPE_VIDEO));
        File mVideoFile = FileUtils.createVideoFile(fileName);
        mCurrentPath = mVideoFile.getAbsolutePath();
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
                if (getPreVideoValue() > 0) {//VIDEO模式下,getPreVideoValue() > 0,要么正在常规录制,要么在预录
                    if (MyApplication.isPreRecording) {//当前正在预录则直接转为正常录制
                        MyApplication.isPreRecording = false;
                        binding.shutImageView.setImageResource(R.drawable.ic_video_recording_background);
                        binding.shutImageView.invalidate();
                        CameraSoundUtils.playSound(getActivity(), ISoundPlayback.START_VIDEO_RECORDING);
                        showPreRecordingTips();
                        releasePreRecordingTimer();
                    } else {
                        if (isVideoRecording) {//当前正在常规录像则停止录像
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

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()!");
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
        SystemProperties.set("sys.camera.status", "0");//相机关闭
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