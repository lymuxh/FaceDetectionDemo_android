package com.cg.liveness;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.cg.liveness.utils.AudioModule;
import com.cg.liveness.utils.OliveappAnimationHelper;
import com.cg.liveness.utils.SampleScreenDisplayHelper;
import com.oliveapp.camerasdk.PhotoModule;
import com.oliveapp.camerasdk.utils.CameraUtil;
import com.oliveapp.face.livenessdetectionviewsdk.event_interface.ViewUpdateEventHandlerIf;
import com.oliveapp.face.livenessdetectionviewsdk.verification_controller.VerificationController;
import com.oliveapp.face.livenessdetectionviewsdk.verification_controller.VerificationControllerFactory;
import com.oliveapp.face.livenessdetectionviewsdk.verification_controller.VerificationControllerIf;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.FacialActionType;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.ImageProcessParameter;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.LivenessDetectionFrames;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.LivenessDetectorConfig;
import com.oliveapp.face.livenessdetectorsdk.livenessdetector.datatype.OliveappFaceInfo;
import com.oliveapp.face.livenessdetectorsdk.prestartvalidator.datatype.PrestartDetectionFrame;
import com.oliveapp.libcommon.utility.LogUtil;
import com.oliveapp.libcommon.utility.PackageNameManager;

import java.util.ArrayList;

import static com.cg.liveness.utils.SampleScreenDisplayHelper.OrientationType.LANDSCAPE;
import static com.cg.liveness.utils.SampleScreenDisplayHelper.OrientationType.PORTRAIT;
import static com.oliveapp.camerasdk.utils.CameraUtil.CAMERA_RATIO_16_9;

/**
 * ViewController 实现了主要的界面逻辑
 * 如果需要定义界面，请继承此类编写自己的Activity，并自己实现事件响应函数
 * 可参考SampleAPP里的ExampleLivenessActivity
 */

public abstract class LivenessDetectionMainActivity extends Activity implements ViewUpdateEventHandlerIf {

    public static final String TAG = LivenessDetectionMainActivity.class.getSimpleName();
    public static final String KEY_USE_FRONT_CAMERA = "useFrontCamera";


    /**
     * 以下4个变量定义了界面中人脸框的位置，采用百分比形式，要根据UI和屏幕比例动态调整，可参考下面代码。
     * 比如一张宽高为900 * 1600的图片，以下变量定义的位置为
     * 左上角(x, y) => (900 * 0.15, 1600 * 0.25) = (135, 400)
     * 人脸框的宽和高(width,height) => (900 * 0.7, 1600 * 0.5) = （630,800)
     */
    private static float mXPercent = 0.271f;
    private static float mYPercent = 0.274f;
    private static float mWidthPercent = 0.735f;
    private static float mHeightPercent = 0.414f;

    private PhotoModule mPhotoModule; // 摄像头模块
    private AudioModule mAudioModule; // 音频播放模块

    private TextView mOliveappDetectedDirectText; //头像上方的指示图像“请眨眼”

    private ImageButton mCloseImageBtn; //关闭按钮
    private ImageView mCapFrame;
    private TextView mCountDownTextView;

    /**
     * 默认无预检动作
     * WITHOUT_PRESTART,无预检过程
     * WITH_PRESTART,有预检过程 (推荐)
     */
    private VerificationControllerFactory.VCType mVerificationControllerType = VerificationControllerFactory.VCType.WITH_PRESTART;

    private OliveappAnimationHelper mAnimController; //控制动画播放
    private static Handler mAnimationHanlder = null; //播放动画的Handler
    private ArrayList<Pair<Double, Double>> mLocationArray; //五官位置
    private int mCurrentActionType;
    private boolean mIsPrestart = false;


    /**
     * ====================================生命周期相关函数====================================
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        LogUtil.i(TAG, "[BEGIN] LivenessDetectionMainActivity::onCreate()");
        /**
         * 有些插件系统用普通方法获取不到PackageName，此代码用于兼容
         */
        if (!PackageNameManager.isPackageNameSet()) {
            PackageNameManager.setPackageName(getPackageName());
        }
        PackageNameManager.setPackageName(PackageNameManager.getPackageName());
        super.onCreate(savedInstanceState);


        // 初始化界面元素
        initViews();
        // 初始化摄像头
        initCamera();
        // 初始化检测逻辑控制器(VerificationController)
        initControllers();

        /**
         * 如果有预检的话播放预检相关动画和音频
         */
        if (mVerificationControllerType == VerificationControllerFactory.VCType.WITH_PRESTART) {
            mAnimationHanlder.post(mPreHintAnimation);
            mIsPrestart = true;
        } else {
            mIsPrestart = false;
        }

        LogUtil.i(TAG, "[END] LivenessDetectionMainActivity::onCreate()");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        LogUtil.i(TAG, "[BEGIN] LivenessDetectionMainActivity::onResume()");
        super.onResume();
        if (mPhotoModule != null) {
            mPhotoModule.onResume();
            // 设置摄像头回调，自此之后VerificationController.onPreviewFrame函数就会源源不断的收到摄像头的数据


        }
        try {
            mPhotoModule.setPreviewDataCallback(mVerificationController, mCameraHandler);
        } catch (NullPointerException e) {
            LogUtil.e(TAG, "PhotoModule set callback failed", e);
        }

        if (mAnimationHanlder != null) {
            if (mIsPrestart) {
                mAnimationHanlder.post(mPreHintAnimation);
            } else {
                mAnimationHanlder.post(mActionAnimationTask);
            }
        }
        LogUtil.i(TAG, "[END] LivenessDetectionMainActivity::onResume()");
    }


    @Override
    protected void onPause() {
        LogUtil.i(TAG, "[BEGIN] LivenessDetectionMainActivity::onPause()");
        super.onPause();

        if (mPhotoModule != null) {
            mPhotoModule.onPause();
        }

        if (mAnimationHanlder != null) {
            mAnimationHanlder.removeCallbacksAndMessages(null);
        }
        LogUtil.i(TAG, "[END] LivenessDetectionMainActivity::onPause()");


    }

    @Override
    protected void onStop() {
        LogUtil.i(TAG, "[BEGIN] LivenessDetectionMainActivity::onStop()");
        super.onStop();
        LogUtil.i(TAG, "[END] LivenessDetectionMainActivity::onStop()");
    }

    @Override
    protected void onDestroy() {
        LogUtil.i(TAG, "[BEGIN] LivenessDetectionMainActivity::onDestroy()");
        super.onDestroy();
        // 关闭摄像头
        if (mPhotoModule != null)
            mPhotoModule.onStop();
        CameraUtil.sContext = null;
        mPhotoModule = null;

        // 关闭音频播放
        if (mAnimationHanlder != null) {
            mAnimationHanlder.removeCallbacksAndMessages(null);
            mAnimationHanlder = null;
        }
        if (mAudioModule != null) {
            mAudioModule.release();
            mAudioModule = null;
        }

        // 退出摄像头处理线程
        if (mCameraHandlerThread != null) {
            try {
                mCameraHandlerThread.quit();
                mCameraHandlerThread.join();
            } catch (InterruptedException e) {
                LogUtil.e(TAG, "Fail to join CameraHandlerThread", e);
            }
        }
        mCameraHandlerThread = null;

        // 销毁检测逻辑控制器
        if (mVerificationController != null) {
            mVerificationController.uninit();
            mVerificationController = null;
        }
        LogUtil.i(TAG, "[END] LivenessDetectionMainActivity::onDestroy()");
    }

    /**
     * ====================================初始化相关函数====================================
     **/

    private VerificationControllerIf mVerificationController; // 逻辑控制器
    private Handler mCameraHandler; // 摄像头回调所在的消息队列
    private HandlerThread mCameraHandlerThread; // 摄像头回调所在的消息队列线程

    /**
     * 初始化并打开摄像头
     */
    private void initCamera() {
        LogUtil.i(TAG, "[BEGIN] initCamera");

        // 寻找设备上的前置摄像头
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int expectCameraFacing;
        boolean useFrontCameraFlag = getIntent().getBooleanExtra(KEY_USE_FRONT_CAMERA, false);
        if (useFrontCameraFlag) {
            expectCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            expectCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        }


        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);

            LogUtil.i(TAG, "camera id: " + camIdx + ", facing: " + cameraInfo.facing + ", expect facing: " + expectCameraFacing);
            if (cameraInfo.facing == expectCameraFacing) {
                getIntent().putExtra(CameraUtil.EXTRAS_CAMERA_FACING, camIdx); // 设置需要打开的摄像头ID
                getIntent().putExtra(CameraUtil.TARGET_PREVIEW_RATIO, CAMERA_RATIO_16_9); // 设置Preview长宽比,默认是16:9
            }
        }
        mPhotoModule = new PhotoModule();
        mPhotoModule.init(this, findViewById(R.id.oliveapp_cameraPreviewView)); // 参考layout XML文件里定义的cameraPreviewView对象
        mPhotoModule.setPlaneMode(false, false); // 取消拍照和对焦功能
        // 打开摄像头预览
        mPhotoModule.onStart();
        // 初始化摄像头处理消息队列
        mCameraHandlerThread = new HandlerThread("CameraHandlerThread");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());

        LogUtil.i(TAG, "[END] initCamera");
    }

    /**
     * 初始化界面元素
     */
    private void initViews() {

        //设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(decideWhichLayout());

        //音频和动画的Handler
        mAudioModule = new AudioModule();
        mAnimationHanlder = new Handler();
        //进入活检页面就开启动画
        mAnimController = new OliveappAnimationHelper(this);
        // DEBUG: 调试相关
        mFrameRateText = (TextView) findViewById(R.id.oliveapp_frame_rate_text);

        mFrameRateText.setVisibility(View.GONE);


        mOliveappDetectedDirectText = (TextView) findViewById(R.id.oliveapp_detected_hint_text);
        mOliveappDetectedDirectText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mOliveappDetectedDirectText.getPaint().setFakeBoldText(true);
        // 倒计时的TextView
        mCountDownTextView = (TextView) findViewById(R.id.oliveapp_count_time_textview);

        /**
         * 设置脸部捕捉框的位置，请根据自己的UI进行调整
         * 因为手机和平板的屏幕比例不同，并且适配不同比例的背景图也不同导致需要相应的适配
         */
        mCapFrame = (ImageView) findViewById(R.id.oliveapp_start_frame);
        if (!SampleScreenDisplayHelper.ifThisIsPhone(this)) {
            PercentRelativeLayout.LayoutParams layoutParams = new PercentRelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            PercentLayoutHelper.PercentLayoutInfo layoutInfo = layoutParams.getPercentLayoutInfo();
            if (SampleScreenDisplayHelper.getFixedOrientation(this) == LANDSCAPE) {
                layoutInfo.topMarginPercent = 0.2f;
                layoutInfo.heightPercent = 0.6f;
                layoutInfo.widthPercent = layoutInfo.heightPercent / (float) SampleScreenDisplayHelper.getScreenScale(this);
                layoutInfo.leftMarginPercent = (1 - layoutInfo.widthPercent) / 2;
            } else {
                layoutInfo.leftMarginPercent = 0.13f;
                layoutInfo.widthPercent = 0.74f;
                layoutInfo.heightPercent = layoutInfo.widthPercent / (float) SampleScreenDisplayHelper.getScreenScale(this);
                layoutInfo.topMarginPercent = (1 - layoutInfo.heightPercent) / 2 - 0.022f;
            }
            mCapFrame.setLayoutParams(layoutParams);
        }
        /**
         * UI相关
         * 提示文字的布局要动态调整
         * 只有平板竖屏的时候要调整
         */
        if ((SampleScreenDisplayHelper.getFixedOrientation(this) == PORTRAIT) && (!SampleScreenDisplayHelper.ifThisIsPhone(this))) {
            PercentRelativeLayout hintLayout = (PercentRelativeLayout) findViewById(R.id.oliveapp_detected_hint_text_layout);
            PercentRelativeLayout.LayoutParams params = new PercentRelativeLayout.LayoutParams(0, 0);
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.widthPercent = 1f;
            info.heightPercent = 0.052f;
            info.topMarginPercent = mYPercent - info.heightPercent;
            info.leftMarginPercent = 0;
            hintLayout.setLayoutParams(params);
        }
    }

    // 图片预处理参数
    private ImageProcessParameter mImageProcessParameter;
    // 活体检测参数
    private LivenessDetectorConfig mLivenessDetectorConfig;

    /**
     * 设置图片处理参数和活体检测参数
     */
    private void setDetectionParameter() throws Exception {
        /**
         * 注意: 默认参数适合手机，一般情况下不需要修改这些参数。如需修改请联系依图工程师
         *
         * 设置从preview图片中截取人脸框的位置，调用doDetection前必须调用本函数。
         * @param shouldFlip 是否左右翻转。一般前置摄像头为false
         * @param cropWidthPercent　截取的人脸框宽度占帧宽度的比例
         * @param verticalOffsetPercent　截取的人脸框上边缘到帧上边缘的距离占帧高度的比例
         * @param preRotationDegree　逆时针旋转角度，只允许0 90 180 270，大部分手机应当是90
         */
        mImageProcessParameter = new ImageProcessParameter(false, 1.0f, 0.0f, 90);

        // 使用预设配置: 满足绝大多数常见场景
        mLivenessDetectorConfig = new LivenessDetectorConfig();
        mLivenessDetectorConfig.usePredefinedConfig(0);

        if (mLivenessDetectorConfig != null) {
            mLivenessDetectorConfig.validate();
        }

        //如果超时时间太长的话便不显示倒计时
        if (mLivenessDetectorConfig.timeoutMs >= 100000) {
            mCountDownTextView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 初始化检测逻辑控制器
     * 请先调用setDetectionParameter()设置参数
     */
    private void initControllers() {
        try {
            setDetectionParameter();
        } catch (Exception e) {
            LogUtil.e(TAG, "初始化参数失败", e);
        }
        //初始化算法模块
        mVerificationController = VerificationControllerFactory.createVerificationController(mVerificationControllerType, LivenessDetectionMainActivity.this,
                mImageProcessParameter,
                mLivenessDetectorConfig,
                LivenessDetectionMainActivity.this,
                new Handler(Looper.getMainLooper()));
        //设置人脸框位置
        mVerificationController.SetFaceLocation(mXPercent, mYPercent, mWidthPercent, mHeightPercent);

    }

    /**===========================算法相关函数==============================**/

    /**
     * 调用此函数后活体检测即开始
     */
    public void startVerification() {
        try {
            if (mVerificationController.getCurrentStep() == VerificationController.STEP_READY) {
                mVerificationController.nextVerificationStep();
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "无法开始活体检测...", e);
        }
    }

    /** ============================== 算法相关的回调函数 ============================**/

    /**
     * 预检阶段成功
     * 请在里面调用mVerificationController.enterLivenessDetection()进入活体检测阶段
     */
    @Override
    public void onPrestartSuccess(LivenessDetectionFrames livenessDetectionFrames, OliveappFaceInfo faceInfo) {
        LogUtil.i(TAG, "[BEGIN] onPrestartSuccess");
        mAnimationHanlder.removeCallbacks(mPreHintAnimation);
        mIsPrestart = false;
        /**
         * 调用此函数进入活体检测过程
         */
        mVerificationController.enterLivenessDetection();
        LogUtil.i(TAG, "[END] onPrestartSuccess");
    }


    /**
     * 预检阶段每一帧的回调函数
     *
     * @param frame                    每一帧
     * @param remainingTimeMillisecond 剩余时间
     * @param faceInfo                 人脸信息
     * @param errorCodeOfInAction      动作不过关的可能原因，可以用来做提示语
     */
    @Override
    public void onPrestartFrameDetected(PrestartDetectionFrame frame, int remainingTimeMillisecond, OliveappFaceInfo faceInfo, ArrayList<Integer> errorCodeOfInAction) {
        mFrameRate += 1;
        long currentTimestamp = System.currentTimeMillis();
        if ((currentTimestamp - mLastTimestamp) > 1000) {
            mLastTimestamp = currentTimestamp;
            mFrameRateText.setText("FrameRate: " + mFrameRate + " FPS");
            mFrameRate = 0;
        }
    }

    /**
     * 预检阶段失败，不可能进入此回调
     */
    @Override
    public void onPrestartFail(int result) {
//        LogUtil.wtf(TAG, "[END] onPrestartFail");
//
//        Intent intent = new Intent(LivenessDetectionMainActivity.this, SampleUnusualResultActivity.class);
//        intent.putExtra(SampleUnusualResultActivity.keyToGetExtra, SampleUnusualResultActivity.PRESTART_FAIL);
//        startActivity(intent);
    }

    /**
     * 活体检测成功的回调
     *
     * @param livenessDetectionFrames 活体检测抓取的图片
     * @param faceInfo                捕获到的人脸信息
     */
    @Override
    public void onLivenessSuccess(LivenessDetectionFrames livenessDetectionFrames, OliveappFaceInfo faceInfo) {
        // 关闭音频播放
        if (mAnimationHanlder != null) {
            mAnimationHanlder.removeCallbacksAndMessages(null);
            mAnimationHanlder = null;
        }
        if (mAudioModule != null) {
            mAudioModule.release();
            mAudioModule = null;
        }
    }

    /**
     * 活检阶段失败
     */
    @Override
    public void onLivenessFail(int result, LivenessDetectionFrames livenessDetectionFrames) {

    }

    /**
     * 每一帧结果的回调方法
     *
     * @param currentActionType           当前是什么动作
     * @param actionState                 当前动作的检测结果
     * @param sessionState                整个Session是否通过
     * @param remainingTimeoutMilliSecond 剩余时间，以毫秒为单位
     * @param faceInfo                    检测到的人脸信息，可以用来做动画
     * @param errorCodeOfInAction         动作不过关的可能原因，可以用来做提示语
     */
    @Override
    public void onFrameDetected(int currentActionType, int actionState, int sessionState, int remainingTimeoutMilliSecond, OliveappFaceInfo faceInfo, ArrayList<Integer> errorCodeOfInAction) {

        LogUtil.i(TAG, "[BEGIN] onFrameDetected " + remainingTimeoutMilliSecond);
        mCountDownTextView.setText("" + (remainingTimeoutMilliSecond / 1000 + 1));
        mLocationArray = getFaceInfoLocation(mCurrentActionType, faceInfo);
        mFrameRate += 1;
        long currentTimestamp = System.currentTimeMillis();
        if ((currentTimestamp - mLastTimestamp) > 1000) {
            mLastTimestamp = currentTimestamp;
            mFrameRateText.setText("FrameRate: " + mFrameRate + " FPS");
            mFrameRate = 0;
        }
        LogUtil.i(TAG, "[END] onFrameDetected");
    }

    /**
     * 切换到下一个动作时的回调方法
     *
     * @param lastActionType     上一个动作类型
     * @param lastActionResult   上一个动作的检测结果
     * @param newActionType      当前新生成的动作类型
     * @param currentActionIndex 当前是第几个动作
     * @param faceInfo           人脸的信息
     */
    public void onActionChanged(int lastActionType, int lastActionResult, int newActionType, int currentActionIndex, OliveappFaceInfo faceInfo) {
        try {
            // 更新提示文字
            String hintText;
            switch (newActionType) {
                case FacialActionType.MOUTH_OPEN:
                    hintText = getString(R.string.oliveapp_step_hint_mouthopen);
                    break;
                case FacialActionType.EYE_CLOSE:
                    hintText = getString(R.string.oliveapp_step_hint_eyeclose);
                    break;
                case FacialActionType.HEAD_UP:
                    hintText = getString(R.string.oliveapp_step_hint_headup);
                    break;
                default:
                    hintText = getString(R.string.oliveapp_step_hint_focus);
            }
            mAnimController.playHintTextAnimation(hintText);
            mLocationArray = getFaceInfoLocation(newActionType, faceInfo);
            mCurrentActionType = newActionType;
            mAnimationHanlder.removeCallbacksAndMessages(null);
            mAnimationHanlder.post(mActionAnimationTask);
        } catch (Exception e) {
            LogUtil.i(TAG, "changeToNextAction interrupt");
        }

    }

    /**==========================================一些辅助函数==============================**/

    /**
     * 拿到当前动作对应的脸部坐标，用于做动画
     *
     * @param actionType 动作类型
     * @param faceInfo   对应的人脸信息
     * @return 脸部坐标数组
     */
    private ArrayList<Pair<Double, Double>> getFaceInfoLocation(int actionType, OliveappFaceInfo faceInfo) {
        ArrayList<Pair<Double, Double>> result = new ArrayList<Pair<Double, Double>>();
        switch (actionType) {
            case FacialActionType.EYE_CLOSE: {
                result.add(faceInfo.leftEye);
                result.add(faceInfo.rightEye);
                break;
            }
            case FacialActionType.MOUTH_OPEN: {
                result.add(faceInfo.mouthCenter);
                break;
            }
            case FacialActionType.HEAD_UP: {
                result.add(faceInfo.chin);
                break;
            }
        }
        return result;
    }

    //========================根据设置决定本Activity要采用哪个layout=======================//
    private int decideWhichLayout() {

        int layout = R.layout.oliveapp_sample_liveness_detection_main_portrait_phone;
        //选择布局文件
        switch (SampleScreenDisplayHelper.getFixedOrientation(this)) {
//            case PORTRAIT:
//                if (SampleScreenDisplayHelper.ifThisIsPhone(this)) {
//                    layout = R.layout.oliveapp_sample_liveness_detection_main_portrait_phone;
//                } else {
//                    layout = R.layout.oliveapp_sample_liveness_detection_main_portrait_tablet;
//                }
//                break;
            case LANDSCAPE:
                if (!SampleScreenDisplayHelper.ifThisIsPhone(this)) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                    layout = R.layout.oliveapp_sample_liveness_detection_main_landscape;
                } else {
//                    layout = R.layout.oliveapp_sample_liveness_detection_main_portrait_phone;
                }

                break;

        }
        return layout;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;
            mXPercent = mCapFrame.getX() / width - 0.1f;
            mYPercent = mCapFrame.getY() / height - 0.1f;
            mWidthPercent = (float) mCapFrame.getWidth() / width + 0.1f;
            mHeightPercent = (float) mCapFrame.getHeight() / height + 0.1f;
        }
    }

    /////////////////// FOR DEBUG //////////////////////
    private TextView mFrameRateText;
    private long mLastTimestamp = System.currentTimeMillis();
    private int mFrameRate = 0;


    /**
     * 播放动作动画和音频的Runnable
     */
    private Runnable mActionAnimationTask = new Runnable() {
        @Override
        public void run() {


            if (mAudioModule != null && mAnimController != null) {
                mAudioModule.playAudio(LivenessDetectionMainActivity.this, FacialActionType.getStringResourceName(mCurrentActionType));
                mAnimController.playActionAnimation(mCurrentActionType, mLocationArray);
                mAnimationHanlder.postDelayed(this, 2500);
            }
        }
    };

    /**
     * 播放预检动画和音频的Runnable
     */
    private Runnable mPreHintAnimation = new Runnable() {
        @Override
        public void run() {
            if (mAudioModule != null && mAnimController != null) {
                mAudioModule.playAudio(LivenessDetectionMainActivity.this, FacialActionType.getStringResourceName(FacialActionType.CAPTURE));
                mAnimController.playAperture();
                mAnimationHanlder.postDelayed(this, 2500);
            }
        }
    };

    /**
     * 算法初始化成功
     */
    public void onInitializeSucc() {
    }

    public void onInitializeFail(Throwable e) {

    }
}
