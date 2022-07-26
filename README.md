# CameraLearnDemo
本示例演示了一些相机开发的基本功能，包括
1. 相机开发的基本步骤
2. 相机开发中的一些配置
3. 相机功能设置
4. 使用FaceDetector识别人脸
5. 整理的几个工具方法

## 1. 相机开发的基本步骤
1. 声明硬件特性
2. 声明需要的权限和动态请求权限
3. 在布局中添加预览类和设置监听器
4. 打开相机和预览
5. 拍照
6. 释放相机

## 1.1 声明硬件特性
在清单文件中声明硬件特性
```
<uses-feature android:name="android.hardware.camera" />
```
## 1.2 声明需要的权限和动态请求权限
声明相机和读写外部存储的权限
```
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
动态申请权限
```
private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
  

 private void checkPerission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (int i = 0; i < permissions.length; i++) {
                int permission = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 321);
                    break;
                }
            }
        }
    }  
```

## 1.3 在布局中添加预览类和设置监听器
在布局中添加SurfaceView作为相机的预览类
```
<SurfaceView
    android:id="@+id/surfaceView"
    android:layout_width="0dp"
    android:layout_height="0dp"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent" />
```
设置SurfaceView的状态回调
```
private void initSurfaceView() {
        mHolder = mBinding.surfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
            //surface创建的时候回调
                //打开相机
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
                mCamera = Camera.open(mCameraID);
                setCallback();
            }
        
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            //surface改变的时候回调
                mCamera.stopPreview();
                setCamera();
            }
        
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            //surface销毁的时候回调
                releaseCamera();
            }
        });
        }
```
一般在surface创建的时候打开相机预览，在surface被销毁的时候释放相机，当surface发生变化的时候需要关闭预览，重新设置相机并打开预览

## 1.4 打开相机和预览
打开前置摄像头，并开启预览
```
mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
mCamera = Camera.open(mCameraID);
//启动预览
mCamera.startPreview();                
```

## 1.5 拍照
```
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
                }
            });
```

## 1.6 释放相机
```
  private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setFaceDetectionListener(null);
            mCamera.release();
            mCamera = null;
        }
    }
```
# 2. 相机开发中的一些配置
1. 预览旋转角度设置
2. 设置合适的预览大小
3. 设置合适的图片大小
4. 保存图片

## 2.1 预览旋转角度设置
相机默认的预览方向和我们期待的预览方向不一致，需要经过一个转换之后才能得到正确的预览效果。不过这个预览旋转角度的值，官网给出了计算公式[Camera.setDisplayOrientation(int orientation)](https://developer.android.google.cn/reference/android/hardware/Camera?hl=zh-cn#setDisplayOrientation(int))
```
 public static void setCameraDisplayOrientation(Activity activity,
         int cameraId, android.hardware.Camera camera) {
     android.hardware.Camera.CameraInfo info =
             new android.hardware.Camera.CameraInfo();
     android.hardware.Camera.getCameraInfo(cameraId, info);
     int rotation = activity.getWindowManager().getDefaultDisplay()
             .getRotation();
     int degrees = 0;
     switch (rotation) {
         case Surface.ROTATION_0: degrees = 0; break;
         case Surface.ROTATION_90: degrees = 90; break;
         case Surface.ROTATION_180: degrees = 180; break;
         case Surface.ROTATION_270: degrees = 270; break;
     }

     int result;
     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
         result = (info.orientation + degrees) % 360;
         result = (360 - result) % 360;  // compensate the mirror
     } else {  // back-facing
         result = (info.orientation - degrees + 360) % 360;
     }
     camera.setDisplayOrientation(result);
 }
 
```

## 2.2 设置合适的预览大小
每一个相机有一个它支持的预览大小的列表，我们给相机设计预览大小的时候，只能从这个列表中选择。一般选择和屏幕宽高比较为接近的分辨率适当的预览大小，然后再对SurfaceView的大小做一个调整，以保证预览不变形。

## 2.3 设置合适的图片大小
一般根据预览的大小，选择一个和预览比例一致的，分辨率最大的的大小作为图片的大小。

## 2.4 保存图片
拍照中的jpeg图片、预览回调图片的方向和相机传感器的视角一致，不受Camera.setDisplayOrientation(int orientation)的影响。预览回调的方向和传感器一致，不过在前置摄像头下，默认做了水平翻转，以便让前置摄像头看起来成镜像。所以我们在保存图片的时候，后置摄像头的图片我们直接按照“预览旋转角度设置”处理即可，而前置摄像头的图片先做一个水平镜像，再按照“预览旋转角度设置”处理即可。
```
public static Matrix getPicMatrix(Activity activity, int cameraId) {
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotate = getCameraDisplayOrientation(activity, cameraId);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //预览回调图片和预览图像之间左右镜像，所以可以先做镜像，再按预览图像的旋转角度旋转
            matrix.postScale(-1f, 1f);
            matrix.postRotate(rotate);
        } else {
            matrix.postRotate(rotate);
        }
        return matrix;
    }
```
应用矩阵，转换bitmap
```
Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
Matrix matrix = CameraUtils.getPicMatrix(CameraActivity3.this, mCameraID);
Bitmap destBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
```

# 3. 相机功能设置
1. 设置对焦模式
2. 启动人脸检测
3. 手动设置测光区域和对焦区域

## 3.1 设置对焦模式
拍照的时候，我们优先使用Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE。设置之前需要先判断相机支不支持这个对焦模式，根据parameters.getSupportedFocusModes()中是否包含要设置的模式来判断相机是否支持这个对焦模式。设置对焦模式：
```
parameters.setFocusMode(mode)
```

常用的对焦模式
1. Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE：用于拍照的连续自动对焦模式
2. Camera.Parameters.FOCUS_MODE_AUTO：需要配合Camera.autoFocus(android.hardware.Camera.AutoFocusCallback)一起使用，调用一次，对焦一次
3. Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO：用于录像的连续自动对焦模式，焦点变化比较平稳，没有FOCUS_MODE_CONTINUOUS_PICTURE快


## 3.2 启动人脸检测
1. 检查是否支持人脸检测:根据parameters.getMaxNumDetectedFaces()是否大于0来表示是否支持人脸检测
2. 创建并添加人脸检测监听器到相机对象
```
mCamera.setFaceDetectionListener((faces, camera) -> {
    
});
```
3. 在预览后开启人脸检测
```
if (parameters.getMaxNumDetectedFaces() > 0) {
                mCamera.startFaceDetection();
}
```
### 3.2.1 人脸检测中回调的人脸信息Face
Face
```
 public static class Face {
        public int id = -1;
        public Point leftEye;
        public Point mouth;
        public Rect rect;
        public Point rightEye;
        public int score;

    }
```
这里返回的坐标是传感器视角下的坐标，左上点为（-1000，-1000），右下点未（1000,1000）。到SurfaceView坐标的转换官网已经给出。[Camera.FAce.rect字段说明](https://developer.android.google.cn/reference/android/hardware/Camera.Face?hl=zh-cn#rect)
```
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

```

## 3.3 设置测光区域
1. 根据params.getMaxNumMeteringAreas()是否大于0来判断是否支持设置测光区域
2. 计算测光区域：先在view上计算一个矩形区域作为测光区域
```
public static RectF getMeteringRect(int x, int y, int radius) {
    return new RectF(x - radius, y - radius, x + radius, y + radius);
}
```
3. 转换测光区域坐标：测光区域的坐标是相机传感器视角下的坐标，我们可以根据上面人脸检测的坐标来计算。人脸检测的坐标是将相机传感器视角下的坐标转换成SurfaceView上的坐标，现在我们是将SurfaceView上的坐标转换成传感器上的坐标，转换公式如下
```
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
```
3. 设置测光区域：应用上面的转换公式将view上的测光区域转换成相机传感器视角下的测光区域
```
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
```

如果要设置对焦区域，方式和设置测光区域类似


# 4. 使用FaceDetector识别人脸
使用FaceDetector来识别Bitmap中的人脸。FaceDetector对识别的Bitmap有要求，格式必须是Bitmap.Config.RGB_565的，且只能识别竖着的人脸。
```
FaceDetector faceDetector = new FaceDetector(destBitmap.getWidth(), destBitmap.getHeight(), 3);
FaceDetector.Face[] faces = new FaceDetector.Face[3];
faceDetector.findFaces(destBitmap, faces);
```
Face中信息
```
 public class Face {
        public static final float CONFIDENCE_THRESHOLD = 0.4F;
        public static final int EULER_X = 0;
        public static final int EULER_Y = 1;
        public static final int EULER_Z = 2;

        Face() {
            throw new RuntimeException("Stub!");
        }

        public float confidence() {
            throw new RuntimeException("Stub!");
        }

        public void getMidPoint(PointF point) {
            throw new RuntimeException("Stub!");
        }

        public float eyesDistance() {
            throw new RuntimeException("Stub!");
        }

        public float pose(int euler) {
            throw new RuntimeException("Stub!");
        }
    }
```
1. confidence()：可信度，值的范围为0-1，一般超过0.3则认为可信了
2. eyesDistance()：眼间距，值相对于bitmap的大小
3. getMidPoint():两眼中点，值相对于bitmap的大小

## 4.1 设置人脸测光区域
使用FaceDetector识别到的人脸区域设置测光区域（设置与点击屏幕设置测光区域一致）
1. 先相对于bitmap的基础上计算一个测光区域
```
 PointF pointF = new PointF();
face.getMidPoint(pointF);
//根据eyemid和eyedistance计算一个矩形
RectF rect = new RectF(pointF.x - face.eyesDistance(), pointF.y - face.eyesDistance() * 0.5f, pointF.x + face.eyesDistance(), pointF.y + face.eyesDistance() * 1.5f);
```
2. 将SurfaceView的测光区域转换成相机传感器的测光区域
```
//将矩形坐标转成camera坐标
Matrix matrix = getCameraSensorLocationMatrix(activity, cameraId, bitmapWidth, bitmapHeight);
RectF cameraRect = new RectF(0, 0, 0, 0);
matrix.mapRect(cameraRect, rect);
```
完整计算代码
```
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

```
设置
```
Camera.Parameters parameters = mCamera.getParameters();
if (parameters.getMaxNumMeteringAreas() > 0) {
    try {
        List<Camera.Area> areas = new ArrayList<>();
        for (FaceDetector.Face face : faces) {
            if (face != null && face.confidence() > 0.3) {
                Camera.Area area = CameraUtils.getCameraArea(this,
                        mCameraID,
                        face,
                        destBitmap.getWidth(),
                        destBitmap.getHeight()
                );
                if (area != null) {
                    Log.d("testfacearea", "pic:" + area.rect);
                    areas.add(area);
                }
            }
        }
        parameters.setMeteringAreas(areas);
        mCamera.setParameters(parameters);
    } catch (RuntimeException e) {
        e.printStackTrace();
        Log.d("testsetparametererror", "" + e.getMessage());
    }
}
```
# 5. 整理的几个工具方法
1. 计算预览图像的旋转角度
2. 计算预览大小
3. 计算图片大小
4. 相机传感器坐标转View的坐标
5. View的坐标转相机传感器坐标
6. 图片旋转

## 5.1 计算预览图像的旋转角度
```
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
```
## 5.2 计算预览大小
```
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

```
## 5.3 计算图片大小
```
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

```
## 5.4 相机传感器坐标转View的坐标
```
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
```
## 5.5 View的坐标转相机传感器坐标
```
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
```
## 5.6 图片旋转
```
public static Matrix getPicMatrix(Activity activity, int cameraId) {
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotate = getCameraDisplayOrientation(activity, cameraId);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //预览回调图片和预览图像之间左右镜像，所以可以先做镜像，再按预览图像的旋转角度旋转
            matrix.postScale(-1f, 1f);
            matrix.postRotate(rotate);
        } else {
            matrix.postRotate(rotate);
        }
        return matrix;
    }
```
