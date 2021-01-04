package com.hynson.plugin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import java.util.zip.Inflater;

public class MainActivity extends FragmentActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        Log.i(TAG, "onCreate: "+R.layout.activity_main);
        Log.i("TAG", "插件的onCreate: ");
    }
}
