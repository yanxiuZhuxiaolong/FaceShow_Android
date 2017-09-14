package com.yanxiu.gphone.faceshow.db;

import android.content.Context;
import android.content.SharedPreferences;

import com.yanxiu.gphone.faceshow.FaceShowApplication;

/**
 * sharePreference管理类
 * 所有sp存储，都应该写在该类里
 */
public class SpManager {

    public static final String SP_NAME = "faceshow_sp";
    private static SharedPreferences mySharedPreferences = FaceShowApplication.getInstance().getContext()
            .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

    private static SpManager instance;

    public static SpManager getInstance() {
        if (instance == null) {
            instance = new SpManager();
            mySharedPreferences = FaceShowApplication.getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }

        return instance;
    }

    /**
     * 第一次启动
     */
    private static final String FRIST_START_UP = "frist_start_up";
    /**
     * 版本号
     */
    private static final String APP_VERSION_CODE = "version_code";
    /*用户是否已经登录成功*/
    private static final String IS_LOGINED = "is_login";


    public static void setFristStartUp(boolean isFristStartUp) {
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putBoolean(FRIST_START_UP, isFristStartUp);
        editor.commit();
    }

    /**
     * 是否第一次启动
     *
     * @return true ： 第一次
     */
    public static boolean isFristStartUp() {
        return mySharedPreferences.getBoolean(FRIST_START_UP, true);
    }

    /**
     * app版本号
     *
     * @return -1 ：没记录
     */
    public static void setAppVersionCode(int versionCode) {
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putInt(APP_VERSION_CODE, versionCode);
        editor.commit();
    }

    /**
     * app版本号
     *
     * @return -1 ：没记录
     */
    public static int getAppVersionCode() {
        return mySharedPreferences.getInt(APP_VERSION_CODE, -1);
    }

    /**
     * 是否已经登录
     *
     * @return false:未登录   true :登录
     */
    public static boolean isLogined() {
        return mySharedPreferences.getBoolean(IS_LOGINED, false);
    }


    /**
     * 设置为登录状态
     */
    public static void haveSignIn() {
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putBoolean(IS_LOGINED, true);
        editor.commit();
    }


}