package com.xyy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.xyy.vwill.R;

/**
 * Created by Administrator on 2017/5/9.
 * 自定义开关按钮
 */

public class XToggleButton extends View {
    //画笔工具
    private Paint paint;
    //绘制的内容宽度
    private int width;
    //左侧圆心和右侧圆心
    private Point leftCenter;
    private Point rightCenter;
    //拖块的中心
    private Point thumbCenter;
    //左右圆的半径
    private int radius;
    //拖块的半径
    private int thumbRadius;
    //关闭时的背景色
    private int closeBgColor = 0xff666666;
    //打开时的背景色
    private int openBgColor = 0xff3F51B5;
    //开文本的颜色
    private int openTextColor = 0xFFFF4081;
    //关文本的颜色
    private int closeTextColor = 0xffffffff;
    //文本大小
    private int textSize;
    //打开时拖块的颜色
    private int openThumbColor = 0xffFF4081;
    //关闭时拖块的颜色
    private int closeThumbColor = 0xffffffff;
    //打开或者关闭的状态
    private boolean isOpen;
    //开状态下的文本
    private String openText = "开";
    //关状态下的文本
    private String closeText = "关";
    //是否是拖动模式
    private boolean isDragMode;


    public XToggleButton(Context context) {
        super(context);
        init();
    }

    public XToggleButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.XToggleButton);
        closeBgColor = ta.getColor(R.styleable.XToggleButton_closeBgColor, closeBgColor);
        openBgColor = ta.getColor(R.styleable.XToggleButton_openBgColor, openBgColor);
        openTextColor = ta.getColor(R.styleable.XToggleButton_openTextColor, openTextColor);
        closeTextColor = ta.getColor(R.styleable.XToggleButton_closeTextColor, closeTextColor);
        textSize = ta.getDimensionPixelSize(R.styleable.XToggleButton_textSize, 0);
        openThumbColor = ta.getColor(R.styleable.XToggleButton_openThumbColor, openThumbColor);
        closeThumbColor = ta.getColor(R.styleable.XToggleButton_closeThumbColor,
                closeThumbColor);
        isOpen = ta.getBoolean(R.styleable.XToggleButton_open, false);
        openText = ta.getString(R.styleable.XToggleButton_openText);
        if (null == openText) {
            openText = "开";
        }
        closeText = ta.getString(R.styleable.XToggleButton_closeText);
        if (closeText == null) {
            closeText = "关";
        }
        ta.recycle();
        init();
    }

    private void init() {
        leftCenter = new Point();
        rightCenter = new Point();
        thumbCenter = new Point();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);
    }

    //中间的矩形
    private Rect centerRect;
    //文本区域
    private Rect bound;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制背景
        int bgColor = isOpen ? openBgColor : closeBgColor;
        paint.setColor(bgColor);
        canvas.drawCircle(leftCenter.x, leftCenter.y, radius, paint);
        canvas.drawCircle(rightCenter.x, rightCenter.y, radius, paint);
        if (null == centerRect) {
            centerRect = new Rect(leftCenter.x, leftCenter.y - radius, width - radius, leftCenter
                    .y +
                    radius);
        }
        canvas.drawRect(centerRect, paint);
        //拖动模式下绘制拖动中的背景
        if (isDragMode) {
            Rect r;
            if (isOpen) {
                //说明当前要拖动去关闭
                paint.setColor(closeBgColor);
                canvas.drawCircle(rightCenter.x, rightCenter.y, thumbRadius, paint);
                //从右侧圆画到拖块位置
                r = new Rect(thumbCenter.x, thumbCenter.y - thumbRadius, rightCenter.x, thumbCenter
                        .y + thumbRadius);
            } else {
                //要拖动去打开
                paint.setColor(openBgColor);
                canvas.drawCircle(leftCenter.x, leftCenter.y, thumbRadius, paint);
                //从左侧圆画到拖块位置
                r = new Rect(leftCenter.x, leftCenter.y - thumbRadius, thumbCenter.x, leftCenter
                        .y + thumbRadius);
            }
            canvas.drawRect(r, paint);
        }
        String s = getText();
        //计算文本位置
        int tX, tY;
        if (thumbCenter.x > width / 2) {
            tX = leftCenter.x - bound.centerX();
            tY = leftCenter.y - bound.centerY();
        } else {
            tX = rightCenter.x - bound.centerX();
            tY = rightCenter.y - bound.centerY();
        }
        canvas.drawText(s, tX, tY, paint);
        //绘制拖块
        int thumbColor = isOpen ? openThumbColor : closeThumbColor;
        paint.setColor(thumbColor);
        canvas.drawCircle(thumbCenter.x, thumbCenter.y, thumbRadius, paint);
    }

    private String getText() {
        //拖块在右半边则画“开”否则画“关”
        String str = thumbCenter.x > width / 2 ? openText : closeText;
        if (bound == null) {
            //画提示文本
            paint.setTextSize(textSize);
            bound = new Rect();
            paint.getTextBounds(str, 0, str.length(), bound);
        }
        int color = thumbCenter.x > width / 2 ? openTextColor : closeTextColor;
        paint.setColor(color);
        return str;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = getSize(widthMeasureSpec, 120);
        int h = getSize(heightMeasureSpec, 80);
        //绘制的圆的高度（内容高度）
        int height = h;
        if (h > width / 2) {
            height = width / 2;
        }
        radius = height / 2; //将半径设置为内容高度的一半
        //如果字体大小未设置则取圆半径作为字体大小
        if (textSize == 0) {
            textSize = radius;
        }
        thumbRadius = radius - 2;
        leftCenter.set(radius, radius);
        rightCenter.set(width - radius, radius);
        //关闭时拖块在左侧，打开时拖块在右侧
        if (!isOpen) {
            thumbCenter.set(leftCenter.x, leftCenter.y);
        } else {
            thumbCenter.set(rightCenter.x, rightCenter.y);
        }
        setMeasuredDimension(width, h);
    }

    //获取尺寸方法（自定义的根据模式获取，wrap_content时设置默认值为80）
    private int getSize(int measureSpec, int defSize) {
        //获取测量模式
        int mode = MeasureSpec.getMode(measureSpec);
        int size = defSize;
        if (mode == MeasureSpec.EXACTLY) {
            //精确的，固定的值，match_parent或者100  200
            size = MeasureSpec.getSize(measureSpec);
        }
        return size;
    }

    //拖动的起始x坐标
    private int startX;
    //记录按下时间，如果按下到离开的时间很短，则当做点击
    private long startTime;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startTime = System.currentTimeMillis();
                isDragMode = true;
                startX = (int) event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragMode) {
                    int x = (int) event.getX();
                    //计算偏移
                    int x1 = x - startX;
                    //计算拖动后拖块的新位置
                    int nX = thumbCenter.x + x1;
                    if (nX < leftCenter.x) { //不允许拖动超过左边圆心以及右边圆心
                        nX = leftCenter.x;
                    } else if (nX > rightCenter.x) {
                        nX = rightCenter.x;
                    }
                    //处理拖块中心的偏移
                    thumbCenter.x = nX;
                    startX = x;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                long current = System.currentTimeMillis();
                //拖动状态下是否拖过一半，不过则恢复，过了则改变开关状态
                //点击状态则直接处理开或者关
                if (isOpen) {
                    if (current - startTime <= 150) {
                        //处理点击
                        close();
                    } else if (isDragMode) {
                        if (thumbCenter.x < width / 2) {
                            close();
                        } else {
                            thumbCenter.x = rightCenter.x;
                        }
                    }
                } else {
                    if (current - startTime <= 150) {
                        open();
                    } else if (isDragMode) {
                        //是否从左拖到又半边
                        if (thumbCenter.x > width / 2) {
                            open();
                        } else {
                            thumbCenter.x = leftCenter.x;
                        }
                    }
                }
                isDragMode = false;
                invalidate();
                break;
        }
        return true;
    }

    private void close() {
        thumbCenter.x = leftCenter.x;
        isOpen = false;
        if (null != onSwitchListener) {
            onSwitchListener.onSwitch(this, false);
        }
    }

    private void open() {
        thumbCenter.x = rightCenter.x;
        isOpen = true;
        if (null != onSwitchListener) {
            onSwitchListener.onSwitch(this, true);
        }
    }

    public int getCloseBgColor() {
        return closeBgColor;
    }

    public void setCloseBgColor(int closeBgColor) {
        this.closeBgColor = closeBgColor;
    }

    public int getOpenBgColor() {
        return openBgColor;
    }

    public void setOpenBgColor(int openBgColor) {
        this.openBgColor = openBgColor;
    }

    public int getOpenTextColor() {
        return openTextColor;
    }

    public void setOpenTextColor(int openTextColor) {
        this.openTextColor = openTextColor;
    }

    public int getCloseTextColor() {
        return closeTextColor;
    }

    public void setCloseTextColor(int closeTextColor) {
        this.closeTextColor = closeTextColor;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getOpenThumbColor() {
        return openThumbColor;
    }

    public void setOpenThumbColor(int openThumbColor) {
        this.openThumbColor = openThumbColor;
    }

    public int getCloseThumbColor() {
        return closeThumbColor;
    }

    public void setCloseThumbColor(int closeThumbColor) {
        this.closeThumbColor = closeThumbColor;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        if (isOpen != open) {
            if (open) {
                open();
            } else {
                close();
            }
            invalidate();
        }
    }

    public String getOpenText() {
        return openText;
    }

    public void setOpenText(String openText) {
        this.openText = openText;
    }

    public String getCloseText() {
        return closeText;
    }

    public void setCloseText(String closeText) {
        this.closeText = closeText;
    }

    /**
     * 开关变化监听器
     */
    public interface OnSwitchListener {
        /**
         * 开关状态变化
         *
         * @param view   发生变化的按钮对象
         * @param isOpen 当前状态
         */
        void onSwitch(XToggleButton view, boolean isOpen);
    }

    public OnSwitchListener getOnSwitchListener() {
        return onSwitchListener;
    }

    public void setOnSwitchListener(OnSwitchListener onSwitchListener) {
        this.onSwitchListener = onSwitchListener;
    }

    private OnSwitchListener onSwitchListener;
}

