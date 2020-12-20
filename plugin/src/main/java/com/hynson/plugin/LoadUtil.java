package com.hynson.plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.lang.reflect.Method;

public class LoadUtil {
    private static final String pluginApkPath = "/sdcard/plugin-debug.apk";

    private static Resources mResources;
    public static Resources getResources(Context context){
        if(mResources==null) mResources = loadResources(context);
        return mResources;
    }

    private static Resources loadResources(Context context){
        try {
            // 1.创建AssetManager
            AssetManager assetManager = AssetManager.class.newInstance();
            // 2.添加插件的资源
            Method addAssetPathMethod = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPathMethod.invoke(assetManager,pluginApkPath);
            // 3.创建resources并传入AssetManager
            Resources res = context.getResources();
            return new Resources(assetManager,res.getDisplayMetrics(),res.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
