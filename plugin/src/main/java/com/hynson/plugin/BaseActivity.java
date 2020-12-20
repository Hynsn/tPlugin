package com.hynson.plugin;

import android.app.Activity;
import android.content.res.Resources;

public class BaseActivity extends Activity {
    @Override
    public Resources getResources() {
        Resources res = LoadUtil.getResources(getApplication());
        return (res == null) ? super.getResources() : res;
    }
}
