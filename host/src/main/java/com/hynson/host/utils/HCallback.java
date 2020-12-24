package com.hynson.host.utils;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class HCallback implements Handler.Callback {

    private static final int LAUNCH_ACTIVITY = 100;

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == LAUNCH_ACTIVITY) {
            Object obj = msg.obj;
            try {
                // 获取启动SubActivity的Intent
                Intent stubIntent = (Intent) ReflectUtil.get(obj, "intent", false);

                // 获取启动PluginActivity的Intent(之前保存在启动SubActivity的Intent之中)
                Intent pluginIntent = stubIntent.getParcelableExtra(HookHelper.PLUGIN_INTENT);

                Log.i("TAG", "handleMessage: "+(pluginIntent==null)+","+(stubIntent==null));
                if(pluginIntent!=null) {
                    Log.i("TAG", "handleMessage:1 ");
                    // 将启动SubActivity的Intent替换为启动PluginActivity的Intent
                    stubIntent.setComponent(pluginIntent.getComponent());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
