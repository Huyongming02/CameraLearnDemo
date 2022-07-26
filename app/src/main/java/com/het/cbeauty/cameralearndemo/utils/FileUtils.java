package com.het.cbeauty.cameralearndemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ------------------------------------------------
 * Copyright © 2014-2021 CLife. All Rights Reserved.
 * Shenzhen H&T Intelligent Control Co.,Ltd.
 * -----------------------------------------------
 *
 * @author huyongming
 * @version v3.1.0
 * @date 2022/6/18-16:09
 * @annotation ....
 */
public class FileUtils {
    public static Bitmap bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    public static Bitmap decodeToBitMap(Context context, byte[] data, Camera camera, int num) {
        try {
            //格式成YUV格式
            YuvImage yuvimage = new YuvImage(data, camera.getParameters().getPreviewFormat(), camera.getParameters().getPreviewSize().width,
                    camera.getParameters().getPreviewSize().height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width,
                    camera.getParameters().getPreviewSize().height), 100, baos);
            Bitmap bitmap = bytes2Bimap(baos.toByteArray());
            saveImage(context, bitmap, num);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void saveImage(Context context, Bitmap bmp, int num) {
        File appDir = new File(getSavePath(context), "MyImage");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = "pre_" + num + ".png";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSavePath(Context context) {
        String path;
        if (Build.VERSION.SDK_INT > 29) {
            path = context.getExternalFilesDir(null).getAbsolutePath();
        } else {
            path = Environment.getExternalStorageDirectory().getPath();
        }
        return path;
    }

    public static String getVideoPath(Context context, String name) {
        File dirct = new File(FileUtils.getSavePath(context), "myvideo");
        if (!dirct.exists()) {
            dirct.mkdirs();
        }
        File pictureFile = new File(dirct, name);
        if (pictureFile == null) {
            return null;
        } else {
            return pictureFile.getAbsolutePath();
        }
    }
}
