package com.cg.liveness.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.oliveapp.libcommon.utility.LogUtil;
import com.oliveapp.libcommon.utility.PackageNameManager;

import java.io.IOException;

/**
 * Created by jthao on 1/13/16.
 */
public class AudioModule {
    private static final String TAG = AudioModule.class.getSimpleName();

    private MediaPlayer mAudioPlayer = new MediaPlayer();
    public void playAudio(Context context, String audioResourceName)
    {
        String packageName = PackageNameManager.getPackageName();

        int rawId = context.getResources().getIdentifier(audioResourceName, "raw", packageName);
        Uri audioUri = Uri.parse("android.resource://" + packageName + "/" + rawId);
        try {
            if (mAudioPlayer != null && mAudioPlayer.isPlaying())
                mAudioPlayer.stop();

            mAudioPlayer.reset();
            mAudioPlayer.setDataSource(context, audioUri);
            mAudioPlayer.prepare();
            mAudioPlayer.start();
        } catch (IOException e) {
            LogUtil.e(TAG, "fail to set data source for audio player", e);
        } catch (NullPointerException e) {
            LogUtil.e(TAG, "", e);
        } catch (IllegalStateException e) {
            LogUtil.e(TAG, "fail to play audio type: ", e);
        }
    }

    public boolean isPlaying() {
        if (mAudioPlayer != null) {
            return mAudioPlayer.isPlaying();
        }
        return false;
    }
    public void release() {
        try {
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
        } catch (Exception e) {
            LogUtil.e(TAG, "Fail to release", e);
        }
    }
}
