package com.hynson.host;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.argusapm.android.api.Client;
import com.argusapm.android.core.Config;
import com.argusapm.android.network.cloudrule.RuleSyncRequest;
import com.argusapm.android.network.upload.CollectDataSyncUpload;
import com.hynson.host.utils.HookManager;
import com.hynson.host.utils.LoadUtil;

import me.weishu.reflection.Reflection;

public class HostApp extends Application {
    final static String TAG = HostApp.class.getSimpleName();
    private Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
        mContext = base;
        Config.ConfigBuilder builder = new Config.ConfigBuilder()
                .setAppContext(this)
                .setAppName("测试打印机")
                .setRuleRequest(new RuleSyncRequest())
                .setUpload(new CollectDataSyncUpload())
                .setAppVersion("0.0.1")
                .setApmid("djlif7g2u892");
        Client.attach(builder.build());
        Client.startWork();
        try {
            HookManager.hookAMS();
            HookManager.getInstance(base).hookHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "attachBaseContext: ");
                try {
                    // 通过Hook IActivityManager实现Activity插件化
                    //LoadUtil.loadClass(base);
                    //HookManager.hookInstrumentation(base);
                    //HookHelper.hookAMS();
                    //HookHelper.hookHandler();
                    // 通过Hook Instrumentation实现Activity插件化
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        try {
//            HookHelper.hookInstrumentation(base);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            LoadUtil.loadClass(mContext);
//            // 通过Hook IActivityManager实现Activity插件化
////                    HookHelper.hookAMS();
////                    HookHelper.hookHandler();
//            // 通过Hook Instrumentation实现Activity插件化
////                    HookHelper.hookInstrumentation(mContext);
//            HookHelper.hookInstrumentation(mContext);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
