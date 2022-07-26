package com.het.cbeauty.cameralearndemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.het.cbeauty.cameralearndemo.databinding.ActivityCamera3Binding;
import com.het.cbeauty.cameralearndemo.utils.CameraUtils;
import com.het.cbeauty.cameralearndemo.utils.ImageSaveUtils;
import com.het.cbeauty.cameralearndemo.view.FaceCoverView;

import java.io.IOException;
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
 * @date 2022/7/4-10:49
 * @annotation 拍照
 */
public class CameraActivity3 extends AppCompatActivity {

    private static final String TAG = "testcamera3";
    private ActivityCamera3Binding mBinding;
    private Camera mCamera;
    private int mCameraID;
    private SurfaceHolder mHolder;
    private Handler mHandler;
    private boolean isStartFaceDetection = false;
    private boolean isMeterFace = false;
    private Camera.Area area;
    private boolean isAutoExposureLock;//是否锁定自动曝光


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCamera3Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mHandler = new Handler();
        initSurfaceView();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mClearFaceCoverRunnable);
    }

    private void initEvent() {
        mBinding.btnTakePhoto.setOnClickListener(v -> {
            mCamera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                    //快门回调发生在图像捕获之后
                    Log.d("testtakepicture", "onShutter");
                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    //当原始图像数据可用时，会发生原始回调
                    Log.d("testtakepicture", "onPictureTaken1:" + data);
                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    //当一个缩放的、完全处理过的postview图像可用时，会发生postview回调
                    Log.d("testtakepicture", "onPictureTaken2:" + data);
                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    //当压缩后的图像可用时，会发生jpeg回调。
                    Log.d("testtakepicture", "onPictureTaken3:" + data);
                    //保存原始图片
                    ImageSaveUtils.savePic(getApplicationContext(), data, "test2.png");
                    //保存经过旋转之后的图片
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Matrix matrix = CameraUtils.getPicMatrix(CameraActivity3.this, mCameraID);
                    Bitmap destBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    if (!bitmap.equals(destBitmap)) {
                        bitmap.recycle();
                    }
                    ImageSaveUtils.saveBitmap(getApplicationContext(), destBitmap, "testCopy2.png");
                    destBitmap.recycle();
                    //重新开启预览
                    reStartCamera();
                }
            });
        });
        mBinding.tvChangeCamera.setOnClickListener(v -> {
            mCameraID = (mCameraID + 1) % Camera.getNumberOfCameras();
            reStartCamera();
        });
        mBinding.coverView.setTouchCallBack((x, y, width, height) -> {
            if (!isStartFaceDetection) {
                //测试转换是否正确
//                    Matrix matrix = CameraUtils.getCameraSensorLocationMatrix(CameraActivity3.this, mCameraID, width, height);
//                    CameraSensorLocationTransitionTest.testTrans(matrix, width, height);
                //设置测光区域
                setMeteringAreas(x, y, width, height);

                //配合Camera.Parameters.FOCUS_MODE_AUTO模式，调用一次，对焦一次
//                mCamera.autoFocus(new Camera.AutoFocusCallback() {
//                    @Override
//                    public void onAutoFocus(boolean success, Camera camera) {
//                        Log.d("testsetfocusmode", "onAutoFocus :" + success);
//                    }
//                });
            }
        });
        mBinding.tvMeterType.setOnClickListener(v -> {
            isStartFaceDetection = !isStartFaceDetection;
            reStartCamera();
        });
        mBinding.isAutoExposure.setOnClickListener(view -> {
            if (mCamera != null) {
                isAutoExposureLock = !isAutoExposureLock;
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.isAutoExposureLockSupported()) {
                    parameters.setAutoExposureLock(isAutoExposureLock);
                    mCamera.setParameters(parameters);
                    refreshAutoExposureView();
                }
            }
        });
        mBinding.tvExposureSubtract.setOnClickListener(view -> {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                int level = parameters.getExposureCompensation();
                if (level - 1 >= parameters.getMinExposureCompensation()) {
                    parameters.setExposureCompensation(level - 1);
                }
                mCamera.setParameters(parameters);
                refreshExposureLevelView();
            }
        });
        mBinding.tvExposureAdd.setOnClickListener(view -> {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                int level = parameters.getExposureCompensation();
                if (level + 1 <= parameters.getMaxExposureCompensation()) {
                    parameters.setExposureCompensation(level + 1);
                }
                mCamera.setParameters(parameters);
                refreshExposureLevelView();
            }
        });
        mBinding.tvMeterFace.setOnClickListener(view -> {
            isMeterFace = !isMeterFace;
            refreshMeterFaceBtn();
        });
    }

    private void refreshExposureLevelView() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            int level = parameters.getExposureCompensation();
            mBinding.tvExposureLevel.setText(String.valueOf(level));
        }
    }

    private void setMeteringAreas(int x, int y, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getMaxNumMeteringAreas() > 0) {
            Matrix matrix = CameraUtils.getCameraSensorLocationMatrix(CameraActivity3.this, mCameraID, width, height);
            RectF rect = CameraUtils.getMeteringRect(x, y, Math.min(width, height) / 10);
            RectF destRect = new RectF();
            matrix.mapRect(destRect, rect);
            List<Camera.Area> areas = new ArrayList<>();
            Camera.Area area = CameraUtils.getMeteringArea(destRect);
            if (area != null) {
                Log.d("testtouch", "area:" + area.rect.toString());
                areas.add(area);
            }
            try {
                parameters.setMeteringAreas(areas);
                mCamera.setParameters(parameters);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private void reStartCamera() {
        releaseCamera();
        mCamera = Camera.open(mCameraID);
        setCallback();
        setCamera();
    }

    private void initSurfaceView() {
        mHolder = mBinding.surfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                //打开相机
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
                mCamera = Camera.open(mCameraID);
                setCallback();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                mCamera.stopPreview();
                setCamera();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                releaseCamera();
            }
        });
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setFaceDetectionListener(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCamera() {
        try {
            isAutoExposureLock = false;
            mCamera.setPreviewDisplay(mHolder);
            //设置预览图片的旋转角度
            mCamera.setDisplayOrientation(CameraUtils.getCameraDisplayOrientation(this, mCameraID));
            Camera.Parameters parameters = mCamera.getParameters();
            //设置预览大小
            setPreviewSize(parameters);
            //设置图片大小
            setPictureSize(parameters);
            //设置对焦模式
            setFocusMode(parameters);
            //设置是否曝光
            setAutoExposureMode(parameters);
            //设置白平衡
            setWhiteBalance(parameters);
            //设置parameters
            mCamera.setParameters(parameters);
            //启动预览
            mCamera.startPreview();
            //启动人脸检测
            startFaceDetection(parameters);
            //显示控制按钮
            refreshControlView();
            //刷新自动曝光的按钮
            refreshAutoExposureView();
            //刷新曝光等级
            refreshExposureLevelView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setWhiteBalance(Camera.Parameters parameters) {
        Log.d("testWhiteBalance", "WhiteBalance:" + parameters.getWhiteBalance());
        //用默认的
//        WHITE_BALANCE_AUTO
//                WHITE_BALANCE_INCANDESCENT
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
//        WHITE_BALANCE_FLUORESCENT
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
//                WHITE_BALANCE_WARM_FLUORESCENT
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT);
//        WHITE_BALANCE_DAYLIGHT
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
//                WHITE_BALANCE_CLOUDY_DAYLIGHT
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
//        WHITE_BALANCE_TWILIGHT
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_TWILIGHT);
//                WHITE_BALANCE_SHADE
//        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
    }

    private void refreshAutoExposureView() {
        mBinding.isAutoExposure.setText(isAutoExposureLock ? "锁定\n曝光" : "自动\n曝光");
    }

    private void setAutoExposureMode(Camera.Parameters parameters) {
        //测光
//        parameters.setExposureCompensation(2);
//        Log.d("testexposure","min:"+parameters.getMinExposureCompensation());
//        Log.d("testexposure","max:"+parameters.getMaxExposureCompensation());
//        Log.d("testexposure","step:"+parameters.getExposureCompensationStep());
        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(isAutoExposureLock);
            Log.d("testexposure", "isAutoExposureLockSupported:" + parameters.isAutoExposureLockSupported());
            Log.d("testexposure", "" + parameters.getAutoExposureLock());
        }
    }

    private void startFaceDetection(Camera.Parameters parameters) {
        if (isStartFaceDetection) {
            if (parameters.getMaxNumDetectedFaces() > 0) {
                mCamera.startFaceDetection();
            }
        }
    }

    private int num;

    private void setCallback() {
        mCamera.setPreviewCallback((data, camera) -> {
            //保存预览图片
//            savePreBitmap(data);
            //从预览图片分析人脸
            if (!isStartFaceDetection && isMeterFace) {
                faceDetector2(data);
            }
        });
        mCamera.setFaceDetectionListener((faces, camera) -> {
            Log.d("testfacedetection", "onFaceDetection:");
            if (faces != null) {
                Matrix matrix = CameraUtils.getViewLoactionMatrix(CameraActivity3.this, mCameraID, mBinding.surfaceView);
                List<FaceCoverView.Face> faceList = new ArrayList<>();
                for (Camera.Face rectF : faces) {
                    FaceCoverView.Face face = new FaceCoverView.Face();
                    if (rectF.rect != null) {
                        Log.d("testfacearea", "camera:" + rectF.rect);
                        RectF srcRect = new RectF(rectF.rect);
                        RectF dstRect = new RectF(0f, 0f, 0f, 0f);
                        //通过Matrix映射 将srcRect放入dstRect中
                        matrix.mapRect(dstRect, srcRect);
                        face.setRectF(dstRect);
                    }
                    //左眼
                    if (rectF.leftEye != null) {
                        float srcLeftEyes[] = new float[]{rectF.leftEye.x, rectF.leftEye.y};
                        float dstLeftEyes[] = new float[2];
                        matrix.mapPoints(dstLeftEyes, srcLeftEyes);
                        face.setLeftEye(dstLeftEyes);
                    }
                    //右眼
                    if (rectF.rightEye != null) {
                        float srcRightEyes[] = new float[]{rectF.rightEye.x, rectF.rightEye.y};
                        float dstRightEyes[] = new float[2];
                        matrix.mapPoints(dstRightEyes, srcRightEyes);
                        face.setRightEye(dstRightEyes);
                    }
                    //嘴巴
                    if (rectF.mouth != null) {
                        float srcMouth[] = new float[]{rectF.mouth.x, rectF.mouth.y};
                        float dstMouth[] = new float[2];
                        matrix.mapPoints(dstMouth, srcMouth);
                        face.setMouth(dstMouth);
                    }
                    faceList.add(face);
                }
                mBinding.coverView.setFacesForCameraFaceDetect(faceList);
                mHandler.removeCallbacks(mClearFaceCoverRunnable);
                mHandler.postDelayed(mClearFaceCoverRunnable, 500);
            }
        });
    }

    private int detectTime = 5;

    private long time1;
    private long time2;


    private void faceDetector2(byte[] data) {
        detectTime++;
        if (detectTime % 20 == 0) {
            long time1 = System.currentTimeMillis();
            Bitmap bitmap = ImageSaveUtils.previewDataToBitmap(data, mCamera);
            Log.d("facedetectortime", "time1:" + (System.currentTimeMillis() - time1));
            long time2 = System.currentTimeMillis();
            Matrix matrix = CameraUtils.getPicMatrix(CameraActivity3.this, mCameraID);
            Bitmap destBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            Log.d("facedetectortime", "time2:" + (System.currentTimeMillis() - time2));
            //横着的人脸无法识别
//        Bitmap destBitmap = bitmap;
            if (destBitmap != null) {
                FaceDetector faceDetector = new FaceDetector(destBitmap.getWidth(), destBitmap.getHeight(), 3);
                FaceDetector.Face[] faces = new FaceDetector.Face[3];
                faceDetector.findFaces(destBitmap, faces);

                //测试根据人脸区域设置测光区域
//                MeterAreaTest.testSetFaceMeterArea(CameraActivity3.this,
//                        mCamera,
//                        mBinding.coverView,
//                        mCameraID,
//                        destBitmap,
//                        faces
//                );

                setFaceMeterArea(destBitmap, faces);
            }
        }
    }

    private void setFaceMeterArea(Bitmap destBitmap, FaceDetector.Face[] faces) {
        List<FaceCoverView.Face> faceList = new ArrayList<>();
        List<Camera.Area> areas = new ArrayList<>();
        Matrix camera2ViewMatrix = CameraUtils.getViewLoactionMatrix(this, mCameraID, mBinding.coverView);
        for (FaceDetector.Face face : faces) {
            if (face != null && face.confidence() > 0.3) {
                Log.d("testpose", "EULER_X:" + face.pose(FaceDetector.Face.EULER_X));
                Log.d("testpose", "EULER_Y:" + face.pose(FaceDetector.Face.EULER_Y));
                Log.d("testpose", "EULER_Z:" + face.pose(FaceDetector.Face.EULER_Z));
                Camera.Area area = CameraUtils.getCameraArea(this,
                        mCameraID,
                        face,
                        destBitmap.getWidth(),
                        destBitmap.getHeight()
                );
                if (area != null) {
                    Log.d("testfacearea", "pic:" + area.rect);
                    areas.add(area);
                    //将camera坐标转成view的坐标
                    RectF viewRect = new RectF(0, 0, 0, 0);
                    camera2ViewMatrix.mapRect(viewRect, new RectF(area.rect.left, area.rect.top, area.rect.right, area.rect.bottom));
                    FaceCoverView.Face coverViewFace2 = new FaceCoverView.Face();
                    coverViewFace2.setRectF(viewRect);
                    faceList.add(coverViewFace2);
                }
            }
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getMaxNumMeteringAreas() > 0) {
            try {
                parameters.setMeteringAreas(areas);
                mCamera.setParameters(parameters);
            } catch (RuntimeException e) {
                e.printStackTrace();
                Log.d("testsetparametererror", "" + e.getMessage());
            }
        }
        mBinding.coverView.setFaces(faceList);
        mHandler.removeCallbacks(mClearFaceCoverRunnable);
        mHandler.postDelayed(mClearFaceCoverRunnable, 1000);
    }


    private void savePreBitmap(byte[] data) {
        if (num == 100) {
            num = 0;
        }
        if (num % 20 == 0) {
            Bitmap bitmap = ImageSaveUtils.previewDataToBitmap(data, mCamera);
            if (bitmap != null && bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                //保存原始图片
                ImageSaveUtils.saveBitmap(getApplicationContext(), bitmap, "testPre" + num + ".png");
                //保存旋转之后的图片
                Matrix matrix = CameraUtils.getPicMatrix(CameraActivity3.this, mCameraID);
                Bitmap destBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (!bitmap.equals(destBitmap)) {
                    bitmap.recycle();
                }
                ImageSaveUtils.saveBitmap(getApplicationContext(), destBitmap, "testPre" + num + "Copy.png");
                destBitmap.recycle();
            }
        }
        num++;
    }

    private Runnable mClearFaceCoverRunnable = new Runnable() {
        @Override
        public void run() {
            mBinding.coverView.clear();
        }
    };

    private void refreshControlView() {
        mBinding.btnTakePhoto.setVisibility(View.VISIBLE);
        mBinding.tvChangeCamera.setVisibility(View.VISIBLE);
        mBinding.tvMeterType.setVisibility(View.VISIBLE);
        refreshChangeCameraBtn();
        refreshMeterTypeBtn();
    }

    private void refreshMeterTypeBtn() {
        mBinding.tvMeterType.setText(isStartFaceDetection ? "人脸\n识别" : "区域\n测光");
        mBinding.tvMeterFace.setVisibility(isStartFaceDetection ? View.GONE : View.VISIBLE);
        refreshMeterFaceBtn();
    }

    private void refreshMeterFaceBtn() {
        mBinding.tvMeterFace.setText(isMeterFace ? "人脸测光:开" : "人脸测光:关");
    }

    private void refreshChangeCameraBtn() {
        mBinding.tvChangeCamera.setText(mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK ? "后置" : "前置");
    }

    private void setPictureSize(Camera.Parameters parameters) {
        Camera.Size size = CameraUtils.getPitureSize(parameters);
        if (size != null) {
            parameters.setPictureSize(size.width, size.height);
        }
    }

    private void setFocusMode(Camera.Parameters parameters) {
        String mode = CameraUtils.getFocusModeForPhoto(parameters);
        if (mode != null) {
            parameters.setFocusMode(mode);
            if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode)) {
//                mCamera.autoFocus(new Camera.AutoFocusCallback() {
//                    @Override
//                    public void onAutoFocus(boolean success, Camera camera) {
//                        Log.d("testsetfocusmode", "onAutoFocus :" + success);
//                    }
//                });
            }
        }
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        Camera.Size size = CameraUtils.getPreviewSize(this, mBinding.surfaceView, parameters);
        if (size != null) {
            parameters.setPreviewSize(size.width, size.height);
            int height = CameraUtils.isPortrait(this) ? mBinding.surfaceView.getHeight() : mBinding.surfaceView.getWidth();
            int width = CameraUtils.isPortrait(this) ? mBinding.surfaceView.getWidth() : mBinding.surfaceView.getHeight();
            float hChange = (height - width * (size.width * 1.0f / size.height)) / 2;
            reduceHeightForPreview(hChange);
        }
    }

    /**
     * 扩大或者缩小view的高度
     *
     * @param change 小于0：扩大view的高度；大于0：缩小view的高度
     */
    private void reduceHeightForPreview(float change) {
        Log.d("testpicturesize", "SurfaceHolder callback change:" + change);
        if (Math.abs(change) < 10) {
            //处理预览的时候出现白色背景的问题
            mBinding.surfaceView.setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        if (change > 0) {
            if (CameraUtils.isPortrait(this)) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.surfaceView.getLayoutParams();
                params.topMargin = (int) change;
                params.bottomMargin = (int) change;
            } else {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.surfaceView.getLayoutParams();
                params.leftMargin = (int) change;
                params.rightMargin = (int) change;
            }
        } else {
            if (CameraUtils.isPortrait(this)) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.surfaceView.getLayoutParams();
                params.height = (int) (mBinding.surfaceView.getHeight() - change * 2);
            } else {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.surfaceView.getLayoutParams();
                params.width = (int) (mBinding.surfaceView.getWidth() - change * 2);
            }
        }
    }
}
