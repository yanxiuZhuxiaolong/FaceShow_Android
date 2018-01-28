package com.yanxiu.gphone.faceshow.classcircle.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PersistableBundle;
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
import com.yanxiu.gphone.faceshow.classcircle.ClassCircleFragment;
import com.yanxiu.gphone.faceshow.classcircle.adapter.ClassCircleAdapter;
import com.yanxiu.gphone.faceshow.classcircle.adapter.PublishedMomentAdapter;
import com.yanxiu.gphone.faceshow.classcircle.dialog.ClassCircleDialog;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleCancelLikeRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleCommentToMasterRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleCommentToUserRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleLikeRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.ClassCircleRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.DiscardCommentRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.DiscardMomentRequest;
import com.yanxiu.gphone.faceshow.classcircle.request.GetUserMomentsRequest;
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
import com.yanxiu.gphone.faceshow.util.ClassCircleTimeUtils;
import com.yanxiu.gphone.faceshow.util.Logger;
import com.yanxiu.gphone.faceshow.util.ScreenUtils;
import com.yanxiu.gphone.faceshow.util.ToastUtil;

import java.util.List;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * 我发布的班级圈列表
 *
 * @author frc on 2018/1/17.
 */

public class PublishedMomentListActivity extends FaceShowBaseActivity {
    public boolean firstEnter = true;

    private LoadMoreRecyclerView mClassCircleRecycleView;
    private PublishedMomentAdapter mClassCircleAdapter;
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
    private PopupWindow mDiscardCommentCancelPopupWindow;
    private PopupWindow mDiscardMomentCancelPopupWindow;
    private ImageView mBackView;
    private TextView mTvSureComment;
    private boolean isCommentLoading = false;
    private String mUserId;

    private UUID mClassCircleRequest;
    private UUID mClassCircleLikeRequest;
    private UUID mClassCircleCancelLikeRequest;
    private UUID mCommentToMasterRequest;
    private UUID mCommentToUserRequest;
    private UUID mDiscardMomentRequest;
    private UUID mDiscardCommentRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = new PublicLoadLayout(this);
        rootView.setContentView(R.layout.activity_published_classcircle);
        rootView.setRetryButtonOnclickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRequest("0");
            }
        });
        setContentView(rootView);
        initView(rootView);
        listener();
        initData();
        EventBus.getDefault().register(PublishedMomentListActivity.this);
        startRequest("0");
    }


    private void initData() {
        mTopView.setBackgroundColor(Color.parseColor("#e6ffffff"));
        mTitleView.setText("我发布的");
        mClassCircleRecycleView.getItemAnimator().setChangeDuration(0);
        mClassCircleRecycleView.setLoadMoreEnable(true);
        mRefreshView.setProgressViewOffset(false, ScreenUtils.dpToPxInt(this, 44), ScreenUtils.dpToPxInt(this, 100));
        mRefreshView.post(new Runnable() {
            @Override
            public void run() {
                mRefreshView.setRefreshing(true);
            }
        });
        mUserId = getIntent().getStringExtra("userId");
    }

    private void initView(PublicLoadLayout rootView) {
        mTopView = rootView.findViewById(R.id.il_title);
        mBackView = (ImageView) rootView.findViewById(R.id.title_layout_left_img);
        mBackView.setVisibility(View.VISIBLE);
        mTitleView = (TextView) rootView.findViewById(R.id.title_layout_title);
        mTitleView.setVisibility(View.VISIBLE);

        mTvSureComment = (TextView) rootView.findViewById(R.id.tv_sure);
        mCommentLayout = (RelativeLayout) rootView.findViewById(R.id.ll_edit);
        mCommentView = (EditText) rootView.findViewById(R.id.ed_comment);
        mAdjustPanView = (SizeChangeCallbackView) rootView.findViewById(R.id.sc_adjustpan);
        mClassCircleRecycleView = (LoadMoreRecyclerView) rootView.findViewById(R.id.lm_class_circle);
        mClassCircleRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mClassCircleAdapter = new PublishedMomentAdapter(this);
        mClassCircleRecycleView.setAdapter(mClassCircleAdapter);

        mRefreshView = (SwipeRefreshLayout) rootView.findViewById(R.id.srl_refresh);
        mDataEmptyView = rootView.findViewById(R.id.rl_data_empty);
    }

    private void listener() {
        mBackView.setOnClickListener(mOnClickListener);
        mTvSureComment.setOnClickListener(mOnClickListener);
        mClassCircleRecycleView.setLoadMoreListener(mLoadMoreListener);
        mRefreshView.setOnRefreshListener(mOnRefreshListener);

        mClassCircleAdapter.setCommentClickListener(mOnCommentClickListener);
        mClassCircleAdapter.setThumbClickListener(mOnLikeClickListener);
        mClassCircleAdapter.setContentLinesChangedlistener(mOnContentLinesChangedlistener);
        mClassCircleAdapter.setDeleteClickListener(mOnDeleteClickListener);
    }

    public void onEventMainThread(RefreshClassCircle refreshClassCircle) {
        mOnRefreshListener.onRefresh();
    }

    private PublishedMomentAdapter.onCommentClickListener mOnCommentClickListener = new PublishedMomentAdapter.onCommentClickListener() {
        @Override
        public void commentClick(int position, ClassCircleResponse.Data.Moments response, int commentPosition, Comments comment, boolean sIsCommentMaster) {
            isCommentMaster = sIsCommentMaster;
            mCommentPosition = commentPosition;
            mMomentPosition = position;
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

            InputMethodManager imm = (InputMethodManager) PublishedMomentListActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mCommentView, 0);
        }

        @Override
        public void commentFinish() {
            mVisibility = View.INVISIBLE;
            mAdjustPanView.setViewSizeChangedCallback(null);
            mCommentLayout.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) PublishedMomentListActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mAdjustPanView.getWindowToken(), 0);
            mMomentPosition = -1;
            mCommentPosition = -1;
        }

        @Override
        public void commentCancelClick(int pos, List<ClassCircleResponse.Data.Moments> data, int commentPosition, Comments comment) {
            mMomentPosition = pos;
            mCommentPosition = commentPosition;
            showDiscardCommentPopupWindow(data);
        }
    };
    private PublishedMomentAdapter.onContentLinesChangedlistener mOnContentLinesChangedlistener = new PublishedMomentAdapter.onContentLinesChangedlistener() {
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
                                mClassCircleRecycleView.scrollBy(0, -(int) diment);
                            }
                        });
                    }
                }
            });
        }
    };

    private PublishedMomentAdapter.onDeleteClickListener mOnDeleteClickListener = new PublishedMomentAdapter.onDeleteClickListener() {
        @Override
        public void delete(int position, List<ClassCircleResponse.Data.Moments> data) {
            mMomentPosition = position;
            showDiscardMomentPopupWindow(data);
        }
    };

    private PublishedMomentAdapter.onLikeClickListener mOnLikeClickListener = new PublishedMomentAdapter.onLikeClickListener() {
        @Override
        public void likeClick(int position, ClassCircleResponse.Data.Moments response) {
            startLikeRequest(position, response);
        }

        @Override
        public void cancelLikeClick(int position, ClassCircleResponse.Data.Moments response) {
            cancelLikeRequest(position, response);
        }
    };


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view == mBackView) {
                onBackPressed();
            } else if (view == rootView.getReturyButton()) {
                mRefreshView.setRefreshing(true);
                rootView.hiddenNetErrorView();
                mOnRefreshListener.onRefresh();
            } else if (view == mTvSureComment) {
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
            }
        }
    };

    private LoadMoreRecyclerView.LoadMoreListener mLoadMoreListener = new LoadMoreRecyclerView.LoadMoreListener() {
        @Override
        public void onLoadMore(LoadMoreRecyclerView refreshLayout) {
            startRequest(mClassCircleAdapter.getIdFromLastPosition());
        }

        @Override
        public void onLoadmoreComplte() {

        }
    };

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            startRequest("0");
        }
    };


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
        if (0 <= n && n < mClassCircleRecycleView.getChildCount()) {
            int top = mClassCircleRecycleView.getChildAt(n).getTop();
            int bottom = mClassCircleRecycleView.getChildAt(n).getBottom();

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
    }

    /**
     * 班级圈
     */
    private void startRequest(final String offset) {
        GetUserMomentsRequest circleRequest = new GetUserMomentsRequest();
        circleRequest.offset = offset;
        circleRequest.userId = mUserId;
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
                ToastUtil.showToast(PublishedMomentListActivity.this, error.getMessage());
            }
        });
    }

    /**
     * 取消赞
     *
     * @param postion
     * @param moments
     */
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
                ToastUtil.showToast(PublishedMomentListActivity.this, error.getMessage());
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
                    mOnCommentClickListener.commentFinish();
                    mCommentView.setText("");
                } else {
                    ToastUtil.showToast(PublishedMomentListActivity.this, R.string.error_tip);
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                mCommentToMasterRequest = null;
                isCommentLoading = false;
                ToastUtil.showToast(PublishedMomentListActivity.this, error.getMessage());
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
                    mOnCommentClickListener.commentFinish();
                    mCommentView.setText("");
                } else {
                    ToastUtil.showToast(PublishedMomentListActivity.this, R.string.error_tip);
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                isCommentLoading = false;
                mCommentToUserRequest = null;
                ToastUtil.showToast(PublishedMomentListActivity.this, error.getMessage());
            }
        });
    }

    /**
     * 删除自己发布的班级圈
     *
     * @param data 所有班级圈信息
     */
    private void discardMoment(final List<ClassCircleResponse.Data.Moments> data) {
        rootView.showLoadingView();
        DiscardMomentRequest discardMomentRequest = new DiscardMomentRequest();
        discardMomentRequest.momentId = data.get(mMomentPosition).id;
        mDiscardMomentRequest = discardMomentRequest.startRequest(DiscardMomentResponse.class, new HttpCallback<DiscardMomentResponse>() {
            @Override
            public void onSuccess(RequestBase request, DiscardMomentResponse ret) {
                rootView.hiddenLoadingView();
                mDiscardMomentRequest = null;
                if (ret != null && ret.getCode() == 0) {
                    data.remove(mMomentPosition);
                    if (data.size() >0) {
                        mDataEmptyView.setVisibility(View.GONE);
                    } else {
                        mDataEmptyView.setVisibility(View.VISIBLE);
                    }
                    mClassCircleAdapter.notifyItemRemoved(mMomentPosition);
                } else {
                    ToastUtil.showToast(PublishedMomentListActivity.this, ret.getError().getMessage());
                }
            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                mDiscardMomentRequest = null;
                ToastUtil.showToast(PublishedMomentListActivity.this, error.getMessage());
            }
        });
    }

    /**
     * 删除自己发布的评论
     *
     * @param data 当前评论所在的话题列表
     */
    private void discardComment(final List<ClassCircleResponse.Data.Moments> data) {
        rootView.showLoadingView();
        DiscardCommentRequest discardCommentRequest = new DiscardCommentRequest();
        discardCommentRequest.commentId = data.get(mMomentPosition).comments.get(mCommentPosition).id;
        mDiscardCommentRequest = discardCommentRequest.startRequest(DiscardMomentResponse.class, new HttpCallback<DiscardMomentResponse>() {
            @Override
            public void onSuccess(RequestBase request, DiscardMomentResponse ret) {
                rootView.hiddenLoadingView();
                mDiscardCommentRequest = null;
                if (ret != null && ret.getCode() == 0) {
                    data.get(mMomentPosition).comments.remove(mCommentPosition);
                    mClassCircleAdapter.notifyItemChanged(mMomentPosition, ClassCircleAdapter.REFRESH_COMMENT_DATA);
                    mOnCommentClickListener.commentFinish();
                } else {
                    ToastUtil.showToast(PublishedMomentListActivity.this, ret.getError().getMessage());
                }

            }

            @Override
            public void onFail(RequestBase request, Error error) {
                rootView.hiddenLoadingView();
                mDiscardCommentRequest = null;
                ToastUtil.showToast(PublishedMomentListActivity.this, error.getMessage());
            }
        });


    }


    private void showDiscardCommentPopupWindow(final List<ClassCircleResponse.Data.Moments> data) {
        mClassCircleAdapter.notifyItemChanged(mMomentPosition, ClassCircleAdapter.REFRESH_ANIM_VIEW);
        if (mDiscardCommentCancelPopupWindow == null) {
            View pop = LayoutInflater.from(this).inflate(R.layout.pop_ask_cancel_layout, null);
            TextView tvDel = (TextView) pop.findViewById(R.id.tv_pop_sure);
            tvDel.setText(R.string.class_circle_delete);
            tvDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissPopupWindow();
                    discardComment(data);
                }
            });
            (pop.findViewById(R.id.tv_cancel)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissPopupWindow();
                }
            });
            mDiscardCommentCancelPopupWindow = new PopupWindow(pop, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mDiscardCommentCancelPopupWindow.setAnimationStyle(R.style.pop_anim);
            mDiscardCommentCancelPopupWindow.setFocusable(true);
            mDiscardCommentCancelPopupWindow.setBackgroundDrawable(new ColorDrawable(0));
        }
        mDiscardCommentCancelPopupWindow.showAtLocation(this.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
    }

    private void showDiscardMomentPopupWindow(final List<ClassCircleResponse.Data.Moments> data) {
        mClassCircleAdapter.notifyItemChanged(mMomentPosition, ClassCircleAdapter.REFRESH_ANIM_VIEW);
        if (mDiscardMomentCancelPopupWindow == null) {
            View pop = LayoutInflater.from(this).inflate(R.layout.pop_ask_cancel_layout, null);
            TextView tvDel = (TextView) pop.findViewById(R.id.tv_pop_sure);
            tvDel.setText(R.string.class_circle_delete);
            tvDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissPopupWindow();
                    discardMoment(data);
                }
            });
            (pop.findViewById(R.id.tv_cancel)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissPopupWindow();
                }
            });
            mDiscardMomentCancelPopupWindow = new PopupWindow(pop, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mDiscardMomentCancelPopupWindow.setAnimationStyle(R.style.pop_anim);
            mDiscardMomentCancelPopupWindow.setFocusable(true);
            mDiscardMomentCancelPopupWindow.setBackgroundDrawable(new ColorDrawable(0));
        }
        mDiscardMomentCancelPopupWindow.showAtLocation(this.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
    }

    private void dismissPopupWindow() {
        if (mDiscardCommentCancelPopupWindow != null) {
            mDiscardCommentCancelPopupWindow.dismiss();
        }
        if (mDiscardMomentCancelPopupWindow != null) {
            mDiscardMomentCancelPopupWindow.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(PublishedMomentListActivity.this);
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

    @Override
    public void onBackPressed() {
        this.setResult(RESULT_OK);
        this.finish();
        super.onBackPressed();
    }
}
