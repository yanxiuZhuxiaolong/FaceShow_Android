package com.yanxiu.gphone.faceshow.classcircle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.test.yanxiu.network.HttpCallback;
import com.test.yanxiu.network.RequestBase;
import com.yanxiu.gphone.faceshow.R;
import com.yanxiu.gphone.faceshow.base.FaceShowBaseActivity;
import com.yanxiu.gphone.faceshow.base.FaceShowBaseFragment;
import com.yanxiu.gphone.faceshow.classcircle.activity.SendClassCircleActivity;
import com.yanxiu.gphone.faceshow.classcircle.adapter.ClassCircleAdapter;
import com.yanxiu.gphone.faceshow.classcircle.dialog.ClassCircleDialog;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleCancelLikeRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleCommentToMasterRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleCommentToUserRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleLikeRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.DiscardCommentRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.DiscardMomentRequest;
import com.yanxiu.gphone.faceshow.classcircle.response.ClassCircleCancelLikeResponse;
import com.yanxiu.gphone.faceshow.classcircle.response.ClassCircleResponse;
import com.yanxiu.gphone.faceshow.classcircle.response.CommentResponse;
import com.yanxiu.gphone.faceshow.classcircle.response.Comments;
import com.yanxiu.gphone.faceshow.classcircle.response.DiscardMomentResponse;
import com.yanxiu.gphone.faceshow.classcircle.response.LikeResponse;
import com.yanxiu.gphone.faceshow.classcircle.response.RefreshClassCircle;
import com.yanxiu.gphone.faceshow.customview.LoadMoreRecyclerView;
import com.yanxiu.gphone.faceshow.customview.PublicLoadLayout;
import com.yanxiu.gphone.faceshow.customview.SizeChangeCallbackView;
import com.yanxiu.gphone.faceshow.homepage.activity.MainActivity;
import com.yanxiu.gphone.faceshow.login.UserInfo;
import com.yanxiu.gphone.faceshow.permission.OnPermissionCallback;
import com.yanxiu.gphone.faceshow.util.ClassCircleTimeUtils;
import com.yanxiu.gphone.faceshow.util.FileUtil;
import com.yanxiu.gphone.faceshow.util.Logger;
import com.yanxiu.gphone.faceshow.util.ScreenUtils;
import com.yanxiu.gphone.faceshow.util.ToastUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * 首页 “班级圈”Fragment
 */
public class ClassCircleFragment extends FaceShowBaseFragment implements LoadMoreRecyclerView.LoadMoreListener, View.OnClickListener, ClassCircleAdapter.onCommentClickListener, ClassCircleAdapter.onLikeClickListener, SwipeRefreshLayout.OnRefreshListener, TextView.OnEditorActionListener, ClassCircleAdapter.onContentLinesChangedlistener, ClassCircleAdapter.onDeleteClickListener {

    private static final int REQUEST_CODE_ALBUM = 0x000;
    private static final int REQUEST_CODE_CAMERA = 0x001;
    private static final int REQUEST_CODE_CROP = 0x002;
    public boolean firstEnter = true;

    private LoadMoreRecyclerView mClassCircleRecycleView;
    private ClassCircleAdapter mClassCircleAdapter;
    private RelativeLayout mCommentLayout;
    private EditText mCommentView;
    private SizeChangeCallbackView mAdjustPanView;
    private TextView mFunctionView;
    private TextView mTitleView;
    private View mTopView;
    private int mMomentPosition = -1;
    private int mCommentPosition = -1;
    private int mVisibility = View.INVISIBLE;
    private int mHeight;
    private boolean isCommentMaster;
    private String mCameraPath;
    private ClassCircleDialog mClassCircleDialog;
    private SwipeRefreshLayout mRefreshView;
    private PublicLoadLayout rootView;
    private View mDataEmptyView;

    private boolean isCommentLoading = false;

    private UUID mClassCircleRequest;
    private UUID mClassCircleLikeRequest;
    private UUID mClassCircleCancelLikeRequest;
    private UUID mCommentToMasterRequest;
    private UUID mCommentToUserRequest;
    private UUID mDiscardMomentRequest;
    private UUID mDiscardCommentRequest;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = new PublicLoadLayout(getContext());
        rootView.setContentView(R.layout.fragment_classcircle);
        EventBus.getDefault().register(ClassCircleFragment.this);
        initView(rootView);
        listener();
        initData();
        startRequest("0");
        return rootView;
    }


    /**
     * 手动刷新页面，当底部通知tab被点击时调用
     */
    public void toRefresh() {
        mRefreshView.post(new Runnable() {
            @Override
            public void run() {
                mRefreshView.setRefreshing(true);
            }
        });
        startRequest("0");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(ClassCircleFragment.this);
        if (mClassCircleRequest != null) {
            RequestBase.cancelRequestWithUUID(mClassCircleRequest);
            mClassCircleRequest = null;
        }
        if (mClassCircleLikeRequest != null) {
            RequestBase.cancelRequestWithUUID(mClassCircleLikeRequest);
            mClassCircleLikeRequest = null;
        }
        if (mClassCircleCancelLikeRequest != null) {
            RequestBase.cancelRequestWithUUID(mClassCircleCancelLikeRequest);
            mClassCircleCancelLikeRequest = null;
        }
        if (mCommentToMasterRequest != null) {
            RequestBase.cancelRequestWithUUID(mCommentToMasterRequest);
            mCommentToMasterRequest = null;
        }
        if (mCommentToUserRequest != null) {
            RequestBase.cancelRequestWithUUID(mCommentToUserRequest);
            mCommentToUserRequest = null;
        }
        if (mDiscardMomentRequest != null) {
            RequestBase.cancelRequestWithUUID(mDiscardMomentRequest);
            mDiscardMomentRequest = null;
        }
        if (mDiscardCommentRequest != null) {
            RequestBase.cancelRequestWithUUID(mDiscardCommentRequest);
            mDiscardCommentRequest = null;
        }
    }

    private void initView(View rootView) {
        mTopView = rootView.findViewById(R.id.il_title);
        ImageView mBackView = (ImageView) rootView.findViewById(R.id.title_layout_left_img);
        mBackView.setVisibility(View.INVISIBLE);
        mTitleView = (TextView) rootView.findViewById(R.id.title_layout_title);
        mFunctionView = (TextView) rootView.findViewById(R.id.title_layout_right_txt);
        mFunctionView.setText(R.string.publish);
        mFunctionView.setTextColor(ContextCompat.getColor(ClassCircleFragment.this.getContext(), R.color.color_1da1f2));
        mFunctionView.setVisibility(View.VISIBLE);

        mCommentLayout = (RelativeLayout) rootView.findViewById(R.id.ll_edit);
        mCommentView = (EditText) rootView.findViewById(R.id.ed_comment);
        mAdjustPanView = (SizeChangeCallbackView) rootView.findViewById(R.id.sc_adjustpan);
        mClassCircleRecycleView = (LoadMoreRecyclerView) rootView.findViewById(R.id.lm_class_circle);
        mClassCircleRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        mClassCircleAdapter = new ClassCircleAdapter(getContext());
        mClassCircleRecycleView.setAdapter(mClassCircleAdapter);

        mRefreshView = (SwipeRefreshLayout) rootView.findViewById(R.id.srl_refresh);
        mDataEmptyView = rootView.findViewById(R.id.rl_data_empty);
    }

    private void listener() {
        mClassCircleRecycleView.setLoadMoreListener(ClassCircleFragment.this);
        mClassCircleAdapter.setCommentClickListener(ClassCircleFragment.this);
        mClassCircleAdapter.setThumbClickListener(ClassCircleFragment.this);
        mClassCircleAdapter.setContentLinesChangedlistener(ClassCircleFragment.this);
        mClassCircleAdapter.setDeleteClickListener(ClassCircleFragment.this);
        mFunctionView.setOnClickListener(ClassCircleFragment.this);
        mCommentView.setOnEditorActionListener(ClassCircleFragment.this);
        mRefreshView.setOnRefreshListener(ClassCircleFragment.this);
        rootView.setRetryButtonOnclickListener(this);
    }

    private void initData() {
        mTopView.setBackgroundColor(Color.parseColor("#e6ffffff"));
        mTitleView.setText(R.string.classcircle);
        mFunctionView.setBackgroundResource(R.drawable.selector_classcircle_photo);
        mClassCircleRecycleView.getItemAnimator().setChangeDuration(0);
        mClassCircleRecycleView.setLoadMoreEnable(true);
        mRefreshView.setProgressViewOffset(false, ScreenUtils.dpToPxInt(getContext(), 44), ScreenUtils.dpToPxInt(getContext(), 100));
        mRefreshView.post(new Runnable() {
            @Override
            public void run() {
                mRefreshView.setRefreshing(true);
            }
        });
    }

    public void onEventMainThread(RefreshClassCircle refreshClassCircle) {
        onRefresh();
    }

    /**
     * 班级圈
     */
    private void startRequest(final String offset) {
        ClassCircleRequest circleRequest = new ClassCircleRequest();
        circleRequest.offset = offset;
        mClassCircleRequest = circleRequest.startRequest(ClassCircleResponse.class, new HttpCallback<ClassCircleResponse>() {
            @Override
            public void onSuccess(RequestBase request, ClassCircleResponse ret) {
                firstEnter = false;
                mClassCircleRequest = null;
                mRefreshView.post(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshView.setRefreshing(false);
                    }
                });
                if (ret != null && ret.data != null && ret.data.moments != null) {
                    if (offset.equals("0")) {
                        mClassCircleAdapter.setData(ret.data.moments);
                    } else {
                        mClassCircleAdapter.addData(ret.data.moments);
                    }
                    if (ret.data.moments == null || ret.data.moments.size() == 0) {
                        mDataEmptyView.setVisibility(View.VISIBLE);
                    } else {
                        mDataEmptyView.setVisibility(View.GONE);
                    }
                    mClassCircleRecycleView.setLoadMoreEnable(ret.data.hasNextPage);
                } else {
                    if (offset.equals("0")) {
                        rootView.showNetErrorView();
                        mClassCircleAdapter.clear();
                    }
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                firstEnter = false;
                mClassCircleRequest = null;
                mRefreshView.post(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshView.setRefreshing(false);
                    }
                });
                if (offset.equals("0")) {
                    rootView.showNetErrorView();
                    mClassCircleAdapter.clear();
                }
            }
        });
    }

    /**
     * 赞
     */
    private void startLikeRequest(final int position, final ClassCircleResponse.Data.Moments moments) {
        rootView.showLoadingView();
        ClassCircleLikeRequest classCircleLikeRequest = new ClassCircleLikeRequest();
        classCircleLikeRequest.momentId = moments.id;
        mClassCircleLikeRequest = classCircleLikeRequest.startRequest(LikeResponse.class, new HttpCallback<LikeResponse>() {
            @Override
            public void onSuccess(RequestBase request, LikeResponse ret) {
                rootView.hiddenLoadingView();
                mClassCircleLikeRequest = null;
                if (ret != null && ret.data != null) {
                    ClassCircleResponse.Data.Moments.Likes likes = moments.new Likes();
                    likes.clazsId = ret.data.clazsId;
                    likes.createTime = ret.data.createTime;
                    likes.id = ret.data.id;
                    likes.momentId = ret.data.momentId;
                    likes.publisher = ret.data.publisher;
                    moments.likes.add(likes);
                    mClassCircleAdapter.notifyItemChanged(position, ClassCircleAdapter.REFRESH_LIKE_DATA);
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                mClassCircleLikeRequest = null;
                ToastUtil.showToast(getContext(), error.getMessage());
            }
        });
    }

    private void cancelLikeRequest(final int postion, final ClassCircleResponse.Data.Moments moments) {
        rootView.showLoadingView();
        final ClassCircleCancelLikeRequest classCircleCancelLikeRequest = new ClassCircleCancelLikeRequest();
        classCircleCancelLikeRequest.momentId = moments.id;
        mClassCircleCancelLikeRequest = classCircleCancelLikeRequest.startRequest(ClassCircleCancelLikeResponse.class, new HttpCallback<ClassCircleCancelLikeResponse>() {
            @Override
            public void onSuccess(RequestBase request, ClassCircleCancelLikeResponse ret) {
                rootView.hiddenLoadingView();
                mClassCircleLikeRequest = null;
                if (ret != null && ret.getCode() == 0) {
                    for (int i = 0; i < moments.likes.size(); i++) {
                        if (moments.likes.get(i).publisher.userId.equals(String.valueOf(UserInfo.getInstance().getInfo().getUserId()))) {
                            moments.likes.remove(i);
                        }
                    }
                    mClassCircleAdapter.notifyItemChanged(postion, ClassCircleAdapter.REFRESH_LIKE_DATA);
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                mClassCircleLikeRequest = null;
                ToastUtil.showToast(getContext(), error.getMessage());
            }
        });
    }

    /**
     * 评论
     */
    private void startCommentToMasterRequest(final int position, final String content, final ClassCircleResponse.Data.Moments moments) {
        rootView.showLoadingView();
        ClassCircleCommentToMasterRequest masterRequest = new ClassCircleCommentToMasterRequest();
        masterRequest.clazsId = moments.clazsId;
        masterRequest.content = content;
        masterRequest.momentId = moments.id;
        mCommentToMasterRequest = masterRequest.startRequest(CommentResponse.class, new HttpCallback<CommentResponse>() {
            @Override
            public void onSuccess(RequestBase request, CommentResponse ret) {
                rootView.hiddenLoadingView();
                isCommentLoading = false;
                mCommentToMasterRequest = null;
                if (ret != null && ret.data != null) {
                    moments.comments.add(ret.data);
                    mClassCircleAdapter.notifyItemChanged(position, ClassCircleAdapter.REFRESH_COMMENT_DATA);
                    commentFinish();
                    mCommentView.setText("");
                } else {
                    ToastUtil.showToast(getContext(), R.string.error_tip);
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                mCommentToMasterRequest = null;
                isCommentLoading = false;
                ToastUtil.showToast(getContext(), error.getMessage());
                rootView.hiddenLoadingView();
            }
        });
    }

    /**
     * 回复
     */
    private void startCommentToUserRequest(final int position, final String content, final ClassCircleResponse.Data.Moments moments, final Comments comments) {
        rootView.showLoadingView();
        ClassCircleCommentToUserRequest userRequest = new ClassCircleCommentToUserRequest();
        userRequest.clazsId = moments.clazsId;
        userRequest.momentId = moments.id;
        userRequest.content = content;
        userRequest.toUserId = comments.publisher.userId;
        userRequest.commentId = comments.id;
        mCommentToUserRequest = userRequest.startRequest(CommentResponse.class, new HttpCallback<CommentResponse>() {
            @Override
            public void onSuccess(RequestBase request, CommentResponse ret) {
                rootView.hiddenLoadingView();
                isCommentLoading = false;
                mCommentToUserRequest = null;
                if (ret != null && ret.data != null) {
                    moments.comments.add(ret.data);
                    mClassCircleAdapter.notifyItemChanged(position, ClassCircleAdapter.REFRESH_COMMENT_DATA);
                    commentFinish();
                    mCommentView.setText("");
                } else {
                    ToastUtil.showToast(getContext(), R.string.error_tip);
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                isCommentLoading = false;
                mCommentToUserRequest = null;
                ToastUtil.showToast(getContext(), error.getMessage());
            }
        });
    }

    @Override
    public void onRefresh() {
        startRequest("0");
    }

    @Override
    public void onLoadMore(LoadMoreRecyclerView refreshLayout) {
        startRequest(mClassCircleAdapter.getIdFromLastPosition());
    }

    @Override
    public void onLoadmoreComplte() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_layout_right_txt:
                SendClassCircleActivity.LuanchActivity(getContext(), SendClassCircleActivity.TYPE_IMAGE, null);
                break;
            case R.id.retry_button:
                mRefreshView.setRefreshing(true);
                rootView.hiddenNetErrorView();
                onRefresh();
                break;
            default:
                break;
        }
    }


    @Override
    public void commentClick(final int position, ClassCircleResponse.Data.Moments moments, int commentPosition, Comments comment, boolean isCommentMaster) {
        this.isCommentMaster = isCommentMaster;
        this.mCommentPosition = commentPosition;
        this.mMomentPosition = position;
        if (!isCommentMaster) {
            mCommentView.setHint(String.format(getString(R.string.class_circle_comment_to_user), comment.publisher.realName));
        } else {
            mCommentView.setHint(R.string.class_circle_comment_to_master);
        }

        Logger.d("onSizeChanged", "commentClick");
        mCommentLayout.setVisibility(View.VISIBLE);
        mCommentView.setFocusable(true);
        mCommentView.clearFocus();
        mCommentView.requestFocus();
        if (mVisibility == View.VISIBLE) {
            setScroll(position, mHeight, false);
        }
        mAdjustPanView.setViewSizeChangedCallback(new SizeChangeCallbackView.onViewSizeChangedCallback() {
            @Override
            public void sizeChanged(int visibility, int height) {
                Logger.d("onSizeChanged", "visibility  " + visibility);
                mVisibility = visibility;
                if (visibility == View.VISIBLE) {
                    mHeight = height;
                    ((MainActivity) getActivity()).setBottomVisibility(View.GONE);
                    setScroll(position, height, true);
                } else {
                    ((MainActivity) getActivity()).setBottomVisibility(View.VISIBLE);
                }
            }
        });
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mCommentView, 0);
    }

    @Override
    public void commentCancelClick(int pos, List<ClassCircleResponse.Data.Moments> data, int commentPosition, Comments comment) {
        showDiscardCommentPopupWindow(pos, data, commentPosition, comment);
    }


    @Override
    public void commentFinish() {
        mVisibility = View.INVISIBLE;
        mAdjustPanView.setViewSizeChangedCallback(null);
        mCommentLayout.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAdjustPanView.getWindowToken(), 0);
        ((MainActivity) getActivity()).setBottomVisibility(View.VISIBLE);
        mMomentPosition = -1;
        mCommentPosition = -1;
    }

    @Override
    public void onContentLinesChanged(final int position, final boolean isShowAll) {
        mClassCircleRecycleView.post(new Runnable() {
            @Override
            public void run() {
                int visibleStart = ((LinearLayoutManager) mClassCircleRecycleView.getLayoutManager()).findFirstVisibleItemPosition();
                int visibleEnd = ((LinearLayoutManager) mClassCircleRecycleView.getLayoutManager()).findLastVisibleItemPosition();
                if ((position < visibleStart || position > visibleEnd) && !isShowAll) {
                    mClassCircleRecycleView.scrollToPosition(position);
                    mClassCircleRecycleView.post(new Runnable() {
                        @Override
                        public void run() {
                            float diment = getResources().getDimension(R.dimen.top_layout_height);
//                            int length=ScreenUtils.dpToPxInt(getContext(),diment);
                            mClassCircleRecycleView.scrollBy(0, -(int) diment);
                        }
                    });
                }
            }
        });
    }

    /**
     * 将选中item滚动到可见位置
     */
    private void setScroll(final int position, final int height, boolean isShouldScroll) {
        Logger.d("mClassCircleRecycleView", "adapter  position  " + position);
        int visibleStart = ((LinearLayoutManager) mClassCircleRecycleView.getLayoutManager()).findFirstVisibleItemPosition();
        int visibleEnd = ((LinearLayoutManager) mClassCircleRecycleView.getLayoutManager()).findLastVisibleItemPosition();
        if ((mMomentPosition < visibleStart || mMomentPosition > visibleEnd) || isShouldScroll) {
            mClassCircleRecycleView.scrollToPosition(position);
        }
        ClassCircleTimeUtils.creat().start(new ClassCircleTimeUtils.onTimeUplistener() {
            @Override
            public void onTimeUp() {
                setSrcollBy(position, height);
            }
        });
    }

    /**
     * 检查选中item位置进行微调
     */
    private void setSrcollBy(final int position, final int height) {
        int visibleIndex = ((LinearLayoutManager) mClassCircleRecycleView.getLayoutManager()).findFirstVisibleItemPosition();
        int n = position - visibleIndex;
        Logger.d("mClassCircleRecycleView", "visibile position  " + visibleIndex);
        Logger.d("mClassCircleRecycleView", "position  " + n);
        if (0 <= n && n < mClassCircleRecycleView.getChildCount()) {
            int top = mClassCircleRecycleView.getChildAt(n).getTop();
            int bottom = mClassCircleRecycleView.getChildAt(n).getBottom();

            Logger.d("mClassCircleRecycleView", "top " + top);
            Logger.d("mClassCircleRecycleView", "bottom " + bottom);
            Logger.d("mClassCircleRecycleView", "height " + height);

            final int heightMove = bottom - height;
            if (heightMove != 0 && bottom != height) {
                Logger.d("mClassCircleRecycleView", "heightMoves " + heightMove);
                mClassCircleRecycleView.post(new Runnable() {
                    @Override
                    public void run() {
                        mClassCircleRecycleView.scrollBy(0, heightMove);
                    }
                });
            }
        }
        Logger.d("mClassCircleRecycleView", " ");
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            String comment = mCommentView.getText().toString();
            if (!TextUtils.isEmpty(comment) && !isCommentLoading) {
                isCommentLoading = true;
                ClassCircleResponse.Data.Moments moments = mClassCircleAdapter.getDataFromPosition(mMomentPosition);
                if (isCommentMaster) {
                    startCommentToMasterRequest(mMomentPosition, comment, moments);
                } else {
                    startCommentToUserRequest(mMomentPosition, comment, moments, moments.comments.get(mCommentPosition));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void likeClick(int position, ClassCircleResponse.Data.Moments moments) {
        startLikeRequest(position, moments);
    }

    @Override
    public void cancelLikeClick(int position, ClassCircleResponse.Data.Moments moments) {
        cancelLikeRequest(position, moments);
    }

    @Override
    public void delete(int position, List<ClassCircleResponse.Data.Moments> data) {
        discardMoment(position, data);
    }

    /**
     * 删除自己发布的班级圈
     *
     * @param position 所在位置
     * @param data     所有班级圈信息
     */
    private void discardMoment(final int position, final List<ClassCircleResponse.Data.Moments> data) {
        rootView.showLoadingView();
        DiscardMomentRequest discardMomentRequest = new DiscardMomentRequest();
        discardMomentRequest.momentId = data.get(position - 1).id;
        mDiscardMomentRequest = discardMomentRequest.startRequest(DiscardMomentResponse.class, new HttpCallback<DiscardMomentResponse>() {
            @Override
            public void onSuccess(RequestBase request, DiscardMomentResponse ret) {
                rootView.hiddenLoadingView();
                mDiscardMomentRequest = null;
                if (ret != null && ret.getCode() == 0) {
                    data.remove(position - 1);
                    mClassCircleAdapter.notifyItemRemoved(position);
                } else {
                    ToastUtil.showToast(ClassCircleFragment.this.getContext(), ret.getError().getMessage());
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                mDiscardMomentRequest = null;
                ToastUtil.showToast(ClassCircleFragment.this.getContext(), error.getMessage());
            }
        });
    }

    /**
     * 删除自己发布的评论
     *
     * @param pos             当前评论所在的话题列表的位置
     * @param data         当前评论所在的话题列表
     * @param commentPosition 当前评论所在评论列表的位置
     * @param comment         当期评论内容
     */
    private void discardComment(final int pos, final List<ClassCircleResponse.Data.Moments> data, final int commentPosition, final Comments comment) {
        rootView.showLoadingView();
        DiscardCommentRequest discardCommentRequest = new DiscardCommentRequest();
        discardCommentRequest.commentId = data.get(pos-1).comments.get(commentPosition).id;
        mDiscardCommentRequest = discardCommentRequest.startRequest(DiscardMomentResponse.class, new HttpCallback<DiscardMomentResponse>() {
            @Override
            public void onSuccess(RequestBase request, DiscardMomentResponse ret) {
                rootView.hiddenLoadingView();
                mDiscardCommentRequest = null;
                if (ret != null && ret.getCode() == 0) {
                    data.get(pos-1).comments.remove(commentPosition);
                    mClassCircleAdapter.notifyItemChanged(pos, ClassCircleAdapter.REFRESH_COMMENT_DATA);
                    commentFinish();
                } else {
                    ToastUtil.showToast(ClassCircleFragment.this.getContext(), ret.getError().getMessage());
                }

            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                mDiscardCommentRequest = null;
                ToastUtil.showToast(ClassCircleFragment.this.getContext(), error.getMessage());
            }
        });


    }


    private PopupWindow mCancelPopupWindow;

    private void showDiscardCommentPopupWindow(final int pos, final List<ClassCircleResponse.Data.Moments> data, final int commentPosition, final Comments comment) {
        if (mCancelPopupWindow == null) {
            View pop = LayoutInflater.from(this.getContext()).inflate(R.layout.pop_ask_cancel_layout, null);
            (pop.findViewById(R.id.tv_pop_sure)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissPopupWindow();
                    discardComment(pos, data, commentPosition, comment);
                }
            });
            (pop.findViewById(R.id.tv_cancel)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissPopupWindow();
                }
            });
            mCancelPopupWindow = new PopupWindow(pop, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mCancelPopupWindow.setAnimationStyle(R.style.pop_anim);
            mCancelPopupWindow.setFocusable(true);
            mCancelPopupWindow.setBackgroundDrawable(new ColorDrawable(0));
        }
        mCancelPopupWindow.showAtLocation(this.getActivity().getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
    }

    private void dismissPopupWindow() {
        if (mCancelPopupWindow != null) {
            mCancelPopupWindow.dismiss();
        }
    }


}
