package com.het.cbeauty.cameralearndemo.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ------------------------------------------------
 * Copyright © 2014-2021 CLife. All Rights Reserved.
 * Shenzhen H&T Intelligent Control Co.,Ltd.
 * -----------------------------------------------
 *
 * @author huyongming
 * @version v3.1.0
 * @date 2022/6/25-13:56
 * @annotation ....
 */
public class CameraUtils {

    /**
     * 获取预览图像需要的旋转角度（保证预览方向正确）
     *
     * @param activity
     * @param cameraId
     */
    public static int getCameraDisplayOrientation(Activity activity,
                                                  int cameraId) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        //rotation是预览Window的旋转方向，对于手机而言，当在清单文件设置Activity的screenOrientation="portait"时，
        //rotation=0，这时候没有旋转，当screenOrientation="landScape"时，rotation=1。
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        Log.d("DisplayOrientation", "roation:" + rotation + ";info.orientation:" + info.orientation);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0://竖屏
                degrees = 0;
                break;
            case Surface.ROTATION_90://横屏，左边在下
                degrees = 90;
                break;
            case Surface.ROTATION_180://竖屏，底边在上
                degrees = 180;
                break;
            case Surface.ROTATION_270://横屏，右边在下
                degrees = 270;
                break;
        }
        //info.orientation:前置-270；后置-90

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 获取屏幕坐标转换到相机传感器坐标的矩阵
     *
     * @param activity
     * @param cameraId
     * @return
     */
    public static Matrix getCameraSensorLocationMatrix(Activity activity, int cameraId, int width, int height) {
        Matrix matrix = new Matrix();
        //缩放
        matrix.postScale(2000f / width, 2000f / height);
        //平移
        matrix.postTranslate(-1000f, -1000f);
        //旋转
        matrix.postRotate((360 - getCameraDisplayOrientation(activity, cameraId)) % 360);
        //前置摄像头需要左右对调
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //前置需要镜像
            matrix.postScale(-1f, 1f);
        } else {
            matrix.postScale(1f, 1f);
        }
        return matrix;
    }

    /**
     * 计算测光区域
     *
     * @param rectF
     * @return
     */
    public static Camera.Area getMeteringArea(RectF rectF) {
        Rect rect = getLegalAreaRect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
        if (rect != null) {
            return new Camera.Area(rect, 1000);
        } else {
            return null;
        }
    }

    public static RectF getMeteringRect(int x, int y, int radius) {
        return new RectF(x - radius, y - radius, x + radius, y + radius);
    }

    public static Rect checkAreaRect(Rect rect) {
        int left = checkRange(rect.left);
        int top = checkRange(rect.top);
        int right = checkRange(rect.right);
        int bottom = checkRange(rect.bottom);
        if (left == right || top == bottom) {
            return null;
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }

    /**
     * 根据预览回调中的图片（调整后的竖着的人脸），经过人脸检测之后得到的Face信息，计算测光区域
     *
     * @param activity
     * @param cameraId
     * @param face
     * @param bitmapWidth
     * @param bitmapHeight
     * @return
     */
    public static Camera.Area getCameraArea(Activity activity, int cameraId, FaceDetector.Face face, int bitmapWidth, int bitmapHeight) {
        if (face != null) {
            PointF pointF = new PointF();
            face.getMidPoint(pointF);
            //根据eyemid和eyedistance计算一个矩形
            RectF rect = new RectF(pointF.x - face.eyesDistance(), pointF.y - face.eyesDistance() * 0.5f, pointF.x + face.eyesDistance(), pointF.y + face.eyesDistance() * 1.5f);
            //将矩形坐标转成camera坐标
            Matrix matrix = getCameraSensorLocationMatrix(activity, cameraId, bitmapWidth, bitmapHeight);
            RectF cameraRect = new RectF(0, 0, 0, 0);
            matrix.mapRect(cameraRect, rect);
            Rect checkedRect = checkAreaRect(new Rect((int) cameraRect.left, (int) cameraRect.top, (int) cameraRect.right, (int) cameraRect.bottom));
            if (checkedRect != null) {
                return new Camera.Area(checkedRect, 1000);
            }
        }
        return null;
    }

    private static Rect getLegalAreaRect(int left, int top, int right, int bottom) {
        left = checkRange(left);
        top = checkRange(top);
        right = checkRange(right);
        bottom = checkRange(bottom);
        if (left == right || top == bottom) {
            return null;
        } else {
            return new Rect(left, top, right, bottom);
        }
    }

    private static int checkRange(int x) {
        if (x < -1000) {
            x = -1000;
        } else if (x > 1000) {
            x = 1000;
        }
        return x;
    }

    /**
     * 获取人脸坐标转换矩阵(https://developer.android.google.cn/reference/android/hardware/Camera.Face#mouth)
     *
     * @param activity
     * @param cameraId
     * @param view
     * @return
     */
    public static Matrix getViewLoactionMatrix(Activity activity, int cameraId, View view) {
        Matrix matrix = new Matrix();//矩阵的转换结果和转换的设置顺序有关
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //前置需要镜像
            matrix.setScale(-1f, 1f);
        } else {
            matrix.setScale(1f, 1f);
        }
        //后乘旋转角度
        matrix.postRotate(CameraUtils.getCameraDisplayOrientation(activity, cameraId));
        //后乘缩放
        matrix.postScale(view.getWidth() / 2000f, view.getHeight() / 2000f);
        //再进行位移
        matrix.postTranslate(view.getWidth() / 2f, view.getHeight() / 2f);
        return matrix;
    }

    /**
     * 旋转图片（不用这种方式）
     *
     * @param cameraId    前置还是后置
     * @param orientation 拍照时传感器方向
     * @param orgPath     图片路径
     */
    @Deprecated
    public static void rotateImageView(int cameraId, int orientation, String orgPath, String destPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(orgPath);
        Matrix matrix = new Matrix();
        matrix.postRotate(Float.valueOf(orientation));
        // 创建新的图片
        Bitmap resizedBitmap;

        if (cameraId == 1) {
            if (orientation == 90) {
                matrix.postRotate(180f);
            }
        }
        // 创建新的图片
        resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        //新增 如果是前置 需要镜面翻转处理
        if (cameraId == 1) {
            Matrix matrix1 = new Matrix();
            matrix1.postScale(-1f, 1f);
            resizedBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0,
                    resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix1, true);

        }


        File file = new File(destPath);
        //重新写入文件
        try {
            // 写入文件
            FileOutputStream fos;
            fos = new FileOutputStream(file);
            //默认jpg
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            resizedBitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }


    /**
     * 获取旋转预览和拍照原始图片的矩阵
     * <p>
     * 照片方向特征
     * 1. 预览回调的图片和拍照得到的图片方向一致，镜像一致
     * 2. 后置摄像头的预览回调图片和预览本身一致
     * 3. 前置摄像头的预览本身做了镜像，预览回调图片没有镜像，保存图片是需要我们自己做镜像
     * 4. 前置摄像头的预览方向横屏和图片一致，竖屏和图片相反，所以保存图片的时候需要在预览的旋转基础上，再旋转180度
     * <p>
     * 前置摄像头-预览图像和预览回调图片之间的关系：
     * 预览回调图片是预览图像的镜像（可以先做镜像，在按预览图像的旋转角度旋转即可）
     *
     * @param activity
     * @param cameraId
     * @return
     */
    public static Matrix getPicMatrix(Activity activity, int cameraId) {
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotate = getCameraDisplayOrientation(activity, cameraId);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //方案一：先旋转，再镜像
//            if (isPortrait(activity)) {
//                rotate += 180;
//                rotate %= 360;
//            }
//            matrix.setRotate(rotate);
//            matrix.postScale(-1f, 1f);

            //方法二:预览回调图片和预览图像之间左右镜像，所以可以先做镜像，再按预览图像的旋转角度旋转
            matrix.postScale(-1f, 1f);
            matrix.postRotate(rotate);
        } else {
            matrix.postRotate(rotate);
        }
        return matrix;
    }


    public static int getRotate(Activity activity, int cameraId) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int rotate = 0;
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            /**
             * 竖屏上（rotation=0）| 270|90
             * 横左下（rotation=1）| 0|0
             * 竖屏下（rotation=2）|90|270
             * 横右下（rotation=3）|180|180
             */
            switch (rotation) {
                case Surface.ROTATION_0://竖屏
                    rotate = 90;
                    break;
                case Surface.ROTATION_90://横屏，左边在下
                    rotate = 0;
                    break;
                case Surface.ROTATION_180://竖屏，底边在上
                    rotate = 270;
                    break;
                case Surface.ROTATION_270://横屏，右边在下
                    rotate = 180;
                    break;
            }
        } else {
            /**
             * 竖屏上（rotation=0）| 90|270
             * 横左下（rotation=1）| 0|0
             * 竖屏下（rotation=2）|270|90
             * 横右下（rotation=3）|180|180
             */
            switch (rotation) {
                case Surface.ROTATION_0://竖屏
                    rotate = 270;
                    break;
                case Surface.ROTATION_90://横屏，左边在下
                    rotate = 0;
                    break;
                case Surface.ROTATION_180://竖屏，底边在上
                    rotate = 90;
                    break;
                case Surface.ROTATION_270://横屏，右边在下
                    rotate = 180;
                    break;
            }
        }

        return rotate;
    }

    public static void rotateImageView2(int rotate, int cameraId, String orgPath, String desPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(orgPath);
        Matrix matrix = new Matrix();
        matrix.setRotate(rotate);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        //新增 如果是前置 需要镜面翻转处理
        if (cameraId == 1) {
            Matrix matrix1 = new Matrix();
            matrix1.postScale(-1f, 1f);
            resizedBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0,
                    resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix1, true);

        }


        File file = new File(desPath);
        //重新写入文件
        try {
            // 写入文件
            FileOutputStream fos;
            fos = new FileOutputStream(file);
            //默认jpg
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            resizedBitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 计算合适的图片大小（取和预览宽高比一致的，没有则取分辨率最大的）
     *
     * @param parameters
     * @return
     */
    public static Camera.Size getPitureSize(Camera.Parameters parameters) {
        Camera.Size previewSize = parameters.getPreviewSize();
        List<Camera.Size> sizeList = parameters.getSupportedPictureSizes();
        if (previewSize != null && sizeList != null && sizeList.size() > 0) {
            //寻找宽高比一致的，从中选图片最大的
            Camera.Size temp = null;
            for (Camera.Size size : sizeList) {
                if (size.height * previewSize.width == size.width * previewSize.height) {
                    if (temp == null) {
                        temp = size;
                    } else {
                        if (size.width * size.height > temp.width * temp.height) {
                            temp = size;
                        }
                    }
                }
            }
            //找分辨率最大的
            if (temp == null) {
                for (Camera.Size size : sizeList) {
                    if (temp == null) {
                        temp = size;
                    } else {
                        if (size.width * size.height > temp.width * temp.height) {
                            temp = size;
                        }
                    }
                }
            }
            return temp;
        }
        return null;
    }

    /**
     * 计算合适的预览大小
     *
     * @param activity
     * @param view
     * @param parameters
     * @return
     */
    public static Camera.Size getPreviewSize(Activity activity, View view, Camera.Parameters parameters) {
        boolean isPortrait = isPortrait(activity);
        List<Camera.Size> localSizes = parameters.getSupportedPreviewSizes();
        if (localSizes != null && localSizes.size() > 0) {
            //过滤分辨率比较小的
            List<Camera.Size> filterList = new ArrayList<>();
            float viewSize = view.getWidth() * view.getHeight();
            for (int i = 0; i < localSizes.size(); i++) {
                Camera.Size size = localSizes.get(i);
                //过滤分辨率比较小的(不同设备上预览的分辨率和屏幕的分辨率差距较大，这个阀值应该根据具体设备调整)
                if (size.width * size.height / viewSize > 0.3) {
                    filterList.add(size);
                }
            }
            int height = isPortrait ? view.getHeight() : view.getWidth();
            int width = isPortrait ? view.getWidth() : view.getHeight();

            if (filterList.size() > 0) {
                return getPreviewSize(filterList, height, width);
            }
            return localSizes.get(0);
        }
        return null;
    }

    private static Camera.Size getPreviewSize(List<Camera.Size> filterList, int height, int width) {
        Camera.Size fitSize = getPreviewSize1(filterList, height, width);
        if (fitSize != null) {
            return fitSize;
        }
        return getPreviewSize2(filterList, height, width);
    }

    /**
     * 获取高宽比大于view的高宽比的值最小的Size
     *
     * @param filterList
     * @param height
     * @param width
     * @return
     */
    @Nullable
    private static Camera.Size getPreviewSize1(List<Camera.Size> filterList, int height, int width) {
        Camera.Size fitSize1 = null;
        float lastRatio = 0;
        float viewRatio = height * 1.0f / width;
        for (int i = 0; i < filterList.size(); i++) {
            Camera.Size temp = filterList.get(i);
            float ratio = temp.width * 1.0f / temp.height;
            if (ratio <= viewRatio && ratio > lastRatio) {
                lastRatio = ratio;
                fitSize1 = temp;
            }
        }
        return fitSize1;
    }

    /**
     * 获取高宽比比view的高宽比小的最大值
     *
     * @param filterList
     * @param height
     * @param width
     * @return
     */
    @Nullable
    private static Camera.Size getPreviewSize2(List<Camera.Size> filterList, int height, int width) {
        Camera.Size fitSize = null;
        float lastRatio = 100;
        float viewRatio = height * 1.0f / width;
        for (int i = 0; i < filterList.size(); i++) {
            Camera.Size temp = filterList.get(i);
            float ratio = temp.width * 1.0f / temp.height;
            if (ratio >= viewRatio && ratio < lastRatio) {
                lastRatio = ratio;
                fitSize = temp;
            }
        }
        return fitSize;
    }


    /**
     * 获取对焦模式
     *
     * @param parameters
     * @return
     */
    public static String getFocusModeForPhoto(Camera.Parameters parameters) {
        if (isSupportFocusModoe(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, parameters)) {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        } else if (isSupportFocusModoe(Camera.Parameters.FOCUS_MODE_AUTO, parameters)) {
            return Camera.Parameters.FOCUS_MODE_AUTO;
        }
        return null;
    }

    private static boolean isSupportFocusModoe(String destMode, Camera.Parameters parameters) {
        List<String> modes = parameters.getSupportedFocusModes();
        if (modes != null) {
            for (String mode : modes) {
                if (destMode.equals(mode)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否是竖屏
     *
     * @param activity
     * @return
     */
    public static boolean isPortrait(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        if (rotation == 0 || rotation == 2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Iterate over supported camera video sizes to see which one best fits the
     * dimensions of the given view while maintaining the aspect ratio. If none can,
     * be lenient with the aspect ratio.
     *
     * @param supportedVideoSizes Supported camera video sizes.
     * @param previewSizes        Supported camera preview sizes.
     * @param w                   The width of the view.
     * @param h                   The height of the view.
     * @return Best match camera video size to fit in the view.
     */
    public static Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes,
                                                  List<Camera.Size> previewSizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        List<Camera.Size> videoSizes;
        if (supportedVideoSizes != null) {
            videoSizes = supportedVideoSizes;
        } else {
            videoSizes = previewSizes;
        }
        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
