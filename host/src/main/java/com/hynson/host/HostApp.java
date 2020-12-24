package com.hynson.host;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.hynson.host.utils.HookHelper;
import com.hynson.host.utils.LoadUtil;

public class HostApp extends Application {
    final static String TAG = HostApp.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        LoadUtil.loadClass(this);
        Log.i(TAG, "attachBaseContext: ");
        try {
            // 通过Hook IActivityManager实现Activity插件化
            HookHelper.hookAMS();
            HookHelper.hookHandler();

            // 通过Hook Instrumentation实现Activity插件化
            //HookHelper.hookInstrumentation(base);
            //HookHelper.hookInstrumentation1(base);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
