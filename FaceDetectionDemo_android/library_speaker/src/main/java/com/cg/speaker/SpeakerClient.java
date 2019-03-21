package com.cg.speaker;

import android.content.Context;
import android.media.AudioManager;

import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

/**
 * Created by minych on 18-10-17.
 */

public class SpeakerClient {

    private String appId = "14456110";

    private String appKey = "Y2H2drktiWpSEu2b9eLRcGZx";

    private String secretKey = "ok7Xai5YO8lc4Yxd2iM5UwWQUCC2OWBL";

    private SpeechSynthesizer speechSynthesizer;

    private SpeakCallback speakCallback;

    private Context context;

    private static SpeakerClient speakerClient;

    private SpeakerClient() {
    }

    public static SpeakerClient getInstance() {
        if (speakerClient == null) {
            synchronized (SpeakerClient.class) {
                if (speakerClient == null) {
                    speakerClient = new SpeakerClient();
                }
            }
        }
        return speakerClient;
    }

    public void init(Context context, String appId, String appKey, String secretKey) {
        if (speechSynthesizer != null) {
            return;
        }
        this.context = context;
        this.appId = appId;
        this.appKey = appKey;
        this.secretKey = secretKey;
        initTTS();
    }

    private void initTTS() {
        speechSynthesizer = SpeechSynthesizer.getInstance();
        speechSynthesizer.setContext(context);
        speechSynthesizer.setSpeechSynthesizerListener(new SpeechSynthesizerListener() {
            @Override
            public void onSynthesizeStart(String s) {
                SpeakerUtil.log("合成开始 : " + s);
                if (speakCallback != null) {
                    SpeakerUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speakCallback.onStart();
                        }
                    });
                }
            }

            @Override
            public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {
            }

            @Override
            public void onSynthesizeFinish(String s) {
                SpeakerUtil.log("合成结束 : " + s);
            }

            @Override
            public void onSpeechStart(String s) {
                SpeakerUtil.log("播放开始 : " + s);
            }

            @Override
            public void onSpeechProgressChanged(String s, int i) {

            }

            @Override
            public void onSpeechFinish(String s) {
                SpeakerUtil.log("播放结束 : " + s);
                if (speakCallback != null) {
                    SpeakerUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speakCallback.onComplete();
                        }
                    });
                }
            }

            @Override
            public void onError(String s, SpeechError speechError) {
                SpeakerUtil.log("合成播放失败 : " + s);
                if (speakCallback != null) {
                    SpeakerUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speakCallback.onError();
                        }
                    });
                }
            }
        });
        speechSynthesizer.setAppId(appId);
        speechSynthesizer.setApiKey(appKey, secretKey);
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
        speechSynthesizer.setAudioStreamType(AudioManager.MODE_IN_CALL);
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_ENCODE, SpeechSynthesizer.AUDIO_ENCODE_PCM);
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_RATE, SpeechSynthesizer.AUDIO_BITRATE_PCM);
        int result = speechSynthesizer.initTts(TtsMode.ONLINE);
        SpeakerUtil.log("初始化语音合成完成 : " + result);
    }

    public void destroy() {
        speechSynthesizer.stop();
        speechSynthesizer.release();
        speechSynthesizer = null;
    }

    public void speak(String text, SpeakCallback speakCallback) {
        this.speakCallback = speakCallback;
        speechSynthesizer.speak(text);
    }

    public void speak(String text) {
        this.speakCallback = null;
        speechSynthesizer.speak(text);
    }

    public interface SpeakCallback {

        void onStart();

        void onComplete();

        void onError();

    }
}
