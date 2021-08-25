package com.frame.camera.utils;

import android.content.Context;

import com.frame.camera.sound.SoundPlaybackImpl;

public class CameraSoundUtils {
    private static SoundPlaybackImpl mSoundPlayback;

    public static void initSound(Context context) {
        if (mSoundPlayback == null) {
            mSoundPlayback = new SoundPlaybackImpl(context);
        }
    }

    public static void playSound(Context context, int sound) {
        if (mSoundPlayback == null) {
            initSound(context);
        }
        mSoundPlayback.play(sound);
    }

    public static void releaseSound() {
        if (mSoundPlayback != null) {
            mSoundPlayback.pause();
            mSoundPlayback.release();
            mSoundPlayback = null;
        }
    }
}
