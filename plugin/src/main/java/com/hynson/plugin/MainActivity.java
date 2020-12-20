package com.hynson.plugin;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends BaseActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        Log.i(TAG, "onCreate: "+R.layout.activity_plugin);
        Log.i("TAG", "插件的onCreate: ");
    }
}
