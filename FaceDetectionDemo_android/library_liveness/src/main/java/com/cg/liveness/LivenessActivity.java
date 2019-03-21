package com.cg.liveness;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.oliveapp.camerasdk.CameraManager;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.LivenessDetectionFrames;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.OliveappFaceInfo;
import com.oliveapp.libcommon.utility.LogUtil;


/**
 * 样例活体检测Activity
 */
public class LivenessActivity extends LivenessDetectionMainActivity implements CameraManager.CameraPreviewDataCallback {

    public static final String TAG = LivenessActivity.class.getSimpleName();

    private ProgressDialog mProgressDialog;
    private byte[] data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 如果有设置全局包名的需要, 在这里进行设置
//        PackageNameManager.setPackageName();
        super.onCreate(savedInstanceState);
    }

    /**
     * =====================启动算法===============
     **/
    @Override
    public void onInitializeSucc() {
        super.onInitializeSucc();
        super.startVerification();
    }

    @Override
    public void onInitializeFail(Throwable e) {
        super.onInitializeFail(e);
        LogUtil.e(TAG, "无法初始化活体检测...", e);
        Toast.makeText(this, "无法初始化活体检测", Toast.LENGTH_LONG).show();

        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ((mProgressDialog != null) && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**===================================活体检测算法的回调==================================**/
    /**
     * 活体检测成功的回调
     *
     * @param livenessDetectionFrames 活体检测抓取的图片
     * @param faceInfo                捕获到的人脸信息
     */
    @Override
    public void onLivenessSuccess(final LivenessDetectionFrames livenessDetectionFrames, OliveappFaceInfo faceInfo) {
        super.onLivenessSuccess(livenessDetectionFrames, faceInfo);
        data = livenessDetectionFrames.verificationData;
        handleResult(1);
    }

    /**
     * 活检阶段失败
     */
    @Override
    public void onLivenessFail(int result, LivenessDetectionFrames livenessDetectionFrames) {
        super.onLivenessFail(result, livenessDetectionFrames);
        handleResult(0);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, CameraManager.CameraProxy cameraProxy, int i) {
        cameraProxy.getCamera();
    }

    private void handleResult(int status) {
        Intent intent = new Intent();
        intent.putExtra("status", status);
        setResult(RESULT_OK, intent);
        finish();
    }

}
