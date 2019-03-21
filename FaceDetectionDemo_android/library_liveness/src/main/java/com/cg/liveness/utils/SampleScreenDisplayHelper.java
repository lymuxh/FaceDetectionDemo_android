package com.cg.liveness.utils;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by cli on 1/10/17.
 */

public class SampleScreenDisplayHelper {

    public static String TAG = SampleScreenDisplayHelper.class.getSimpleName();
    public static final double MIN_TABLET_SIZE = 7.0;
    /**
     * 设置设备是显示横屏还是竖屏
     * OrientationType枚举中有两种选项
     * PORTRAIT代表竖屏
     * LANDSCAPE代表横屏
     */
    public enum OrientationType {
        PORTRAIT, LANDSCAPE
    }

    /**
     * 请在这里设置决定要采用的屏幕方向
     * 默认都是竖屏
     * 若要自定义，请重写getFixedOrientation(Context)方法
     */
    public static OrientationType getFixedOrientation(Context context) {

        /**
         *  手机只有竖屏，平板根据摄像头位置可以设置横屏或者竖屏
         *  若要设置横屏可返回OrientationType.LANDSCAPE
         *  然后在具体的Activity的decideWhichLayout()方法中返回相应的layout值；
         */
        if (ifThisIsPhone(context)) {
            return OrientationType.PORTRAIT;
        } else {


            return OrientationType.PORTRAIT;
        }
    }


    /**
     * 判断本机是手机还是平板
     * 若是手机，返回true
     */
    public static boolean ifThisIsPhone(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        int widthPixels = dm.widthPixels;
        int heightPixels = dm.heightPixels;
        double diagonalPixels = Math.sqrt(new Double(widthPixels * widthPixels + heightPixels * heightPixels));
        double dpi = dm.densityDpi;
        double physicalSize = diagonalPixels / dpi;
        double scale = new Double(heightPixels) / new Double(widthPixels);
        double standard_16_9 = new Double(16).doubleValue() / new Double(9).doubleValue();
        double standard_4_3 = new Double(4).doubleValue() / new Double(3).doubleValue();
        double acceptable = 0.2;

        if (physicalSize > MIN_TABLET_SIZE) {
            return false;
        } else {
            if (Math.abs(scale - standard_16_9) < acceptable) {
                return true;
            }
            return false;
        }
    }

    /**
     * 获取屏幕比例
     * @param context
     * @return
     */
    public static double getScreenScale(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        int widthPixels = dm.widthPixels;
        int heightPixels = dm.heightPixels;
        double scale = new Double(heightPixels) / new Double(widthPixels);
        return scale;
    }

}
