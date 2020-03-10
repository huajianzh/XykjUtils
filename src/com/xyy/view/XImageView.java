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

public class XImageView extends android.support.v7.widget.AppCompatImageView {
    //单击的超时时间
    private static final int TAP_TIMEOUT = 150;
    //双击时间
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int TAP = 3;
    private static final int TAP_CANCEL = 4;
    private static final int MSG_GIF_INIT_START = 5;
    private static final int MSG_GIF_INIT_OK = 6;
    private static final int MSG_GIF_UPDATE = 7;
    //无操作（普通模式）
    public static final int MODE_NONE = -1;
    //可以进行拖动、缩放的模式
    public static final int MODE_ENABLE_ZOOM = 0;
    private static final int MODE_DRAG = 1; //拖动
    private static final int MODE_ZOOM = 2; //缩放或者旋转
    private int mode = MODE_ENABLE_ZOOM;

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
    //变形中的Matrix
    private Matrix mMatrix;
    private RectF originalRect;
    private float originalScale = 1;
    private float SCALE_MAX = 5;
    //    //播放gif的对象
//    private Movie mMovie;
//    private GifDecoder gifDecoder;
    private boolean isGif;
    private GifDecoder.GifFrame[] gifFrames;
    //当前显示的gif下标
    private int currentIndex;
    private Handler mHandler;

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
        mHandler = new MHandler(this);
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        releaseGif();
        super.setImageURI(uri);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        releaseGif();
        super.setImageBitmap(bm);
    }

    @Override
    public void setImageResource(int resId) {
        releaseGif();
        super.setImageResource(resId);
    }

    //    private Point movieStartPoint;
    private ExecutorService mExecutor;
    private boolean isLoadingGif;
    private Future<?> initFuture;

    public void loadGifPath(final String filePath) {
        if (null == mExecutor) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        if (null != initFuture && !initFuture.isCancelled() && !initFuture.isDone()) {
            Log.e("m_tag", "loadGif cancel current:" + filePath);
            initFuture.cancel(true);
        }
        isGif = true;
        initFuture = mExecutor.submit(new LoadGifRunnable(filePath));
    }

    private class LoadGifRunnable implements Runnable {
        private String path;

        private LoadGifRunnable(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            mHandler.sendEmptyMessage(MSG_GIF_INIT_START);
            try {
                Thread.sleep(0);
                GifDecoder gifDecoder = new GifDecoder();
                int state = gifDecoder.read(new FileInputStream(path));
                if (state == GifDecoder.STATUS_OK) {
                    GifDecoder.GifFrame[] frames = gifDecoder.getFrames();
                    mHandler.obtainMessage(MSG_GIF_INIT_OK, gifDecoder.getWidth(), gifDecoder.getHeight(), frames).sendToTarget();
                }
            } catch (Exception e) {
                TipsUtil.log("load gif in XImageView -- interrupt : " + path + " " + e.getMessage());
            }
        }
    }

    //    private float scale = 1;
    private int viewWidth, viewHeight;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        if (null != gifFrames && gifFrames.length > 0) {
//            if (null == movieStartPoint) {
//                movieStartPoint = new Point();
//            }
//            int w = gifSize.x;
//            int h = gifSize.y;
//            float scaleW = (float) getMeasuredWidth() / w;
//            float scaleH = (float) getMeasuredHeight() / h;
//            scale = Math.min(scaleW, scaleH);
//            //计算出放大后对应的起始位置
//            int x = (int) ((getMeasuredWidth() - scale * w) / 2);
//            int y = (int) ((getMeasuredHeight() - scale * h) / 2);
//            //基于放大后的位置折算出在放大的画布上的绝对起始位置
//            movieStartPoint.set((int) (x / scale), (int) (y / scale));
//            setMeasuredDimension(w, h);
//        }
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (null != originalMatrix) {
            originalMatrix = null;
        }
    }

    private void releaseGif() {
        if (isGif) {
            isGif = false;
            mHandler.removeMessages(MSG_GIF_UPDATE);
            if (null != gifFrames) {
                int len = gifFrames.length;
                for (int i = 0; i < len; i++) {
                    Bitmap b = gifFrames[i].image;
                    if (null != b && !b.isRecycled()) {
                        b.recycle();
                    }
                }
                gifFrames = null;
            }
            currentIndex = 0;
        }
    }

    public void release() {
        if (null != gifFrames && gifFrames.length > 0) {
            for (int i = 0; i < gifFrames.length; i++) {
                if (null != gifFrames[i].image && !gifFrames[i].image.isRecycled()) {
                    gifFrames[i].image.recycle();
                }
            }
            gifFrames = null;
        }
    }

//    private long movieStartTime;

    @Override
    protected void onDraw(Canvas canvas) {
        if (isLoadingGif) {
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            p.setTextSize(32);
            String s = "gif图像加载中";
            Rect r = new Rect();
            p.getTextBounds(s, 0, s.length(), r);
            int cx = (getWidth() - r.width()) >> 1;
            int cy = (getHeight() - r.height()) >> 1;
            canvas.drawText(s, cx, cy, p);
            return;
        }
        super.onDraw(canvas);
    }


    //上一次触控点离开的时间(处理双击时使用)
    private long lastUpTime;
    //缩放变换中的数据
    float[] matrixValues = new float[9];
    private boolean isChanged;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null || mode == MODE_NONE){
            return super.onTouchEvent(event);
        }
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mHandler.removeMessages(TAP);
                mHandler.removeMessages(TAP_CANCEL);
                //单个触控点按下
                //记录当前的状态
                lastPoint.set((int) event.getX(), (int) event.getY());
                mode = MODE_DRAG;
                initOriginal();
                mMatrix = getImageMatrix();
                isChanged = false;
                mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
                mHandler.sendEmptyMessageDelayed(TAP_CANCEL, TAP_TIMEOUT);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mHandler.removeMessages(TAP);
                mHandler.removeMessages(TAP_CANCEL);
                //记录缩放中心点
                lastDis = getDistance(event);
                if (lastDis >= 10) {
                    mode = MODE_ZOOM;
                    //记录第一个旋转弧度上的点
                    lastArcPoint.set((int) event.getX(1), (int) event.getY(1));
                }
                initOriginal();
                mMatrix = getImageMatrix();
                isChanged = true;
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
                        getImageInfo(mMatrix);
                        if (x1 > 0) {
                            if (imageRect.left >= 0) {
                                x1 = 0;
                            } else if (imageRect.left + x1 > 0) {
                                x1 = (int) (imageRect.left * (-1));
                            }
                        } else if (x1 < 0) {
                            if (imageRect.right <= viewWidth) {
                                x1 = 0;
                            } else if (imageRect.right + x1 < viewWidth) {
                                x1 = (int) (viewWidth - imageRect.right);
                            }
                        }
                        if (y1 > 0) {
                            if (imageRect.top >= 0) {
                                y1 = 0;
                            } else if (imageRect.top + y1 > 0) {
                                y1 = (int) (imageRect.top * (-1));
                            }
                        } else if (y1 < 0) {
                            if (imageRect.bottom <= viewHeight) {
                                y1 = 0;
                            } else if (imageRect.bottom + y1 < viewHeight) {
                                y1 = (int) (viewHeight - imageRect.bottom);
                            }
                        }
                        isChanged = (x1 != 0 || y1 != 0);
                        if (isChanged) {
                            //计算偏移
                            mMatrix.postTranslate(x1, y1);
                            updateImageView(mMatrix, "drag");
                        }
                        //记录当前点为下一个移动的参考点
                        lastPoint.set(x, y);
                        if (isChanged) {
                            mHandler.removeMessages(TAP);
                            mHandler.removeMessages(TAP_CANCEL);
                        }
                        break;
                    case MODE_ZOOM:
                        getCenter(event);
                        //当前旋转的新点(弧线上的)
                        float nX = event.getX(1);
                        float nY = event.getY(1);
                        float newDis = getDistance(event);
//                        if (Math.abs(newDis - lastDis) < 15) {
//                        //旋转
//                        //获取起始夹角
//                        float d1 = getDegress(lastArcPoint.x, lastArcPoint.y, getRotateRadius(lastArcPoint.x, lastArcPoint.y));
//                        //获取当前的新夹角
//                        float d2 = getDegress(nX, nY, getRotateRadius(nX, nY));
//                        //计算旋转角度以及旋转画面
//                        getImageMatrix().postRotate(d2 - d1, center.x, center.y);
//                        } else {
                        //处理缩放
                        float sc = newDis / lastDis; //计算缩放比例
                        mMatrix.getValues(matrixValues);
                        //最大5倍最小originalScale倍,大于5倍则可以收缩，小于0.3则可以放大
                        if (sc != 1 && ((matrixValues[0] <= SCALE_MAX && matrixValues[0] >= originalScale) || (matrixValues[0] > SCALE_MAX && sc < 1) || (matrixValues[0] < originalScale && sc > 1))) {
                            mMatrix.postScale(sc, sc, center.x, center.y);
                            updateImageView(mMatrix, "scale");
                        }
//                        }
                        lastDis = newDis;
                        lastArcPoint.set((int) nX, (int) nY);
                        break;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = MODE_ENABLE_ZOOM;
                break;
            case MotionEvent.ACTION_UP:
                //单个触控点离开
                mode = MODE_ENABLE_ZOOM;
                long t = System.currentTimeMillis();
                //如果两次离开的时间比较短则表示双击
                if (t - lastUpTime <= DOUBLE_TAP_TIMEOUT) {
                    mHandler.removeMessages(TAP);
                    mHandler.removeMessages(TAP_CANCEL);
                    //双击
                    //获取当前的图片状态
                    getImageInfo(mMatrix);
                    //如果当前状态的宽度比原宽度大则恢复，否则放大2倍
                    if (imageRect.width() > originalRect.width() || imageRect.width() < originalRect.width()) {
//                        setImageMatrix(originalMatrix);
                        updateImageView(originalMatrix, "double tap");
                    } else {
                        float[] values = new float[9];
                        mMatrix.getValues(values);
                        float px = event.getX();
                        float py = event.getY();
                        //放大2倍
                        mMatrix.postScale(values[0] * 2, values[4] * 2, px, py);
                        updateImageView(mMatrix, "double tap");
                    }
                } else {
                    mHandler.removeMessages(TAP_CANCEL);
                }
                lastUpTime = t;
                break;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP && isChanged) {
            mHandler.removeMessages(TAP);
            mHandler.removeMessages(TAP_CANCEL);
            mMatrix.getValues(matrixValues);
//            TipsUtil.log("==up or cancel==" + matrixValues[0]);
            if (matrixValues[0] <= originalScale) {
                resetImage();
                mMatrix = null;
            }
        }
        return true;
    }

    private void updateImageView(Matrix matrix, String tag) {
        try {
            Method m = getClass().getMethod("animateTransform", Matrix.class);
            m.setAccessible(true);
            m.invoke(this, matrix);
        } catch (Exception e) {
            getImageMatrix().set(matrix);
            invalidate();
        }
    }

    private static class MHandler extends Handler{
        private WeakReference<XImageView> v;
        private MHandler(XImageView view){
            v = new WeakReference<>(view);
        }
        @Override
        public void handleMessage(Message msg) {
            if(v.get() == null){
                removeCallbacksAndMessages(null);
                return;
            }
            switch (msg.what) {
                case TAP:
                    //响应点击监听
                    v.get().performClick();
                    break;
                case TAP_CANCEL:
                    removeMessages(TAP);
                    break;
                case MSG_GIF_INIT_START:
                    v.get().isLoadingGif = true;
                    v.get().invalidate();
                    break;
                case MSG_GIF_INIT_OK:
                    v.get().isLoadingGif = false;
                    v.get().gifFrames = (GifDecoder.GifFrame[]) msg.obj;
                    if (null != v.get().gifFrames && v.get().gifFrames.length > 0) {
                        v.get().requestLayout();
                        sendEmptyMessage(MSG_GIF_UPDATE);
                        v.get().currentIndex = 0;
                    }
                    break;
                case MSG_GIF_UPDATE:
                    v.get().updateGifFrame();
                    break;
            }
        }
    }

    private void updateGifFrame(){
        if(null == getDrawable()) {
            XImageView.super.setImageBitmap(gifFrames[currentIndex].image);
        }else{
            BitmapDrawable bd = (BitmapDrawable) getDrawable();
            try {
                Method m = bd.getClass().getMethod("setBitmap", Bitmap.class);
                m.setAccessible(true);
                m.invoke(bd, gifFrames[currentIndex].image);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if (gifFrames.length > 1) {
            mHandler.sendEmptyMessageDelayed(MSG_GIF_UPDATE, gifFrames[currentIndex].delay);
            if (currentIndex < gifFrames.length - 1) {
                currentIndex++;
            } else {
                currentIndex = 0;
            }
        }
    }

    private void initOriginal() {
        if (null == originalMatrix) {
            originalMatrix = new Matrix();
            originalMatrix.set(getImageMatrix());
            float[] v = new float[9];
            originalMatrix.getValues(v);
            originalScale = v[0];
            SCALE_MAX = originalScale * 5;
            originalRect = new RectF(getImageInfo());
        }
    }

    public void resetImage() {
        if (null != originalMatrix) {
//            setImageMatrix(originalMatrix);
            updateImageView(originalMatrix, "reset");
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
        return getImageInfo(getImageMatrix());
    }

    public RectF getImageInfo(Matrix matrix) {
        Rect rect = getDrawable().getBounds();
        float[] values = new float[9];
        matrix.getValues(values);
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

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        if (mode == MODE_NONE) {
            resetImage();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        TipsUtil.log("XImageView onDetachedFromWindow");
        cancelGifTask();
        releaseGif();
        super.onDetachedFromWindow();
    }

    public void cancelGifTask() {
        if (null != initFuture && !initFuture.isDone() && !initFuture.isCancelled()) {
            initFuture.cancel(true);
            initFuture = null;
        }
        if (null != mExecutor) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    public int getViewWidth() {
        return viewWidth;
    }

    public int getViewHeight() {
        return viewHeight;
    }

    public boolean isGif() {
        return isGif;
    }
}
