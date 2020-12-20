package com.hynson.host;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Method;

public class HostActivity extends Activity {
    final static String TAG = HostActivity.class.getSimpleName();

    private Button btnPlugin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: "+R.layout.activity_main);

        btnPlugin = (Button) findViewById(R.id.tv_start_plugin_activity);
        btnPlugin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                //MainActivity.this, PluginActivity.class
                intent.setComponent(new ComponentName("com.hynson.plugin","com.hynson.plugin.MainActivity"));
                startActivity(intent);

                //testPlugin();
            }
        });
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
}
