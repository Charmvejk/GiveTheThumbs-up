package com.example.expandtextview.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import com.example.expandtextview.app.App;

/**
 * @作者: njb
 * @时间: 2019/8/12 18:12
 * @描述:
 */
public class Utils {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5F);
    }


    public static int dp2px(float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, App.getContext().getResources().getDisplayMetrics());
    }


    /**
     * 初始化工具类
     *
     * @param context 上下文
     */
    public static void init(@NonNull final Context context) {
        Utils.context = context.getApplicationContext();
    }

    /**
     * 获取ApplicationContext
     *
     * @return ApplicationContext
     */
    public static Context getContext() {
        if (Utils.context != null) return Utils.context;
        throw new NullPointerException("u should init first");
    }

    /**
     * 获取状态栏高度
     * @return
     */
    public static int getStatusBarHeight() {
        int result = 0;
        int resourceId = App.getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result =App.getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
