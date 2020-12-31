package com.hynson.plugin;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

public class MainActivity extends BaseActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: "+R.layout.activity_main);
        Log.i("TAG", "插件的onCreate: ");
    }
}
