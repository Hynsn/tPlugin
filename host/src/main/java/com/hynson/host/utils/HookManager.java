package com.hynson.host.utils;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Log;

import com.hynson.host.ProxyActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HookManager {

    private static final String TAG = HookManager.class.getSimpleName();
    public static final String PLUGIN_INTENT = "plugin_intent";

    private Context mContext;
    private static HookManager instance;

    private HookManager(Context context) {
        this.mContext = context;
    }

    public static HookManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HookManager.class) {
                if (instance == null) {
                    instance = new HookManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Hook IActivityManager
     *
     * @throws Exception
     */
    public static void hookAMS() throws Exception {
        Log.e(TAG, "hookAMS");
        Object singleton = null;
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){ // 获取ActivityManagerNative中的gDefault字段
            singleton = ReflectUtil.get("android.app.ActivityManagerNative","gDefault",false);
        }
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){ // 获取ActivityManager中的IActivityManagerSingleton字段
            singleton = ReflectUtil.get("android.app.ActivityManager","IActivityManagerSingleton",false);
        }
        else { // 获取ActivityTaskManager中的IActivityTaskManagerSingleton字段
            singleton = ReflectUtil.get("android.app.ActivityTaskManager","IActivityTaskManagerSingleton",false);
        }

        // 获取Singleton中mInstance字段
        Field mInstanceField = ReflectUtil.getField("android.util.Singleton", "mInstance",false);
        // 获取IActivityManager
        Object iActivityManager = mInstanceField.get(singleton);
        Class<?> iActivityManagerClazz = null;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            iActivityManagerClazz = Class.forName("android.app.IActivityManager");
        }
        else {
            iActivityManagerClazz = Class.forName("android.app.IActivityTaskManager");
        }
        // 获取IActivityManager代理对象
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerClazz}, new IActivityManagerProxy(iActivityManager));

        // 将IActivityManager代理对象赋值给Singleton中mInstance字段
        mInstanceField.set(singleton, proxy);
    }

    public static void hookLaunchActivity() throws Exception {
        //获取 ActivityThread 类
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");

        //获取 ActivityThread 的 currentActivityThread() 方法
        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        //获取 ActivityThread 实例
        Object mActivityThread = currentActivityThread.invoke(null);

        //获取 ActivityThread 的 mH 属性
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(mActivityThread);

        //获取 Handler 的 mCallback 属性
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        //设置我们自定义的 CallBack
        mCallbackField.set(mH, new HCallback());
    }

    /**
     * Hook ActivityThread中Handler成员变量mH
     *
     * @throws Exception
     */
    public void hookHandler() throws Exception {
        // 获取ActivityThread中成员变量sCurrentActivityThread字段（应用程序启动后就会在attach方法中赋值）
        Object currentActivityThread = ReflectUtil.get("android.app.ActivityThread","sCurrentActivityThread",false);
        // 通过获取ActivityThread中成员变量mH字段，获取Handler对象
        Handler mH = (Handler) ReflectUtil.get(currentActivityThread,"mH",false);

        // 将我们自己的HCallback对象赋值给mH的mCallback
        ReflectUtil.set(Handler.class, "mCallback", mH, new TCallback());
    }
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
            InstrumentationProxy instrumentationProxy = new InstrumentationProxy((Instrumentation) mInstrumentationField.get(activityThread),context.getPackageManager());
            mInstrumentationField.set(activityThread,instrumentationProxy);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("plugin", "hookInstrumentation: error");
        }
    }

    /**
     * 获取插件文件的ApplicationInfo
     * @param file
     * @return
     * @throws Exception
     */
    private ApplicationInfo getAppInfo(File file) throws Exception {
        /*
            执行此方法获取 ApplicationInfo
            public static ApplicationInfo generateApplicationInfo(Package p, int flags,PackageUserState state)
         */
        Class<?> mPackageParserClazz = Class.forName("android.content.pm.PackageParser");
        Class<?> mPackageClazz = Class.forName("android.content.pm.PackageParser$Package");
        Class<?> mPackageUserStateClazz = Class.forName("android.content.pm.PackageUserState");

        //获取 generateApplicationInfo 方法
        Method generateApplicationInfoMethod = mPackageParserClazz.getDeclaredMethod("generateApplicationInfo",
                mPackageClazz, int.class, mPackageUserStateClazz);

        //创建 PackageParser 实例
        Object mPackageParser = mPackageParserClazz.newInstance();

        /*
            执行此方法获取 Package 实例
            public Package parsePackage(File packageFile, int flags)
         */
        //获取 parsePackage 方法
        Method parsePackageMethod = mPackageParserClazz.getDeclaredMethod("parsePackage", File.class, int.class);
        //执行 parsePackage 方法获取 Package 实例
        Object mPackage = parsePackageMethod.invoke(mPackageParser, file, PackageManager.GET_ACTIVITIES);

        //执行 generateApplicationInfo 方法，获取 ApplicationInfo 实例
        ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(null, mPackage, 0,
                mPackageUserStateClazz.newInstance());
        // 设置ApplicationInfo路径，获取的默认路径未设置
        applicationInfo.sourceDir = file.getAbsolutePath();
        applicationInfo.publicSourceDir = file.getAbsolutePath();
        return applicationInfo;
    }

    /**
     * 自己创造一个LoadedApk.ClassLoader 添加到 mPackages，此LoadedApk 专门用来加载插件里面的 class
     * 倒序写代码的方式
     */
//    private void customLoadedApkAction() throws Exception{
////         //public final LoadedApk getPackageInfo(ApplicationInfo ai, CompatibilityInfo compatInfo,int flags)
////        //要得到一个LoadedApk 我们需要调用ActivityThread 的 getPackageInfo方法
////
//        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
//        Class mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
////        //getPackageInfo 需要的三个参数类型的 class 类类型
//        Method mGetPackageInfoMethod = mActivityThreadClass.getMethod("getPackageInfo",
//                ApplicationInfo.class, mCompatibilityInfoClass, int.class);
////
////        //要执行getPackageInfo 这个方法 需要传递上面的中三个参数之外，还需要传入你需要执行的方法是属于哪个类 哪个实例的
////        //也就是我们需要获得ActivityThread的实例
//
//        //TODO @1 参数的获取
//        //public static ActivityThread currentActivityThread() 得到ActivityThread的实例
//        Method mCurrentActivityThreadMethod = mActivityThreadClass.getMethod("currentActivityThread");
//        //因为是静态方法 切方法不需要参数 所以第一参数传递null就可以了
//        Object mActivtyThreadObj = mCurrentActivityThreadMethod.invoke(null);
//
//        //TODO @2 参数的获取 调用PackageParser中的generateApplicationInfo 方法
//        //public static ApplicationInfo generateApplicationInfo(Package p, int flags,
//        //            PackageUserState state, int userId)
//        ApplicationInfo mApplicationInfoPlugin = getApplicationInfoAction();
//
//        //AppBindData mBoundApplication
//        /*Field mBoundApplicationField = mActivityThreadClass.getDeclaredField("mBoundApplication");
//        mBoundApplicationField.setAccessible(true);
//        Object mBoundApplicationObj = mBoundApplicationField.get(mActivtyThreadObj);
//        //ApplicationInfo appInfo
//        Field mMainAppInfoField = mBoundApplicationObj.getClass().getDeclaredField("appInfo");
//        mMainAppInfoField.setAccessible(true);
//        ApplicationInfo mainAppInfo = (ApplicationInfo)mMainAppInfoField.get(mBoundApplicationObj);
//        mApplicationInfo.uid = mainAppInfo.uid;*/
//        //TODO ActiivtyThread 中 getPackageInfo得到loadedApk的时候 会检查插件的uid是否是等于主app的uid，不相等就会抛出上面的错误，
//        // 因此，当我们利用反色调用getPackageInfo获取LoadedApk的时候，需要将插件的uid设置为何宿主App一样的才能绕过检查,也可以用上面的反射获得
//        ApplicationInfo mainApplicationInfo = getPackageManager().getApplicationInfo("com.android.plugin.loadedapk", 0);
//        mApplicationInfoPlugin.uid = mainApplicationInfo.uid;
//
//        //TODO @3 参数的获取 看CompatibilityInfo 的源码 获取默认的 public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO
//        Field mDefaultCompatibilityFiled = mCompatibilityInfoClass.getField("DEFAULT_COMPATIBILITY_INFO");
//        mDefaultCompatibilityFiled.setAccessible(true);
//        Object mCompatibilityInfoObj = mDefaultCompatibilityFiled.get(null);
//
//        //TODO @4 看源码 传递 Context.CONTEXT_INCLUDE_CODE 我们也可以传递这个常量值
//        //要执行getPackageInfo 这个方法 需要传递上面的中三个参数之外，还需要传入你需要执行的方法是属于哪个类 哪个实例的
//        //也就是我们需要获得ActivityThread的实例 @1 ActivityThread  @2 ApplicationInfo  @3 CompatibilityInfo  @4 int
//        Object mLoadedApkProxy = mGetPackageInfoMethod.invoke(mActivtyThreadObj,mApplicationInfoPlugin,
//                mCompatibilityInfoObj, Context.CONTEXT_INCLUDE_CODE);
//
//
//        Field mPackagesField = mActivityThreadClass.getDeclaredField("mPackages");
//        mPackagesField.setAccessible(true);
//        // 拿到mPackages对象
//        Object mPackagesObj = mPackagesField.get(mActivtyThreadObj);
//
//        Map mPackages = (Map) mPackagesObj;
//
//
//        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "pluginpackage-debug.apk");
//        String pulginPath = file.getAbsolutePath();
//        File fileDir = getDir("pulginPathDir", Context.MODE_PRIVATE);
//
//        //得到 loadedApk 里面需要加载插件里面class的ClassLoader
//        PluginClassLoader proxyClassLoader = new PluginClassLoader(pulginPath,fileDir.getAbsolutePath(),
//                null,getClassLoader());
//
//        Field mClassLoaderField = mLoadedApkProxy.getClass().getDeclaredField("mClassLoader");
//        mClassLoaderField.setAccessible(true);
//        mClassLoaderField.set(mLoadedApkProxy, proxyClassLoader); // 替换 LoadedApk 里面的 ClassLoader
//
//        // 添加自定义的 LoadedApk 专门加载 插件里面的 class
//
//        // 最终的目标 mPackages.put(插件的包名，插件的LoadedApk);
//        WeakReference weakReference = new WeakReference(mLoadedApkProxy); // 放入 自定义的LoadedApk --》 插件的
//        Log.e("TAG","mApplicationInfo 包名为："+mApplicationInfoPlugin.packageName);
//        mPackages.put(mApplicationInfoPlugin.packageName, weakReference); // 增加了我们自己的LoadedApk
//    }

//    public void customLoadApkAction() throws Exception {
//        long start = System.currentTimeMillis();
//        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin-debug.apk");
//        if (!file.exists()) {
//            throw new FileNotFoundException("插件包不存在");
//        }
//
//        //获取 ActivityThread 类
//        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
//        //通过ActivityThread的currentActivityThread()方法 获得 ActivityThread对象
//        Object mActivityThread = ReflectUtil.getNullMethod(mActivityThreadClass,"currentActivityThread",false);
//        //获取 mPackages 属性 ArrayMap<String, WeakReference<LoadedApk>>
//        Field mPackagesField = ReflectUtil.getField(mActivityThreadClass,"mPackages",false);
//        ArrayMap<String, Object> mPackages = (ArrayMap<String, Object>) mPackagesField.get(mActivityThread);
//
//        for (int i = 0; i < mPackages.size(); i++) {
//            Log.i(TAG, "customLoadApkAction:1 "+mPackages.keyAt(i));
//        }
//
//        //自定义一个 LoadedApk，系统是如何创建的我们就如何创建
//        //执行下面的方法会返回一个 LoadedApk，我们就仿照系统执行此方法
//        /*
//              this.packageInfo = client.getPackageInfoNoCheck(activityInfo.applicationInfo,
//                    compatInfo);
//              public final LoadedApk getPackageInfo(ApplicationInfo ai, CompatibilityInfo compatInfo,
//                    int flags)
//         */
//        Class<?> mCompatibilityInfoClazz = Class.forName("android.content.res.CompatibilityInfo");
//        Method getLoadedApkMethod = mActivityThreadClass.getDeclaredMethod("getPackageInfoNoCheck",
//                ApplicationInfo.class, mCompatibilityInfoClazz);
//
//        /*
//             public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {};
//         */
//        //以上注释是获取默认的 CompatibilityInfo 实例
//        Field mCompatibilityInfoDefaultField = mCompatibilityInfoClazz.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
//        Object mCompatibilityInfo = mCompatibilityInfoDefaultField.get(null);
//
//        //获取一个 ApplicationInfo实例
//        ApplicationInfo applicationInfo = getAppInfo(file);
////        applicationInfo.uid = context.getApplicationInfo().uid;
//        //执行此方法，获取一个 LoadedApk
//        Object mLoadedApk = getLoadedApkMethod.invoke(mActivityThread, applicationInfo, mCompatibilityInfo);
//
//        //自定义一个 ClassLoader
//        String optimizedDirectory = mContext.getDir("plugin", Context.MODE_PRIVATE).getAbsolutePath();
//        Log.i(TAG, "customLoadApkAction: "+optimizedDirectory);
//        PluginClassLoader classLoader = new PluginClassLoader(file.getAbsolutePath(), optimizedDirectory,
//                null, mContext.getClassLoader());
//
//        //private ClassLoader mClassLoader;
//        //获取 LoadedApk 的 mClassLoader 属性
//        Field mClassLoaderField = mLoadedApk.getClass().getDeclaredField("mClassLoader");
//        mClassLoaderField.setAccessible(true);
//        //设置自定义的 classLoader 到 mClassLoader 属性中
//        mClassLoaderField.set(mLoadedApk, classLoader);
//
//        WeakReference loadApkReference = new WeakReference(mLoadedApk);
//        //添加自定义的 LoadedApk
//        mPackages.put(applicationInfo.packageName, loadApkReference);
//        //重新设置 mPackages
//        mPackagesField.set(mActivityThread, mPackages);
//        for (int i = 0; i < mPackages.size(); i++) {
//            Log.i(TAG, "customLoadApkAction:2 "+mPackages.keyAt(i));
//        }
//        Log.i(TAG, "customLoadApkAction: "+(System.currentTimeMillis() - start));
//    }

    public void customLoadApkAction() throws Exception {
        long start = System.currentTimeMillis();
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin-debug.apk");
        if (!file.exists()) {
            throw new FileNotFoundException("插件包不存在");
        }

        //获取 ActivityThread 类
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
        //获取 ActivityThread 的 currentActivityThread() 方法
        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        //获取 ActivityThread 实例
        Object mActivityThread = currentActivityThread.invoke(null);
        Log.i(TAG, "customLoadApkAction: class "+mActivityThread.getClass());
        //final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<>();
        //获取 mPackages 属性
        Field mPackagesField = mActivityThreadClass.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);
        //获取 mPackages 属性的值
        ArrayMap<String, Object> mPackages = (ArrayMap<String, Object>) mPackagesField.get(mActivityThread);

        for (int i = 0; i < mPackages.size(); i++) {
            Log.i(TAG, "customLoadApkAction:1 "+mPackages.keyAt(i));
        }
//        if (mPackages.size() >= 2) {
//            return;
//        }

        //自定义一个 LoadedApk，系统是如何创建的我们就如何创建
        //执行下面的方法会返回一个 LoadedApk，我们就仿照系统执行此方法
        /*
              this.packageInfo = client.getPackageInfoNoCheck(activityInfo.applicationInfo,
                    compatInfo);
              public final LoadedApk getPackageInfo(ApplicationInfo ai, CompatibilityInfo compatInfo,
                    int flags)
         */
        Class<?> mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Method getLoadedApkMethod = mActivityThreadClass.getDeclaredMethod("getPackageInfoNoCheck",
                ApplicationInfo.class, mCompatibilityInfoClass);

        /*
             public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {};
         */
        //以上注释是获取默认的 CompatibilityInfo 实例
        Field mCompatibilityInfoDefaultField = mCompatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        Object mCompatibilityInfo = mCompatibilityInfoDefaultField.get(null);

        //获取一个 ApplicationInfo实例
        ApplicationInfo applicationInfo = getAppInfo(file);
//        applicationInfo.uid = context.getApplicationInfo().uid;
        //执行此方法，获取一个 LoadedApk
        Object mLoadedApk = getLoadedApkMethod.invoke(mActivityThread, applicationInfo, mCompatibilityInfo);

        //自定义一个 ClassLoader
        String optimizedDirectory = mContext.getDir("plugin", Context.MODE_PRIVATE).getAbsolutePath();
        Log.i(TAG, "customLoadApkAction: "+optimizedDirectory);
        PluginClassLoader classLoader = new PluginClassLoader(file.getAbsolutePath(), optimizedDirectory,
                null, mContext.getClassLoader());

        //private ClassLoader mClassLoader;
        //获取 LoadedApk 的 mClassLoader 属性
        Field mClassLoaderField = mLoadedApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        //设置自定义的 classLoader 到 mClassLoader 属性中
        mClassLoaderField.set(mLoadedApk, classLoader);

        WeakReference loadApkReference = new WeakReference(mLoadedApk);
        //添加自定义的 LoadedApk
        mPackages.put(applicationInfo.packageName, loadApkReference);
        //重新设置 mPackages
        mPackagesField.set(mActivityThread, mPackages);
        for (int i = 0; i < mPackages.size(); i++) {
            Log.i(TAG, "customLoadApkAction:2 "+mPackages.keyAt(i));
        }
        Log.i(TAG, "customLoadApkAction: "+(System.currentTimeMillis() - start));
    }

    private void hookGetPackageInfo() throws Exception {
        //static volatile IPackageManager sPackageManager;
        //获取 系统的 sPackageManager 把它替换成我们自己的动态代理
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
        Field mPackageManager1Field = mActivityThreadClass.getDeclaredField("sPackageManager");
        mPackageManager1Field.setAccessible(true);
        final Object sPackageManager = mPackageManager1Field.get(null);

        //public static IPackageManager getPackageManager()
        // 获取getPackageManager方法
//        Method getPackageManagerMethod = mActivityThreadClass.getDeclaredMethod("getPackageManager");
//        //执行 getPackageManager方法，得到 sPackageManager
//        final Object sPackageManager = getPackageManagerMethod.invoke(null);

        Class<?> mIPackageManagerClass = Class.forName("android.content.pm.IPackageManager");

        //实现动态代理
        Object mPackageManagerProxy = Proxy.newProxyInstance(
                mContext.getClassLoader(),
                new Class[]{mIPackageManagerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("getPackageInfo".equals(method.getName())) {
                            //如果是 getPackageInfo 方法才做我们的逻辑
                            //如何才能绕过 PMS检查呢
                            //pi != null
                            //直接返回一个 PackageInfo
                            Log.e("yuongzw", method.getName());
                            return new PackageInfo();

                        }
                        return method.invoke(sPackageManager, args);
                    }
                }
        );

        //替换 换成我们自己的动态代理
        mPackageManager1Field.set(null, mPackageManagerProxy);
    }

    public void hookAMSAction() throws Exception {
        //动态代理
        Class<?> mIActivityManagerClass;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mIActivityManagerClass = Class.forName("android.app.IActivityTaskManager");
        } else {
            mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        }
        //获取 ActivityManager 或 ActivityManagerNative 或 ActivityTaskManager
        Class<?> mActivityManagerClass;
        Method getActivityManagerMethod;
        // 7.1及以下
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // 7.1~9
            mActivityManagerClass = Class.forName("android.app.ActivityManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        } else { // 9.0及以上
            mActivityManagerClass = Class.forName("android.app.ActivityTaskManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        }
        getActivityManagerMethod.setAccessible(true);
        //这个实例本质是 IActivityManager或者IActivityTaskManager
        final Object IActivityManager = getActivityManagerMethod.invoke(null);

        //创建动态代理
        Object mActivityManagerProxy = Proxy.newProxyInstance(
                mContext.getClassLoader(),
                new Class[]{mIActivityManagerClass},//要监听的回调接口
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())) {
                            //做自己的业务逻辑
                            //换成可以通过AMS检测的Activity
                            Intent intent = new Intent(mContext, ProxyActivity.class);
                            intent.putExtra(PLUGIN_INTENT, (Intent) args[2]);
                            args[2] = intent;
                        }
                        //class android.app.ActivityManagerProxy
                        Log.i(TAG, "invoke: "+IActivityManager.getClass());
                        //让程序继续能够执行下去
                        return method.invoke(IActivityManager, args);
                    }
                }
        );

        //获取 IActivityTaskManagerSingleton 或者 IActivityManagerSingleton 或者 gDefault 属性
        Field mSingletonField;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mSingletonField = mActivityManagerClass.getDeclaredField("gDefault");
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
        } else {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
        }
        mSingletonField.setAccessible(true);
        Object mSingleton = mSingletonField.get(null);

        //替换点
        Class<?> mSingletonClass = Class.forName("android.util.Singleton");
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //将我们创建的动态代理设置到 mInstance 属性当中
        mInstanceField.set(mSingleton, mActivityManagerProxy);
    }



    public static final int EXECUTE_TRANSACTION = 159;
    public static final int LAUNCH_ACTIVITY = 100;

    class TCallback implements Handler.Callback{
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == EXECUTE_TRANSACTION) {
                try {
                    Field mActivityCallbacksField = msg.obj.getClass().getDeclaredField("mActivityCallbacks");
                    mActivityCallbacksField.setAccessible(true);
                    List<Object> mActivityCallbacks = (List<Object>) mActivityCallbacksField.get(msg.obj);
                    if (mActivityCallbacks != null && mActivityCallbacks.size() > 0) {
                        Object mClientTransactionItem = mActivityCallbacks.get(0);
                        Class<?> mLaunchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");
                        if (mLaunchActivityItemClass.isInstance(mClientTransactionItem)) {
                            //获取 LaunchActivityItem 的 mIntent 属性
                            Field mIntentField = mClientTransactionItem.getClass().getDeclaredField("mIntent");
                            mIntentField.setAccessible(true);
                            Intent intent = (Intent) mIntentField.get(mClientTransactionItem);
                            //取出我们传递的值
                            Intent actonIntent = intent.getParcelableExtra(PLUGIN_INTENT);
                            Log.i("TAG", "handleMessage: "+(actonIntent==null));
                            /**
                             * 我们在以下代码中，对插件 和 宿主进行区分
                             */
                            Field mActivityInfoField = mClientTransactionItem.getClass().getDeclaredField("mInfo");
                            mActivityInfoField.setAccessible(true);
                            ActivityInfo mActivityInfo = (ActivityInfo) mActivityInfoField.get(mClientTransactionItem);

                            if (actonIntent != null) {
                                //替换掉原来的intent属性的值
                                mIntentField.set(mClientTransactionItem, actonIntent);
                                //证明是插件
                                if (actonIntent.getPackage() == null) {
                                    mActivityInfo.applicationInfo.packageName = actonIntent.getComponent().getPackageName();
                                    //hook 拦截 getPackageInfo 做自己的逻辑
                                    hookGetPackageInfo();
                                } else {
                                    //宿主的
                                    mActivityInfo.applicationInfo.packageName = actonIntent.getPackage();
                                }
                            }

                            mActivityInfoField.set(mClientTransactionItem, mActivityInfo);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (msg.what == LAUNCH_ACTIVITY) {

                /*
                    7.0以下代码
                     case LAUNCH_ACTIVITY: {
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                    final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

                    r.packageInfo = getPackageInfoNoCheck(
                            r.activityInfo.applicationInfo, r.compatInfo);
                    handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                } break;

                 */
                try {
                    //获取 ActivityClientRecord 的 intent 属性
                    Field intentField = msg.obj.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);
                    Intent intent = (Intent) intentField.get(msg.obj);
                    //取出我们传递的值
                    Intent actonIntent = intent.getParcelableExtra(PLUGIN_INTENT);
                    Log.i("TAG", "handleMessage: "+msg.toString());
                    /**
                     * 我们在以下代码中，对插件 和 宿主进行区分
                     */
                    Field mActivityInfoField = msg.obj.getClass().getDeclaredField("activityInfo");
                    mActivityInfoField.setAccessible(true);
                    ActivityInfo mActivityInfo = (ActivityInfo) mActivityInfoField.get(msg.obj);

                    if (actonIntent != null) {
                        //替换掉原来的intent属性的值
                        intentField.set(msg.obj, actonIntent);
                        //证明是插件
                        if (actonIntent.getPackage() == null) {
                            mActivityInfo.applicationInfo.packageName = actonIntent.getComponent().getPackageName();
                            hookGetPackageInfo();
                        } else {
                            //宿主的
                            mActivityInfo.applicationInfo.packageName = actonIntent.getPackage();
                        }
                    }
                    mActivityInfoField.set(msg.obj, mActivityInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }
}
