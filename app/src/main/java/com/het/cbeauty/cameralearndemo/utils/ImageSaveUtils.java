package com.het.cbeauty.cameralearndemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

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
 * @date 2022/6/25-15:02
 * @annotation ....
 */
public class ImageSaveUtils {

    private static final String TAG = "ImageSaveUtils";

    /**
     * 预览数据转换成bitmap
     *
     * @param data
     * @param camera
     * @return
     */
    public static Bitmap previewDataToBitmap(byte[] data, Camera camera) {
        ByteArrayOutputStream baos = null;
        try {
            //格式成YUV格式
            YuvImage yuvimage = new YuvImage(data, camera.getParameters().getPreviewFormat(), camera.getParameters().getPreviewSize().width,
                    camera.getParameters().getPreviewSize().height, null);
            baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width,
                    camera.getParameters().getPreviewSize().height), 100, baos);
            return bytes2Bimap(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 保存预览图片
     *
     * @param context
     * @param data
     * @param camera
     * @param name
     * @return
     */
    public static String savePrePic(Context context, byte[] data, Camera camera, String name) {
        Bitmap bitmap = previewDataToBitmap(data, camera);
        if (bitmap != null) {
            return saveBitmap(context, bitmap, name);
        }
        return null;
    }

    private static Bitmap bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
            return BitmapFactory.decodeByteArray(b, 0, b.length, options);
        } else {
            return null;
        }
    }

    public static String saveBitmap(Context context, Bitmap bitmap, String name) {
        File dirct = new File(FileUtils.getSavePath(context), "mycamera");
        if (!dirct.exists()) {
            dirct.mkdirs();
        }
        File pictureFile = new File(dirct, name);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return null;
        }
        //重新写入文件
        try {
            // 写入文件
            FileOutputStream fos;
            fos = new FileOutputStream(pictureFile);
            //默认jpg
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return pictureFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String savePic(Context context, byte[] data, String name) {
        File dirct = new File(FileUtils.getSavePath(context), "mycamera");
        if (!dirct.exists()) {
            dirct.mkdirs();
        }
        File pictureFile = new File(dirct, name);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return null;
        }

        try {
            Log.d(TAG, "data.size:" + data.length);
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            return pictureFile.getAbsolutePath();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return null;
    }
}
