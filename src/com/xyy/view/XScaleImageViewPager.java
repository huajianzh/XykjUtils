package com.xyy.view;

import android.content.Context;
import android.graphics.RectF;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Administrator on 2017/11/21.
 */
public class XScaleImageViewPager extends ViewPager {
    public XScaleImageViewPager(Context context) {
        super(context);
        init();
    }

    public XScaleImageViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setCurrentItem(int item) {
        currentIndex = item;
        super.setCurrentItem(item);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        currentIndex = item;
        super.setCurrentItem(item, smoothScroll);
    }

    int currentIndex;

    private void init() {
        addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (currentIndex != position) {
                    XImageView imageView = (XImageView) findViewWithTag(currentIndex);
                    imageView.resetImage();
                    currentIndex = position;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private int lastX, lastY;
    private RectF imageRect;
    XImageView imageView;
    private static final int MODE_DRAG = 0;
    private static final int MODE_ZOOM = 1;
    private int mode;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                //该方法要结合适配器中itemView.setTag(position);一起使用才有效
                imageView = (XImageView) findViewWithTag(getCurrentItem());
                mode = MODE_DRAG;
//                imageRect = imageView.getImageInfo();
//                if (imageRect.left >= 0 && imageRect.right <= imageView.getMeasuredWidth()
//                        && imageRect.top >= 0 && imageRect.bottom <= imageView.getMeasuredHeight()) {
//                    //ViewPager自己消耗
////                    return super.onInterceptTouchEvent(event);
//                } else {
//                    return false;
//                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = MODE_ZOOM;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (mode == MODE_DRAG) {
                    int cX = (int) event.getX();
                    int cY = (int) event.getY();
                    int x1 = cX - lastX;
                    int y1 = cY - lastY;
                    imageRect = imageView.getImageInfo();
                    lastX = cX;
                    lastY = cY;
                    if (Math.abs(x1) > Math.abs(y1)) {
                        //左右滑动
                        if (x1 < 0) {
                            //右往左-->检测图像右边是否已经在视图内，是交由父容器处理
                            if (imageRect.right <= imageView.getMeasuredWidth()) {
//                            return super.onInterceptTouchEvent(event);
                                return true;
                            } else {
                                //阻止ViewPager拦截事件(XImageView消耗该事件)
                                return false;
                            }
                        } else {
                            //左往右
                            if (imageRect.left >= 0) {
//                            return super.onInterceptTouchEvent(event);
                                return true;
                            } else {
                                //阻止ViewPager拦截事件(XImageView消耗该事件)
                                return false;
                            }
                        }
                    } else if (Math.abs(x1) == Math.abs(y1)) {
                        return false;
                    } else {
                        //上下滑动
                        if (y1 < 0) {
                            // 下往上
                            if (imageRect.bottom <= imageView.getMeasuredHeight()) {
//                            return super.onInterceptTouchEvent(event);
                                return true;
                            } else {
                                //阻止ViewPager拦截事件(XImageView消耗该事件)
                                return false;
                            }
                        } else {
                            //上往下
                            if (imageRect.top >= 0) {
//                            return super.onInterceptTouchEvent(event);
                                return true;
                            } else {
                                //阻止ViewPager拦截事件(XImageView消耗该事件)
                                return false;
                            }
                        }
                    }
                }else{
                    return false;
                }
            case MotionEvent.ACTION_POINTER_UP:
                mode = MODE_DRAG;
                break;
        }
        return super.onInterceptTouchEvent(event);
    }
}
