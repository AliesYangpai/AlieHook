package org.alie.aliehook;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
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

    private Context context;

    public  void hookHookMh(Context context  ) {
        try {
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = forName.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
//            还原系统的ActivityTread   mH
            Object activityThreadObj=currentActivityThreadField.get(null);

            Field handlerField = forName.getDeclaredField("mH");
            handlerField.setAccessible(true);
//            hook点找到了
            Handler mH= (Handler) handlerField.get(activityThreadObj);
            Field callbackField = Handler.class.getDeclaredField("mCallback");

            callbackField.setAccessible(true);

            callbackField.set(mH,new ActivityMH(mH));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public void hookStartActivity(Context context) {
        this.context = context;
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
            /**
             * 这一步操作，是反射中的替换，目的是将 gDefault(Singleton对象)中的mIntsance替换成
             * 我们的oldIactivityManager，怎么做呢？
             * mInstance ：原属性
             * defaultValue： mInstance所属的那个类的对象
             * oldIactivityManager ：动态代理构造出来的类
             */
            mInstance.set(defaultValue, oldIactivityManager);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    class ActivityMH implements  Handler.Callback{
        private  Handler mH;

        public ActivityMH(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {
//LAUNCH_ACTIVITY ==100 即将要加载一个activity了
            if (msg.what == 100) {
//加工 --完  一定丢给系统  secondActivity  -hook->proxyActivity---hook->    secondeActivtiy
                handleLuachActivity(msg);
            }
//做了真正的跳转
            mH.handleMessage(msg);
            return  true;
        }

        private void handleLuachActivity(Message msg) {
//            还原
            Object obj = msg.obj;
            try {
                Field intentField=obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                //  ProxyActivity   2
                Intent realyIntent = (Intent) intentField.get(obj);
//                sconedActivity  1
                Intent oldIntent = realyIntent.getParcelableExtra("oldIntent");
                if (oldIntent != null) {

                    // *** 下面就是逻辑判断了
//                    集中式登录
//                    SharedPreferences share = context.getSharedPreferences("david",
//                            Context.MODE_PRIVATE);
//                    if (share.getBoolean("login",false)||oldIntent.getComponent().getClassName().equals(SceondActivity.class.getName())) {
//
////                      登录  还原  把原有的意图    放到realyIntent
//                        realyIntent.setComponent(oldIntent.getComponent());
//                    }else {
//                        ComponentName componentName = new ComponentName(context,LoginActivity.class);
//                        realyIntent.putExtra("extraIntent", oldIntent.getComponent()
//                                .getClassName());
//                        realyIntent.setComponent(componentName);
//                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }


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
                // 开始 操作回传的 Object[] args 数组参数，我们要找到arg中的intent

                /**
                 * 以下这波操作目的是找到系统的intent，之后把它给封装到newIntent中并替换
                 */
                Intent intent = null;
                int index= 0;
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if(arg instanceof Intent) {
                        intent = (Intent) arg;
                        index = i;
                    }
                }

                Intent newIntent = new Intent();
                ComponentName componentName = new ComponentName(context,ProxyActivity.class);
                newIntent.setComponent(componentName);
                newIntent.putExtra("oldIntent",intent);


                args[index] = newIntent;

            }
            return method.invoke(iActivtyManagerObject,args);
        }
    }
}
