package com.yanxiu.gphone.faceshow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.StrictMode;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.google.gson.Gson;
import com.igexin.sdk.PushManager;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.tendcloud.tenddata.TCAgent;
import com.yanxiu.gphone.faceshow.constant.Constants;
import com.yanxiu.gphone.faceshow.getui.FaceShowGeTuiIntentService;
import com.yanxiu.gphone.faceshow.getui.FaceShowGeTuiService;
import com.yanxiu.gphone.faceshow.homepage.activity.WelcomeActivity;
import com.yanxiu.gphone.faceshow.http.envconfig.EnvConfigBean;
import com.yanxiu.gphone.faceshow.http.envconfig.UrlBean;
import com.yanxiu.gphone.faceshow.http.envconfig.UrlRepository;
import com.yanxiu.gphone.faceshow.util.CrashHandler;
import com.yanxiu.gphone.faceshow.util.FileUtil;
import com.yanxiu.gphone.faceshow.util.FrcLogUtils;
import com.yanxiu.gphone.faceshow.util.LBSManager;

import io.fabric.sdk.android.Fabric;
import org.litepal.LitePalApplication;

public class FaceShowApplication extends LitePalApplication {
    private static FaceShowApplication instance;

    public static FaceShowApplication getInstance() {
        return instance;
    }


    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        try {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            builder.detectFileUriExposure();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error error) {
            error.printStackTrace();
        }
        instance = this;
        initUrlServer();
        Stetho.initializeWithDefaults(this);
        CrashHandler.getInstance().init(this);
        /*个推初始化*/
        PushManager.getInstance().initialize(getApplicationContext(), FaceShowGeTuiService.class);
        // FaceShowGeTuiIntentService 为第三方自定义的推送服务事件接收类
        PushManager.getInstance().registerPushIntentService(this.getApplicationContext(), FaceShowGeTuiIntentService.class);
        LBSManager.init(getApplicationContext());

        TCAgent.LOG_ON = true;
        TCAgent.init(this, "37E19B68638D4FF5B7AF00360317BA71", "");
        TCAgent.setReportUncaughtExceptions(true);

        FrcLogUtils.init();
    }

    private void initUrlServer() {
        UrlBean urlBean;
        Gson gson = new Gson();
        String urlJson = FileUtil.getDataFromAssets(this, Constants.URL_SERVER_FILE_NAME);
        if (urlJson.contains(Constants.MULTICONFIG)) {
            EnvConfigBean envConfigBean = gson.fromJson(urlJson, EnvConfigBean.class);
            urlBean = envConfigBean.getData().get(envConfigBean.getCurrentIndex());
        } else {
            urlBean = gson.fromJson(urlJson, UrlBean.class);
        }
        UrlRepository.getInstance().setUrlBean(urlBean);
    }

}
