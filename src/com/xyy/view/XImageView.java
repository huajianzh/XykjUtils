package com.xyy.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class XImageView extends ImageView {
    private static final int MODE_DRAG = 1; //拖动
    private static final int MODE_ZOOM = 2; //缩放或者旋转
    private int mode;

    //记录拖动的上一个点
    private Point lastPoint;
    //缩放、旋转中心点
    private Point center;
    //缩放的上一个参考距离
    private float lastDis;
    //旋转弧度上的上一个点
    private Point lastArcPoint;
    //原始的状态，恢复图片效果时使用
    private Matrix originalMatrix;
    private RectF originalRect;

    public XImageView(Context context) {
        super(context);
        init();
    }

    public XImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        lastPoint = new Point();
        center = new Point();
        lastArcPoint = new Point();
    }

    //上一次触控点离开的时间(处理双击时使用)
    private long lastUpTime;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //单个触控点按下
                //记录当前的状态
                lastPoint.set((int) event.getX(), (int) event.getY());
                mode = MODE_DRAG;
                if (null == originalMatrix) {
                    originalMatrix = new Matrix();
                    originalMatrix.set(getImageMatrix());
                    originalRect = new RectF(getImageInfo());
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //记录缩放中心点
                lastDis = getDistance(event);
                if (lastDis > 10) {
                    mode = MODE_ZOOM;
                    //记录第一个旋转弧度上的点
                    lastArcPoint.set((int) event.getX(1), (int) event.getY(1));
                }
                if (null == originalMatrix) {
                    originalMatrix = new Matrix();
                    originalMatrix.set(getImageMatrix());
                    originalRect = new RectF(getImageInfo());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //触控在屏幕上的过程
                switch (mode) {
                    case MODE_DRAG:
                        //得到当前的点
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        int x1 = x - lastPoint.x;
                        int y1 = y - lastPoint.y;
                        //计算偏移
                        getImageMatrix().postTranslate(x1, y1);
                        //记录当前点为下一个移动的参考点
                        lastPoint.set(x, y);
                        invalidate();
                        break;
                    case MODE_ZOOM:
                        getCenter(event);
                        //当前旋转的新点(弧线上的)
                        float nX = event.getX(1);
                        float nY = event.getY(1);
                        float newDis = getDistance(event);
                        if (Math.abs(newDis - lastDis) < 15) {
                            //旋转
                            //获取起始夹角
                            float d1 = getDegress(lastArcPoint.x, lastArcPoint.y, getRotateRadius(lastArcPoint.x, lastArcPoint.y));
                            //获取当前的新夹角
                            float d2 = getDegress(nX, nY, getRotateRadius(nX, nY));
                            //计算旋转角度以及旋转画面
                            getImageMatrix().postRotate(d2 - d1, center.x, center.y);
                        } else {
                            //处理缩放
                            float sc = newDis / lastDis; //计算缩放比例
                            getImageMatrix().postScale(sc, sc, center.x, center.y);
                        }
                        invalidate();
                        lastDis = newDis;
                        lastArcPoint.set((int) nX, (int) nY);
                        break;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = MODE_DRAG;
                break;
            case MotionEvent.ACTION_UP:
                //单个触控点离开
                mode = 0;
                long t = System.currentTimeMillis();
                //如果两次离开的时间比较短则表示双击
                if (t - lastUpTime <= 300) {
                    //双击
                    //获取当前的图片状态
                    getImageInfo();
                    //如果当前状态的宽度比原宽度大则恢复，否则放大2倍
                    if (imageRect.width() > originalRect.width()) {
                        setImageMatrix(originalMatrix);
                    } else {
                        float[] values = new float[9];
                        getImageMatrix().getValues(values);
                        float px = event.getX();
                        float py = event.getY();
                        //放大2倍
                        getImageMatrix().postScale(values[0] * 2, values[4] * 2, px, py);
                    }
                    invalidate();
                }
                lastUpTime = t;
                break;
        }
        return true;
    }

    public void resetImage() {
        if (null != originalMatrix) {
            setImageMatrix(originalMatrix);
            originalMatrix = null;
        }
    }

    /**
     * 根据旋转弧度上的点获取半径
     *
     * @param x
     * @param y
     * @return
     */
    private float getRotateRadius(float x, float y) {
        //x方向的平方
        double x2 = Math.pow(x - center.x, 2);
        //y方向平方
        double y2 = Math.pow(y - center.y, 2);
        //计算平方和开根号
        return (float) Math.sqrt(x2 + y2);
    }

    /**
     * 根据弧线上的点
     *
     * @param x
     * @param y
     * @param r
     * @return
     */
    private float getDegress(float x, float y, float r) {
        //计算圆顶部切线夹角的tan值
        float tanValue = (y - center.y + r) / Math.abs(x - center.x);
        //反tan值(0-2PI)
        float dPi = (float) Math.atan(tanValue);
        //计算角度
        float d = (float) Math.toDegrees(dPi);
        float degress = d * 2;
        if (x < center.x) {
            degress = 360 - degress;
        }
        return degress;
    }

    /**
     * 获取两个触控点之间的距离
     *
     * @param event
     * @return
     */
    private float getDistance(MotionEvent event) {
        //x方向的平方
        double x2 = Math.pow(event.getX(0) - event.getX(1), 2);
        //y方向平方
        double y2 = Math.pow(event.getY(0) - event.getY(1), 2);
        //计算平方和开根号
        return (float) Math.sqrt(x2 + y2);
    }

    /**
     * 获取缩放中心点
     *
     * @param event
     */
    private void getCenter(MotionEvent event) {
        int x = (int) ((event.getX(0) + event.getX(1)) / 2);
        int y = (int) ((event.getY(0) + event.getY(1)) / 2);
        center.set(x, y);
    }

    private RectF imageRect;

    public RectF getImageInfo() {
        Rect rect = getDrawable().getBounds();
        float[] values = new float[9];
        getImageMatrix().getValues(values);
        float scaleWidth = rect.width() * values[0];
        float scaleHeight = rect.height() * values[4];
        float left = values[2];
        float top = values[5];
        float right = left + scaleWidth;
        float bottom = top + scaleHeight;
        if (null == imageRect) {
            imageRect = new RectF(left, top, right, bottom);
        } else {
            imageRect.set(left, top, right, bottom);
        }
        return imageRect;
    }
}
