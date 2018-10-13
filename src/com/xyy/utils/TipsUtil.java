package com.xyy.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by admin on 2016/10/11.
 */
public class TipsUtil {
    private static final boolean DEBUG = true;


    public static void showToast(Context context, String msg) {
        showToast(context, msg, Toast.LENGTH_SHORT);
    }

    public static void showToast(Context context, String msg, int time) {
        Toast.makeText(context, msg, time).show();
    }


    public static void log(String msg) {
        log("m_tag", msg);
    }

    public static void log(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }
    
    //�������Ͳ�������(����)��Ӧ��������class A<B>������A�д���B����
    public static <T> T createObj(Object obj, int i) {
        // class ClassName<T,K,V>
        Type superType = obj.getClass().getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) superType;
            //�����������е�����
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
