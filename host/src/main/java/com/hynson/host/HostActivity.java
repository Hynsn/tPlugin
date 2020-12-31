package com.hynson.host;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hynson.host.utils.HookManager;

import java.lang.reflect.Method;

public class HostActivity extends Activity implements View.OnClickListener{
    final static String TAG = HostActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: "+R.layout.activity_main);

        findViewById(R.id.tv_start_plugin_activity).setOnClickListener(this);
        findViewById(R.id.btn_loadplugin).setOnClickListener(this);
    }
    private void testPlugin(){
        try {
            Class<?> clazz = Class.forName("com.hynson.plugin.Test");
            Log.i(TAG, "testPlugin: "+clazz);
            Method test = clazz.getMethod("print");
            test.invoke(null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_loadplugin:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HookManager.getInstance(getApplication()).customLoadApkAction();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.tv_start_plugin_activity:
                Intent intent = new Intent();
                //MainActivity.this, PluginActivity.class
                //intent.setComponent(new ComponentName("com.yuong.plugin", "com.yuong.plugin.PluginActivity"));
                intent.setComponent(new ComponentName("com.hynson.plugin", "com.hynson.plugin.MainActivity"));
//                intent.setComponent(new ComponentName("com.hynson.test", "com.hynson.test.MainActivity"));
                startActivity(intent);
                break;
        }
    }
}
