package com.frame.camera.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.frame.camera.R;
import com.frame.camera.databinding.FragmentCameraBinding;
import com.frame.camera.sound.ISoundPlayback;
import com.frame.camera.utils.CameraSoundUtils;
import com.frame.camera.utils.FileUtils;
import com.frame.camera.utils.FocusUtils;
import com.frame.camera.utils.SystemProperties;
import com.frame.camera.utils.ThumbnailUtils;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.markers.AutoFocusMarker;
import com.otaliastudios.cameraview.markers.AutoFocusTrigger;
import com.otaliastudios.cameraview.markers.DefaultAutoFocusMarker;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "CameraFragment:CAMERA";
    private static final String VIDEO_KEY_DOWN_ACTION = "com.runbo.video.key.down";
    private static final String CAMERA_KEY_DOWN_ACTION = "com.runbo.camera.key.down";
    private static final int UPDATE_THUMB_UI = 0;

    private FragmentCameraBinding binding;
    private CameraView mCameraView;
    private MyReceiver myReceiver;

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_THUMB_UI: {
                    updateThumbBtnUI();
                    break;
                }

                default:
                    break;
            }
        }
    };

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action: " + action);
            if (action.equals(VIDEO_KEY_DOWN_ACTION)) {
                //如果当前是拍照模式则切换为录像模式
                if (mCameraView.getMode() == Mode.PICTURE) {
                    onModeSwitch("video");
                }
                onShutterClick();
            } else if (action.equals(CAMERA_KEY_DOWN_ACTION)) {
                if (mCameraView.isTakingVideo()) {
                    mCameraView.takePictureSnapshot();
                } else {
                    //如果当前是录像模式则切换为拍照模式
                    if (mCameraView.getMode() == Mode.VIDEO) {
                        onModeSwitch("camera");
                    }
                    onShutterClick();
                }
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
        return requireActivity().getIntent().getBooleanExtra("key_down_start", false);
    }

    private void resetKeyDownFlag() {
        requireActivity().getIntent().putExtra("key_down_start", false);
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
        binding.modeSwitchImageView.setOnClickListener(this);
        binding.shutImageView.setOnClickListener(this);
        binding.thumbImageView.setOnClickListener(this);
        binding.cameraSwitchImageView.setOnClickListener(this);
        binding.flashImageView.setOnClickListener(this);
    }

    private void savePictureData(PictureResult result) {
        byte[] bytes = result.getData();
        FileOutputStream output = null;
        File mPictureFile = FileUtils.createPictureFile(FileUtils.getMediaFileName(FileUtils.MEDIA_TYPE_IMAGE));
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

    CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            super.onCameraOpened(options);
            SystemProperties.set("sys.camera.status", "1");//相机打开
            Log.d(TAG, SystemProperties.get("sys.camera.status", "0"));
            CameraSoundUtils.initSound(getActivity());
            mCameraView.startAutoFocus((float) mCameraView.getWidth() / 2, (float) mCameraView.getHeight() / 2);
            mCameraView.setAutoFocusResetDelay(1000);
            if (isKeyDownStart()) {
                onShutterClick();
                resetKeyDownFlag();
            }
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            SystemProperties.set("sys.camera.status", "0");//相机关闭
            Log.d(TAG, SystemProperties.get("sys.camera.status", "0"));
            CameraSoundUtils.releaseSound();
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            savePictureData(result);
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
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
            binding.shutImageView.setImageResource(R.drawable.ic_video_recording_background);
            binding.shutImageView.invalidate();
            binding.thumbImageView.setEnabled(false);
            CameraSoundUtils.playSound(getActivity(), ISoundPlayback.START_VIDEO_RECORDING);
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            binding.shutImageView.setImageResource(R.drawable.ic_video_btn_background);
            binding.shutImageView.invalidate();
            binding.thumbImageView.setEnabled(true);
            mHandler.sendEmptyMessage(UPDATE_THUMB_UI);
            CameraSoundUtils.playSound(getActivity(), ISoundPlayback.STOP_VIDEO_RECORDING);
        }

        @Override
        public void onPictureShutter() {
            super.onPictureShutter();
        }
    };

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

    private void onShutterClick() {
        if (mCameraView.getMode() == Mode.PICTURE) {
            // 隐藏对焦框
            FocusUtils.hideFocus();
            // 停止播放对焦声音
            mCameraView.setPlaySounds(false);
            CameraSoundUtils.playSound(getActivity(), ISoundPlayback.SHUTTER_CLICK);
            mCameraView.takePictureSnapshot();
        } else if (mCameraView.getMode() == Mode.VIDEO) {
            if (mCameraView.isTakingVideo()) {
                mCameraView.stopVideo();
            } else {
                // 隐藏对焦框
                FocusUtils.hideFocus();
                // 停止播放对焦声音
                mCameraView.setPlaySounds(false);
                File mVideoFile = FileUtils.createVideoFile(FileUtils.getMediaFileName(FileUtils.MEDIA_TYPE_VIDEO));
                mCameraView.takeVideoSnapshot(mVideoFile);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == binding.modeSwitchImageView) {
            onModeSwitch(null);
        } else if (v == binding.shutImageView) {
            onShutterClick();
        } else if (v == binding.thumbImageView) {
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
        }
    }

    private final Runnable thumbRunnable = () -> mHandler.sendEmptyMessage(UPDATE_THUMB_UI);

    @Override
    public void onResume() {
        super.onResume();
        mCameraView.open();
        mHandler.postDelayed(thumbRunnable, 0);
        registerReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver();
        mCameraView.close();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraView.removeCameraListener(cameraListener);
        mCameraView.destroy();
        binding = null;
    }

}