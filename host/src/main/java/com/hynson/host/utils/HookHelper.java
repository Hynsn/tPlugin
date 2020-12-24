package com.hynson.host.utils;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.hynson.host.ProxyActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class HookHelper {

    private static final String TAG = "HookHelper";
    public static final String PLUGIN_INTENT = "plugin_intent";

    /**
     * Hook IActivityManager
     *
     * @throws Exception
     */
    public static void hookAMS() throws Exception {
        Log.e(TAG, "hookAMS");
        Object singleton = null;
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){
            // 获取ActivityManagerNative中的gDefault字段
            /*
            Class<?> activityManagerNativeClazz = Class.forName("android.app.ActivityManagerNative");
            Field gDefaultField = ReflectUtil.getField(activityManagerNativeClazz, "gDefault");
            singleton = gDefaultField.get(activityManagerNativeClazz);
            */
            singleton = ReflectUtil.get("android.app.ActivityManagerNative","gDefault",false);
        }
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            // 获取ActivityManager中的IActivityManagerSingleton字段
            /*
            Class<?> activityManageClazz = Class.forName("android.app.ActivityManager");
            Field iActivityManagerSingletonField = ReflectUtil.getField(activityManageClazz, "IActivityManagerSingleton");
            singleton = iActivityManagerSingletonField.get(activityManageClazz);
             */
            singleton = ReflectUtil.get("android.app.ActivityManager","IActivityManagerSingleton",false);
        }

        // 获取Singleton中mInstance字段
        Field mInstanceField = ReflectUtil.getField("android.util.Singleton", "mInstance",false);
        // 获取IActivityManager
        Object iActivityManager = mInstanceField.get(singleton);

        Class<?> iActivityManagerClazz = Class.forName("android.app.IActivityManager");
        // 获取IActivityManager代理对象
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerClazz}, new IActivityManagerProxy(iActivityManager));

        // 将IActivityManager代理对象赋值给Singleton中mInstance字段
        mInstanceField.set(singleton, proxy);
    }
//    public void hookAMSAction() throws Exception {
//        //动态代理
//        Class<?> mIActivityManagerClass;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            mIActivityManagerClass = Class.forName("android.app.IActivityTaskManager");
//        } else {
//            mIActivityManagerClass = Class.forName("android.app.IActivityManager");
//        }
//        //获取 ActivityManager 或 ActivityManagerNative 或 ActivityTaskManager
//        Class<?> mActivityManagerClass;
//        Method getActivityManagerMethod;
//        // 7.1及以下
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
//            mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
//            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
//        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // 7.1~9
//            mActivityManagerClass = Class.forName("android.app.ActivityManager");
//            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
//        } else { // 9.0及以上
//            mActivityManagerClass = Class.forName("android.app.ActivityTaskManager");
//            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
//        }
//        getActivityManagerMethod.setAccessible(true);
//        //这个实例本质是 IActivityManager或者IActivityTaskManager
//        final Object IActivityManager = getActivityManagerMethod.invoke(null);
//
//        //创建动态代理
//        Object mActivityManagerProxy = Proxy.newProxyInstance(
//                context.getClassLoader(),
//                new Class[]{mIActivityManagerClass},//要监听的回调接口
//                new InvocationHandler() {
//                    @Override
//                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//
//                        if ("startActivity".equals(method.getName())) {
//                            //做自己的业务逻辑
//                            //换成可以通过AMS检测的Activity
//                            Intent intent = new Intent(context, ProxyActivity.class);
//                            intent.putExtra("actonIntent", (Intent) args[2]);
//                            args[2] = intent;
//                        }
//                        //class android.app.ActivityManagerProxy
//                        Log.i(TAG, "invoke: "+IActivityManager.getClass());
//                        //让程序继续能够执行下去
//                        return method.invoke(IActivityManager, args);
//                    }
//                }
//        );
//
//        //获取 IActivityTaskManagerSingleton 或者 IActivityManagerSingleton 或者 gDefault 属性
//        Field mSingletonField;
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
//            mSingletonField = mActivityManagerClass.getDeclaredField("gDefault");
//        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
//        } else {
//            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
//        }
//        mSingletonField.setAccessible(true);
//        Object mSingleton = mSingletonField.get(null);
//
//        //替换点
//        Class<?> mSingletonClass = Class.forName("android.util.Singleton");
//        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
//        mInstanceField.setAccessible(true);
//        //将我们创建的动态代理设置到 mInstance 属性当中
//        mInstanceField.set(mSingleton, mActivityManagerProxy);
//    }


    /**
     * Hook ActivityThread中Handler成员变量mH
     *
     * @throws Exception
     */
    public static void hookHandler() throws Exception {
        Log.e(TAG, "hookHandler");

        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        /*
        // 获取ActivityThread中成员变量sCurrentActivityThread字段
        Field sCurrentActivityThreadField = ReflectUtil.getField(activityThreadClazz, "");
        // 获取ActivityThread主线程对象(应用程序启动后就会在attach方法中赋值)
        Object currentActivityThread = sCurrentActivityThreadField.get(activityThreadClazz);
        */
        Object currentActivityThread = ReflectUtil.get(activityThreadClazz,"sCurrentActivityThread",false);

        // 获取ActivityThread中成员变量mH字段
        // 获取ActivityThread主线程中Handler对象
        Field mHField = ReflectUtil.getField(activityThreadClazz, "mH",false);
        Handler mH = (Handler) mHField.get(currentActivityThread);

        // 将我们自己的HCallback对象赋值给mH的mCallback
        ReflectUtil.set(Handler.class, "mCallback", mH, new HCallback());
    }
//    public static void hookHandler1() throws Exception {
//        //获取 ActivityThread 类
//        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
//
//        //获取 ActivityThread 的 currentActivityThread() 方法
//        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
//        currentActivityThread.setAccessible(true);
//        //获取 ActivityThread 实例
//        Object mActivityThread = currentActivityThread.invoke(null);
//
//        //获取 ActivityThread 的 mH 属性
//        Field mHField = mActivityThreadClass.getDeclaredField("mH");
//        mHField.setAccessible(true);
//        Handler mH = (Handler) mHField.get(mActivityThread);
//
//        //获取 Handler 的 mCallback 属性
//        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
//        mCallbackField.setAccessible(true);
//        //设置我们自定义的 CallBack
//        mCallbackField.set(mH, new HCallback());
//    }
    /**
     * Hook Instrumentation
     *
     * @param context 上下文环境
     * @throws Exception
     */
    public static void hookInstrumentation(Context context) throws Exception {
        Log.e(TAG, "hookInstrumentation");
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        // 获取ActivityThread中成员变量sCurrentActivityThread字段
        // 获取ActivityThread主线程对象(应用程序启动后就会在attach方法中赋值)
        Object currentActivityThread = ReflectUtil.getField(activityThreadClazz, "sCurrentActivityThread",false).get(activityThreadClazz);
        // 获取ActivityThread中成员变量mInstrumentation字段
        // 获取Instrumentation对象
        Instrumentation instrumentation = (Instrumentation) (ReflectUtil.getField(activityThreadClazz, "mInstrumentation",false).get(currentActivityThread));
        // 创建Instrumentation代理对象
        InstrumentationProxy instrumentationProxy = new InstrumentationProxy(instrumentation, context.getPackageManager());

        // 用InstrumentationProxy代理对象替换原来的Instrumentation对象
        ReflectUtil.set(activityThreadClazz, "mInstrumentation", currentActivityThread, instrumentationProxy);
    }

    public static void hookInstrumentation1(Context context) {
        try {
            Class contextImplClz = Class.forName("android.app.ContextImpl");
            Field mMainThread = contextImplClz.getDeclaredField("mMainThread");
            mMainThread.setAccessible(true);
            Object activityThread = mMainThread.get(context);

            Class activityThreadClz = Class.forName("android.app.ActivityThread");
            Field mInstrumentationField = activityThreadClz.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            mInstrumentationField.set(activityThread,
                    new InstrumentationProxy((Instrumentation) mInstrumentationField.get(activityThread),
                            context.getPackageManager()));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("plugin", "hookInstrumentation: error");
        }
    }
}
