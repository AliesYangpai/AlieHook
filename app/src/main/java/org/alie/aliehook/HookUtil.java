package org.alie.aliehook;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Alie on 2019/9/8.
 * 类描述
 * 版本
 */
public class HookUtil {

    private static final String  TAG = "HookUtil";

    public void hookStartActivity() {
        // 还原gDefault 而gefualt 是ActivityManagerNative的成员变量，所以我们当然是要先得到ActivityManagerNative
        try {
            Class<?> activityManagerNativeClas = Class.forName("android.app.ActivityManagerNative");
            Field gDefault = activityManagerNativeClas.getDeclaredField("gDefault");
            gDefault.setAccessible(true);// 由于gDefault是private修饰的，因此，此处要解禁访问权限
            // 因为是静态变量，所以这里的反射操作 获取的就是原系统中的那个变量
            // 这个gDefault是个伪hook点，只是因为它是静态，并且与真正hook点有关系而已
            Object defaultValue = gDefault.get(null);
            // 而我们真正要hook得到的是mInstance对象，因此我们接下来需要做的就是 通过反射来得到mInstance


            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstance = singletonClass.getDeclaredField("mInstance");
            // 还原IActivityManager系统对象
            mInstance.setAccessible(true);
            Object iActivtyManagerObject = mInstance.get(defaultValue);
            // 通过源码中发现 iActivtyManagerObject 中并没有提供设置接口的方法，因此，如果我们想接下来插入逻辑
            // 就要使用动态代理的方法了

            /**
             * 开始动态代理啦：
             * ClassLoader loader,
             * Class<?>[] interfaces,代表要实现的hook对象的特征接口,这个传入目标类之后，
             * 在newProxyInstance返回的代理对象中就自动实现了 目标类中的接口了
             * InvocationHandler invocationHandler：分发方法来被调用，这个分发是什么意思？所有在 代理对象中实现的方法
             * 都会调用invocationHandler 中的involk方法，
             * 比如我调用startActivity方法，那么当我们设置完后代理对象后，starActivity方法就会走我们的InvocationHandler中的
             * invoke方法 来 并传入相应的 参数
             */
            startActivtiy startActivtiyMethond = new startActivtiy(iActivtyManagerObject);
            Class<?> iActivityManagerInterapt = Class.forName("android.app.IActivityManager");
            Object oldIactivityManager = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{iActivityManagerInterapt}, startActivtiyMethond);

            // 将系统中 IactivityManger 替换为 我们代理生成的oldIactivityManager，
            mInstance.set(defaultValue, oldIactivityManager);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    class startActivtiy implements InvocationHandler {

        /**
         * 动态代理必须要有一个，源对象的引用，为什么呢 ？因为如果我们要使用源对象系统api，那么我们肯定要给一个
         * 源对象的引用，这样才能使用目标api呀
         */
        private Object iActivtyManagerObject;

        public startActivtiy(Object iActivtyManagerObject) {
            this.iActivtyManagerObject = iActivtyManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i(TAG,"=====invoke");
            if("startActivity".equals(method.getName())){
                Log.i(TAG,"=====invoke===startActivity");
            }
            return method.invoke(iActivtyManagerObject,args);
        }
    }
}
