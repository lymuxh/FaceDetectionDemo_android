package com.cg.camera;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.cg.cameralibrary.R;

import java.io.File;

public class RecordVideoActivity extends AppCompatActivity {

    private RecordVideoView mRecordView;
    private boolean isRecording;
    private String mPath;
    private boolean mIsPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_record_video);
        mRecordView = findViewById(R.id.record_video);

        mPath = new File(Environment.getExternalStorageDirectory(), "backups/video.mp4").getAbsolutePath();
    }

    public void startRecord(View view) {
        if (isRecording) {
            mRecordView.stopRecord();
            Toast.makeText(this, mPath, Toast.LENGTH_SHORT).show();
        } else {
            mRecordView.startRecord(mPath);
        }
        isRecording = !isRecording;
    }

    public void onChangeFlash(View view) {
        mRecordView.toggleFlash();
    }

    public void onChangeCamera(View view) {
        mRecordView.changeCamera();
    }

    public void onPause(View view) {
        if (mIsPause) {
            mRecordView.resume();
        } else {
            mRecordView.pause();
        }
        mIsPause = !mIsPause;
    }
}
