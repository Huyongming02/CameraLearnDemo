package com.het.cbeauty.cameralearndemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * ------------------------------------------------
 * Copyright © 2014-2021 CLife. All Rights Reserved.
 * Shenzhen H&T Intelligent Control Co.,Ltd.
 * -----------------------------------------------
 *
 * @author huyongming
 * @version v3.1.0
 * @date 2022/6/25-10:16
 * @annotation 绘制人脸区域
 */
public class FaceCoverView extends View {

    private Paint mPaint;
    private Paint mPaintLeftEye;
    private Paint mPaintRightEye;
    private Paint mPaintMouth;

    private Paint mPaint2;
    private Paint mPaintLeftEye2;
    private Paint mPaintRightEye2;
    private Paint mPaintMouth2;

    private List<Face> mFaces;
    private List<Face> mFaces2;
    private List<Point> mPoints;

    private TouchCallBack mTouchCallBack;

    public void setTouchCallBack(TouchCallBack mTouchCallBack) {
        this.mTouchCallBack = mTouchCallBack;
    }

    public FaceCoverView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeWidth(2);

        mPaint2 = new Paint(mPaint);
        mPaint2.setColor(Color.YELLOW);

        mPaintLeftEye = new Paint(mPaint);
        mPaintLeftEye.setStyle(Paint.Style.FILL);
        mPaintLeftEye.setStrokeCap(Paint.Cap.ROUND);
        mPaintLeftEye.setStrokeWidth(20);
        mPaintLeftEye2 = new Paint(mPaintLeftEye);
        mPaintLeftEye2.setColor(Color.YELLOW);

        mPaintRightEye = new Paint(mPaint);
        mPaintRightEye.setStyle(Paint.Style.FILL);
        mPaintRightEye.setStrokeCap(Paint.Cap.ROUND);
        mPaintRightEye.setColor(Color.YELLOW);
        mPaintRightEye.setStrokeWidth(20);
        mPaintRightEye2 = new Paint(mPaintRightEye);
        mPaintRightEye2.setColor(Color.YELLOW);

        mPaintMouth = new Paint(mPaint);
        mPaintMouth.setStyle(Paint.Style.FILL);
        mPaintMouth.setStrokeCap(Paint.Cap.ROUND);
        mPaintMouth.setStrokeWidth(20);
        mPaintMouth2 = new Paint(mPaintMouth);
        mPaintMouth2.setColor(Color.YELLOW);
    }


    public void setFaces(List<Face> mFaces) {
        this.mFaces = mFaces;
        invalidate();
    }

    public void setFacesForCameraFaceDetect(List<Face> mFaces) {
        this.mFaces2 = mFaces;
        invalidate();
    }

    public void setPoints(List<Point> mPoints) {
        this.mPoints = mPoints;
        invalidate();
    }

    public void clear() {
        this.mFaces = null;
        this.mFaces2 = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mFaces != null) {
            for (Face face : mFaces) {
                if (face.getRectF() != null) {
                    canvas.drawRect(face.getRectF(), mPaint);
                }
                if (face.getLeftEye() != null && face.getLeftEye().length == 2) {
                    canvas.drawPoint(face.getLeftEye()[0], face.getLeftEye()[1], mPaintLeftEye);
                }
                if (face.getRightEye() != null && face.getRightEye().length == 2) {
                    canvas.drawPoint(face.getRightEye()[0], face.getRightEye()[1], mPaintRightEye);
                }
                if (face.getMouth() != null && face.getMouth().length == 2) {
                    canvas.drawPoint(face.getMouth()[0], face.getMouth()[1], mPaintMouth);
                }
            }
        }
        if (mFaces2 != null) {
            for (Face face : mFaces2) {
                if (face.getRectF() != null) {
                    canvas.drawRect(face.getRectF(), mPaint2);
                }
                if (face.getLeftEye() != null && face.getLeftEye().length == 2) {
                    canvas.drawPoint(face.getLeftEye()[0], face.getLeftEye()[1], mPaintLeftEye2);
                }
                if (face.getRightEye() != null && face.getRightEye().length == 2) {
                    canvas.drawPoint(face.getRightEye()[0], face.getRightEye()[1], mPaintRightEye2);
                }
                if (face.getMouth() != null && face.getMouth().length == 2) {
                    canvas.drawPoint(face.getMouth()[0], face.getMouth()[1], mPaintMouth2);
                }
            }
        }
        if (mPoints != null) {
            for (Point point : mPoints) {
                if (point != null) {
                    canvas.drawPoint(point.x, point.y, mPaintMouth);
                }
            }
        }
    }

    public static class Face {
        private RectF rectF;
        private float[] leftEye;
        private float[] rightEye;
        private float[] mouth;

        public RectF getRectF() {
            return rectF;
        }

        public void setRectF(RectF rectF) {
            this.rectF = rectF;
        }

        public float[] getLeftEye() {
            return leftEye;
        }

        public void setLeftEye(float[] leftEye) {
            this.leftEye = leftEye;
        }

        public float[] getRightEye() {
            return rightEye;
        }

        public void setRightEye(float[] rightEye) {
            this.rightEye = rightEye;
        }

        public float[] getMouth() {
            return mouth;
        }

        public void setMouth(float[] mouth) {
            this.mouth = mouth;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mTouchCallBack != null) {
                mTouchCallBack.onTouch((int) event.getX(), (int) event.getY(), getMeasuredWidth(), getMeasuredHeight());
            }
        }
        return super.onTouchEvent(event);
    }

    public static interface TouchCallBack {
        void onTouch(int x, int y, int width, int height);
    }
}
