package com.yanxiu.gphone.faceshow.homepage.activity;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.stetho.common.StringUtil;
import com.test.yanxiu.network.HttpCallback;
import com.test.yanxiu.network.RequestBase;
import com.yanxiu.gphone.faceshow.FaceShowApplication;
import com.yanxiu.gphone.faceshow.R;
import com.yanxiu.gphone.faceshow.base.FaceShowBaseActivity;
import com.yanxiu.gphone.faceshow.db.SpManager;
import com.yanxiu.gphone.faceshow.http.login.GetUserInfoRequest;
import com.yanxiu.gphone.faceshow.http.login.GetUserInfoResponse;
import com.yanxiu.gphone.faceshow.http.login.SignInRequest;
import com.yanxiu.gphone.faceshow.http.login.SignInResponse;
import com.yanxiu.gphone.faceshow.login.LoginActivity;
import com.yanxiu.gphone.faceshow.login.UserInfo;
import com.yanxiu.gphone.faceshow.util.ToastUtil;

import java.lang.ref.WeakReference;

/**
 * Created by 戴延枫 on 2017/9/14.
 */

public class WelcomeActivity extends FaceShowBaseActivity {

    /**
     * add LOAD_TIME and change time
     * cwq
     */
    private static final int LOAD_TIME = 400;

    private RelativeLayout mRootView;

    private Handler mHander;
    private final static int GO_LOGIN = 0x0001;
    private final static int GO_MAIN = 0x0002;

    private Context mContext;
    private ImageView mImgLogo;
    private static boolean isAnimationEnd = false;
    private static boolean isGetUserInfoSuccess = false;
    private static boolean isGetUserInfoFailure = false;
    private Animator.AnimatorListener logoAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            isAnimationEnd = true;
            if (isGetUserInfoSuccess) {
                MainActivity.invoke(WelcomeActivity.this);
                WelcomeActivity.this.finish();
            }
            if (isGetUserInfoFailure) {
                LoginActivity.toThisAct(WelcomeActivity.this);
                WelcomeActivity.this.finish();
            }

        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        mContext = this;
        initView();
        /*欢迎页logo的动画效果*/
        mImgLogo.animate().translationY(-800).setDuration(1000).setListener(logoAnimatorListener);
        checkUserStatus();
    }

    private void initView() {
        mRootView = (RelativeLayout) findViewById(R.id.root_view);
        mImgLogo = (ImageView) findViewById(R.id.img_logo);
        mHander = new WelcomeHandler(this);

    }

    /**
     * 检查用户
     */
    private void checkUserStatus() {
        //TODO @荣成 判断用户信息是否登录
        if (TextUtils.isEmpty(SpManager.getToken())) {
            //用户信息不完整,跳转登录页
            mHander.sendEmptyMessageDelayed(GO_LOGIN, LOAD_TIME);
        } else {
            //用户信息完整，跳转首页
            mHander.sendEmptyMessageDelayed(GO_MAIN, LOAD_TIME);
        }
    }

    private static class WelcomeHandler extends Handler {

        private WeakReference<WelcomeActivity> mActivity;

        public WelcomeHandler(WelcomeActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final WelcomeActivity activity = mActivity.get();

            switch (msg.what) {
                case GO_LOGIN:
                    LoginActivity.toThisAct(activity);
                    activity.finish();
                    break;
                case GO_MAIN:
                    //进入首页
                    getUserInfo(activity);
                    break;
            }
        }
    }

    private static void getUserInfo(final Activity activity) {
        GetUserInfoRequest getUserInfoRequest = new GetUserInfoRequest();
        getUserInfoRequest.startRequest(GetUserInfoResponse.class, new HttpCallback<GetUserInfoResponse>() {
            @Override
            public void onSuccess(RequestBase request, GetUserInfoResponse ret) {
                if (ret.getCode() == 0) {
                    UserInfo.getInstance().setInfo(ret.getData());
                    isGetUserInfoSuccess = true;
                    if (isAnimationEnd) {
                        MainActivity.invoke(activity);
                        activity.finish();
                    }
                } else {
                    isGetUserInfoFailure = true;
                    if (isAnimationEnd) {
                        LoginActivity.toThisAct(activity);
                        activity.finish();
                    }
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                ToastUtil.showToast(FaceShowApplication.getContext(), error.getMessage());
                isGetUserInfoFailure = true;
                if (isAnimationEnd) {
                    LoginActivity.toThisAct(activity);
                    activity.finish();
                }

            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //退出程序
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        mHander.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}