package com.hynson.host.utils;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

public class LoadUtil {
    private static final String TAG = LoadUtil.class.getSimpleName();
    private static final String pluginApkPath = "/sdcard/plugin-debug.apk";

    public static void loadClass(Context context){
        try {
            /*
            Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
            Field dexElementsField = dexPathListClass.getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
*/
            Field dexElementsField = ReflectUtil.getField("dalvik.system.DexPathList","dexElements",false);
            /*
            Class<?> classLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = classLoaderClass.getDeclaredField("pathList");
            pathListField.setAccessible(true);
*/
            Field pathListField = ReflectUtil.getField("dalvik.system.BaseDexClassLoader","pathList",false);
            // 1. 获取宿主的
            ClassLoader pathClassLoader = context.getClassLoader();
            Object hostPathList = pathListField.get(pathClassLoader);
            // 获取dexElements对象
            Object[] hostDexElements = (Object[]) dexElementsField.get(hostPathList);

            // 2.获取插件的
            // context.getCacheDir().getAbsolutePath()，
            // parent和版本有关 需适配，7.0之前传pathClassLoader，7.0可以传null
            //
            ClassLoader pluginClassLoader = new DexClassLoader(pluginApkPath,
                    context.getCacheDir().getAbsolutePath(),null,pathClassLoader);
            Object pluginPathList = pathListField.get(pluginClassLoader);
            // 获取dexElements对象
            Object[] pluginDexElements = (Object[]) dexElementsField.get(pluginPathList);

            // 合并
            Object[] newElements = (Object[]) Array.newInstance(hostDexElements.getClass().getComponentType(),
                    hostDexElements.length + pluginDexElements.length);
            System.arraycopy(hostDexElements,0,newElements,0,hostDexElements.length);
            System.arraycopy(pluginDexElements,0,newElements,hostDexElements.length,pluginDexElements.length);

            // 赋值到宿主dexElement中去
            dexElementsField.set(hostPathList,newElements);

            Log.i(TAG, "loadClass success!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
