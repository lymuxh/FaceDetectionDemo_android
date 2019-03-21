package com.cg.codec;

import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class EncoderVideoActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private boolean mIsRecord;
    private String mVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encoder_video);

        File dir = new File(Environment.getExternalStorageDirectory(), "customVideo");
        FileUtils.createOrExistsDir(dir);
        File file = new File(dir, System.currentTimeMillis() + ".mp4");
        mVideoPath = file.getAbsolutePath();


        findView();
        initSurfaceView();
    }

    private void initSurfaceView() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
    }

    private void initCamera() {
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCamera.setDisplayOrientation(90);

            Camera.Parameters params = mCamera.getParameters();
            params.set("orientation", "portrait");
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            params.setRecordingHint(true);
            if (params.isVideoStabilizationSupported()) {
                params.setVideoStabilization(true);
            }
//            Camera.Size previewSize = getOptimalSize(params.getSupportedPreviewSizes(), ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
//            if (previewSize != null) {
//                params.setPreviewSize(previewSize.width, previewSize.height);
//            }
//            Camera.Size pictureSize = getOptimalSize(params.getSupportedPictureSizes(), ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight());
//            if (previewSize != null) {
//                params.setPictureSize(pictureSize.width, pictureSize.height);
//            }
//            mCamera.setParameters(params);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        try {
            if (mIsRecord) {
                return;
            }
            mIsRecord = true;

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            mMediaRecorder.setAudioEncodingBitRate(44100);
            if (mProfile.videoBitRate > 5 * 1024 * 1024) {
                mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
            } else {
                mMediaRecorder.setVideoEncodingBitRate(1024 * 1024);
            }
            mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
            mMediaRecorder.setOrientationHint(90);
            mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            mMediaRecorder.setOutputFile(mVideoPath);
            mCamera.unlock();
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        if (mMediaRecorder != null) {
            if (!mIsRecord) {
                return;
            }
            mIsRecord = false;
            ToastUtils.showShort("录像停止");
            scanFile(mVideoPath);
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void findView() {
        mSurfaceView = findViewById(R.id.sv);
    }

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

    public void onTakePicture(View view) {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File dir = new File(Environment.getExternalStorageDirectory(), "customImage");
                FileUtils.createOrExistsDir(dir);
                File imageFile = new File(dir, System.currentTimeMillis() + ".jpg");
                FileIOUtils.writeFileFromBytesByChannel(imageFile, data, true);
                ToastUtils.showShort("保存成功:" + imageFile.getAbsolutePath());
                scanFile(imageFile.getAbsolutePath());
                mCamera.startPreview();
            }
        });
    }

    public void onStartRecord(View view) {
        if (mIsRecord) {
            stopRecord();
        } else {
            startRecord();
        }
    }

    private void scanFile(String filePath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(filePath)));
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        try {
            mCamera.stopPreview();
            mCamera.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
