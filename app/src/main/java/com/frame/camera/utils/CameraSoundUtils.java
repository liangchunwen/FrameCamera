package com.frame.camera.utils;

import android.content.Context;
import android.media.MediaPlayer;

import com.frame.camera.R;
import com.frame.camera.sound.ISoundPlayback;
import com.frame.camera.sound.SoundPlaybackImpl;

public class CameraSoundUtils {
    private static SoundPlaybackImpl mSoundPlayback;
    private static MediaPlayer mediaPlayer;

    public static void initSound(Context context) {
        if (mSoundPlayback == null) {
            mSoundPlayback = new SoundPlaybackImpl(context);
        }
    }

    public static void playSound(Context context, int sound) {
        if (sound == ISoundPlayback.START_VIDEO_RECORDING) {
            playRecordingSound(context, R.raw.start_video_rec);
        } else if (sound == ISoundPlayback.STOP_VIDEO_RECORDING) {
            playRecordingSound(context, R.raw.stop_video_rec);
        } else {
            if (mSoundPlayback == null) {
                initSound(context);
            }
            mSoundPlayback.play(sound);
        }
    }

    private static void playRecordingSound(Context context, int raw) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = MediaPlayer.create(context, raw);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }

    public static void releaseSound() {
        if (mSoundPlayback != null) {
            mSoundPlayback.pause();
            mSoundPlayback.release();
            mSoundPlayback = null;
        }
    }
}
