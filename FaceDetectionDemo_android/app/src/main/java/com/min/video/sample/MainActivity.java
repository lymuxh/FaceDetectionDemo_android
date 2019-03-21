package com.min.video.sample;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.cg.camera.RecordVideoActivity;
import com.cg.codec.DecoderVideoActivity;
import com.cg.codec.EncoderVideoActivity;
import com.cg.face.detect.FdActivity;
import com.cg.liveness.LivenessActivity;
import com.cg.speaker.SpeakerClient;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LIVENESS = 10001;
    private static final int REQUEST_CODE_FACE_DETECT = 10002;

    public static String[] permissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionUtils.permission(permissions)
                .rationale(new PermissionUtils.OnRationaleListener() {
                    @Override
                    public void rationale(final ShouldRequest shouldRequest) {
                        ToastUtils.showShort("rationale");
                    }
                })
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                        ToastUtils.showShort("授权成功");
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        ToastUtils.showShort("授权失败：" + permissionsDeniedForever.toString());
                    }
                })
                .request();
    }

    public void onLivenessClick(View view) {
        Intent intent = new Intent(this, LivenessActivity.class);
        intent.putExtra(LivenessActivity.KEY_USE_FRONT_CAMERA, true);
        startActivityForResult(intent, REQUEST_CODE_LIVENESS);
    }

    public void onFaceClick(View view) {
        Intent intent = new Intent(this, FdActivity.class);
        intent.putExtra(FdActivity.KEY_USE_FRONT_CAMERA, true);
        startActivityForResult(intent, REQUEST_CODE_FACE_DETECT);
    }

    public void onSpeakClick(View view) {
        SpeakerClient.getInstance().speak("你好吗?", new SpeakerClient.SpeakCallback() {
            @Override
            public void onStart() {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError() {

            }
        });
    }

    public void onRecordVideo(View view) {
        Intent intent = new Intent(this, RecordVideoActivity.class);
        startActivity(intent);
    }

    public void onVideoEncode(View view) {
        Intent intent = new Intent(this, EncoderVideoActivity.class);
        startActivity(intent);
    }

    public void onVideoDecode(View view) {
        Intent intent = new Intent(this, DecoderVideoActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQUEST_CODE_LIVENESS) {
            int status = data.getIntExtra("status", 0);
            String statusStr = status == 0 ? "失败" : "成功";
            ToastUtils.showLong("活体检测结果：" + statusStr);
        } else if (requestCode == REQUEST_CODE_FACE_DETECT) {
            String path = data.getStringExtra("path");
            ToastUtils.showLong("人脸检测成功:" + path);
        }
    }

}
