package com.frame.camera.utils;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import com.frame.camera.focus.FocusOverlayManager;
import com.frame.camera.focus.FocusView;
import com.otaliastudios.cameraview.CameraView;

public class FocusUtils {
    private static FocusOverlayManager mFocusOverlayManager;
    private static FocusView mFocusView;

    public static void initFocus(Context context, CameraView cameraView) {
        mFocusView = new FocusView(context);
        mFocusView.setVisibility(View.GONE);
        mFocusView.initFocusArea(cameraView.getWidth(), cameraView.getHeight());
        mFocusOverlayManager =  new FocusOverlayManager(mFocusView, Looper.getMainLooper());
    }

    public static FocusView getFocusView(Context context) {
        if (mFocusView == null) {
            mFocusView = new FocusView(context);
        }

        return mFocusView;
    }

    public static void startFocus(float x, float y) {
        if (mFocusOverlayManager != null) {
            mFocusOverlayManager.startFocus(x, y);
        }
    }

    public static void focusSuccess() {
        if (mFocusOverlayManager != null) {
            mFocusOverlayManager.focusSuccess();
        }
    }

    public static void hideFocus() {
        if (mFocusOverlayManager != null) {
            mFocusOverlayManager.hideFocusUI();
        }
    }
}
