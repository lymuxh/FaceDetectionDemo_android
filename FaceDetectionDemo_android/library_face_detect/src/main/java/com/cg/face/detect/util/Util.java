package com.cg.face.detect.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Util {

    public static final String DIR_NAME = "face_detect";

    private static Toast mToast;

    public static void toast(Context context, String text) {
        if (mToast == null) {
            mToast = Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }

    public static String saveBitmap(Context context, Bitmap bitmap) {
        try {
            File dirFile = getFileDir(context, DIR_NAME);
            File picFile = new File(dirFile, System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(picFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return picFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File getFileDir(Context context, String name) {
        File externalFilesDir = context.getExternalFilesDir(null);
        File dirFile = new File(externalFilesDir, name);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        return dirFile;
    }

}
