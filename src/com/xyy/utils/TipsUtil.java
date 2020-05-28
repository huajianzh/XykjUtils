package com.xyy.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by admin on 2016/10/11.
 */
public class TipsUtil {
    private static final boolean DEBUG = true;
private static Toast toast = null;
    private static WeakReference<Context> contextRef;

    public static void toast(Context context, int res){
        toast(context,res,Toast.LENGTH_SHORT);
    }
    public static void toast(Context context, int res,int duration) {
        toast(context, context.getResources().getString(res),duration);
    }

    public static void toast(Context context, String msg) {
        toast(context, msg, Toast.LENGTH_SHORT);
    }

    public static void toast(Context context, String msg, int duration) {
        //防止多次显示Toast是频繁弹出提示框
        if (null == toast) {
            if (null == contextRef || contextRef.get() == null) {
                contextRef = new WeakReference<>(context.getApplicationContext());
            }
            //使用ApplicationContext防止内存泄漏
            toast = Toast.makeText(contextRef.get(), msg, duration);
        } else {
            toast.setDuration(duration);
            toast.setText(msg);
        }
        toast.show();
    }


    public static void log(String msg) {
        log("m_tag", msg);
    }

    public static void log(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }
    
    //根据类型参数创建(泛型)对应对象，如在class A<B>，即类A中创建B对象
    public static <T> T createObj(Object obj, int i) {
        // class ClassName<T,K,V>
        Type superType = obj.getClass().getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) superType;
            //遍历尖括号中的类型
            Type[] ts = pType.getActualTypeArguments();
            Class<T> cls = (Class<T>) ts[i];
            try {
                return cls.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
   
}
