package com.yanxiu.gphone.faceshow.user;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yanxiu.gphone.faceshow.R;
import com.yanxiu.gphone.faceshow.base.FaceShowBaseFragment;
import com.yanxiu.gphone.faceshow.customview.ClearEditText;
import com.yanxiu.gphone.faceshow.customview.PublicLoadLayout;
import com.yanxiu.gphone.faceshow.homepage.activity.checkIn.CheckInNotesActivity;
import com.yanxiu.gphone.faceshow.login.LoginActivity;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * 首页 “我的”Fragment
 */
public class MyFragment extends FaceShowBaseFragment {
    private final static String TAG = MyFragment.class.getSimpleName();
    private Unbinder unbinder;
    private PublicLoadLayout rootView;
    @BindView(R.id.person_img)
    ImageView person_img;
    @BindView(R.id.pregistration_img)
    ImageView pregistration_img;
    @BindView(R.id.person_info)
    RelativeLayout person_info;
    @BindView(R.id.registration)
    RelativeLayout registration;
    @BindView(R.id.ll_logout)
    LinearLayout ll_logout;
    @BindView(R.id.title_layout_title)
    TextView title_layout_title;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my, container, false);
        unbinder = ButterKnife.bind(this, view);
        title_layout_title.setText(R.string.my);
        return view;
    }

    @OnClick({R.id.person_info, R.id.registration, R.id.ll_logout})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.person_info://点击个人信息
                Intent i = new Intent(getActivity(), ProfileActivity.class);
                startActivity(i);
                break;
            case R.id.registration://点击签到记录
                CheckInNotesActivity.toThisAct(getActivity());
                break;
            case R.id.ll_logout:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

}