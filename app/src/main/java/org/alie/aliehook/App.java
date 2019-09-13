package org.alie.aliehook;

import android.app.Application;

/**
 * Created by Alie on 2019/9/8.
 * 类描述
 * 版本
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HookUtil hookUtil = new HookUtil();
        hookUtil.hookStartActivity(this);
    }
}
