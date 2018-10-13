package com.xyy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by admin on 2016/10/18.
 */
public class XScrollView extends ScrollView {
    public XScrollView(Context context) {
        super(context);
    }

    public XScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public XScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if(null != onSrollChangeListener){
            onSrollChangeListener.onSrollChanged(l,t,oldl,oldt);
        }
    }

    public interface OnSrollChangedListener{
        void onSrollChanged(int l, int t, int oldl, int oldt);
    }

    private OnSrollChangedListener onSrollChangeListener;

    public OnSrollChangedListener getOnSrollChangedListener() {
        return onSrollChangeListener;
    }

    public void setOnSrollChangedListener(OnSrollChangedListener onSrollChangeListener) {
        this.onSrollChangeListener = onSrollChangeListener;
    }
}
