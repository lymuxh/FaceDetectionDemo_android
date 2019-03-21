package com.min.video.sample.base;

import android.app.Application;

import com.blankj.utilcode.util.Utils;
import com.cg.speaker.SpeakerClient;

/**
 * Created by minych on 18-11-1.
 */

public class App extends Application {

    private String appId = "14456110";

    private String appKey = "Y2H2drktiWpSEu2b9eLRcGZx";

    private String secretKey = "ok7Xai5YO8lc4Yxd2iM5UwWQUCC2OWBL";

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        SpeakerClient.getInstance().init(this, appId, appKey, secretKey);
    }
}
