package com.cg.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cg.cameralibrary.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordVideoView extends FrameLayout implements SurfaceHolder.Callback, MediaRecorder.OnErrorListener {

    private SurfaceView mSufaceView;
    private Chronometer mRecordTime;

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private String mCurrentPath;//当前视频的路径
    private String mSavePath;//视频保存路径

    private long mRecordPauseTime;
    private boolean mIsBackCamera = true;//是否是后置摄像头
    private boolean mIsRecording = false;//是否正在录制
    private boolean mIsPause = false;//是否暂停

    public RecordVideoView(Context context) {
        super(context);
    }

    public RecordVideoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_video_record, null);
        addView(view);
        mSufaceView = findViewById(R.id.surface_video_record);
        mRecordTime = findViewById(R.id.chronometer_video_record);
        mSurfaceHolder = mSufaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(this);
    }

    private void initCamera() {
        try {
            if (mCamera != null) stopCamera();
            if (mIsBackCamera) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
            if (mCamera == null) {
                Toast.makeText(getContext(), "相机不可用！", Toast.LENGTH_SHORT).show();
                return;
            }
            mCamera.setPreviewDisplay(mSurfaceHolder);
            setCameraParams();
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setCameraParams() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            //设置相机的横竖屏幕
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                params.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
            } else {
                params.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
            }
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            params.setRecordingHint(true);
            if (params.isVideoStabilizationSupported()) {
                params.setVideoStabilization(true);
            }
            List<Camera.Size> sizeList = params.getSupportedPreviewSizes();

            Camera.Size previewSize = getOptimalSize(sizeList, getWidth(), getHeight());
            if (previewSize != null) {
                params.setPreviewSize(previewSize.width, previewSize.height);
            }
            mCamera.setParameters(params);
        }
    }

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void setConfigRecord() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setOnErrorListener(this);

        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setAudioEncodingBitRate(44100);
        if (mProfile.videoBitRate > 2 * 1024 * 1024) {
            mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
        } else {
            mMediaRecorder.setVideoEncodingBitRate(1024 * 1024);
        }
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setOrientationHint(90);
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setOutputFile(mCurrentPath);
    }

    //获取合适的预览尺寸方案
    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private void changeStatus(boolean isRecording, boolean isPause) {
        this.mIsRecording = isRecording;
        this.mIsPause = isPause;
    }

    private void changeTimer(boolean isStart, long recordPauseTime) {
        if (mRecordTime.getVisibility() == GONE) return;
        if (isStart) {
            long baseTime = recordPauseTime == 0 ? SystemClock.elapsedRealtime() :
                    (SystemClock.elapsedRealtime() - (recordPauseTime - mRecordTime.getBase()));
            mRecordTime.setBase(baseTime);
            mRecordTime.start();
        } else {
            mRecordPauseTime = recordPauseTime;
            mRecordTime.stop();
        }
    }


    /**
     * 开始录制视频
     */
    public void startRecord(String path) {
        if (mIsRecording) {
            Toast.makeText(getContext(), "正在录制视频", Toast.LENGTH_SHORT).show();
            return;
        }
        mCurrentPath = path;
        initCamera();
        mCamera.unlock();
        setConfigRecord();
        try {
            //开始录制
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            changeStatus(true, false);
            changeTimer(true, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (!mIsRecording) {
            Toast.makeText(getContext(), "未开始录制", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.pause();
        } else {
            stopRecord();
            mSavePath = mCurrentPath;
        }
        changeStatus(false, true);
        changeTimer(false, SystemClock.elapsedRealtime());
    }

    public void resume() {
        if (!mIsPause) {
            Toast.makeText(getContext(), "未暂停录制", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.resume();
        } else {
            startRecord(mCurrentPath + ".sec");
        }
        changeStatus(true, false);
        changeTimer(true, mRecordPauseTime);
    }

    /**
     * 停止录制视频
     */
    public void stopRecord() {
        if (!mIsRecording && !mIsPause) {
            Toast.makeText(getContext(), "未开始录制", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (!TextUtils.isEmpty(mSavePath)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList videoList = new ArrayList<>();
                        videoList.add(mSavePath);
                        videoList.add(mCurrentPath);
                        String mergePath = mCurrentPath + ".bc";

                        VideoComposer composer = new VideoComposer(videoList, mergePath);
                        final boolean result = composer.joinVideo();
//                        VideoComposer.mergeMP4(videoList, mergePath);

                        new File(mCurrentPath).delete();
                        new File(mSavePath).delete();

                        new File(mergePath).renameTo(new File(mSavePath));
                        mSavePath = null;
                        changeStatus(false, false);
                    }
                }).start();
                return;
            }
            changeStatus(false, false);
            changeTimer(false, 0);
        }
    }

    /**
     * 切换摄像头
     */
    public void changeCamera() {
        mIsBackCamera = !mIsBackCamera;
        initCamera();
    }


    /**
     * 闪光灯模式 Parameters
     */
    public void toggleFlash() {
        if (mCamera == null) return;
        Camera.Parameters parameters = mCamera.getParameters();
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        mCamera.setParameters(parameters);
    }

    /**
     * 闪光灯模式 Parameters
     */
    public void setFlashMode(String flashMode) {
        if (mCamera == null) return;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(flashMode);
        mCamera.setParameters(parameters);
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public boolean isPause() {
        return mIsPause;
    }

    public void showTimer(boolean showTimer) {
        mRecordTime.setVisibility(showTimer ? VISIBLE : GONE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mSurfaceHolder.getSurface() == null) {
            return;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }


    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mMediaRecorder != null) mMediaRecorder.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

