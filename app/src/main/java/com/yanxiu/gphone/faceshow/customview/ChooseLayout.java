package com.yanxiu.gphone.faceshow.customview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yanxiu.gphone.faceshow.R;
import com.yanxiu.gphone.faceshow.course.bean.VoteInfoBean;
import com.yanxiu.gphone.faceshow.course.bean.VoteItemBean;
import com.yanxiu.gphone.faceshow.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import static com.yanxiu.gphone.faceshow.course.bean.VoteBean.TYPE_MULTI;
import static com.yanxiu.gphone.faceshow.course.bean.VoteBean.TYPE_SINGLE;

/**
 * 选择控件
 * dyf
 */
public class ChooseLayout extends LinearLayout implements View.OnClickListener {

    private Context mContext;
    private onItemClickListener mOnItemClickListener;
    private int mChooseType = TYPE_SINGLE;
    private boolean mIsClick = true;

    private VoteInfoBean mData;
    private ArrayList<String> mAnswerList;//保存选项结果

    private final String[] mEms = new String[]{" A.", " B.", " C.", " D.", " E.", " F.", " G.", " H.", " I.", " J.", " K.", " L.", " M.", " N."};


    public interface onItemClickListener {
        void onChooseItemClick(int position, boolean isSelected);
    }

    public ChooseLayout(Context context) {
        super(context);
        init(context);
    }

    public ChooseLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChooseLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        if (isInEditMode()) {
            return;
        }
        this.setOrientation(LinearLayout.VERTICAL);
    }

    public void setData(VoteInfoBean data) {
        mData = data;
        addChildView(data);
    }

    /**
     * 选择结果，保存到这个list里（该list，就是bean里的list）
     *
     * @param list
     */
    public void setSaveChooceResultList(ArrayList<String> list) {
        mAnswerList = list;
        for (int i = 0; i < mAnswerList.size(); i++) {
            //adapter复用时，恢复数据
            int position = Integer.parseInt(mAnswerList.get(i));
            setSelect(position);
        }
    }


    private void addChildView(final VoteInfoBean data) {
        this.removeAllViews();
        ArrayList<VoteItemBean> list = data.getVoteItems();
        for (int i = 0; i < list.size(); i++) {
            VoteItemBean bean = list.get(i);
            View view = LayoutInflater.from(mContext).inflate(R.layout.layout_choose_item, this, false);
            final ViewHolder holder = new ViewHolder();
            holder.position = i;
            holder.mQuestionContentView = (TextView) view.findViewById(R.id.tv_question_content);
            holder.mQuestionContentView.setText(mEms[i] + bean.getItemName());
            holder.mQuestionSelectView = view.findViewById(R.id.v_question_select);
            if (this.mIsClick) { //选项界面
                if (mChooseType == TYPE_MULTI) {
                    ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.multi_unselect));
                } else {
                    ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.single_unselect));
                }
            } else {
                //查看结果界面
                holder.mQuestionSelectView.setVisibility(GONE);
            }

            view.setOnClickListener(ChooseLayout.this);
            view.setTag(holder);
            this.addView(view);
        }
    }

    public class ViewHolder {
        public int position;
        public boolean mSelect = false;
        public TextView mQuestionContentView;
        public View mQuestionSelectView;
    }


    public void setIsClick(boolean isClick) {
        this.mIsClick = isClick;
        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = this.getChildAt(i);
            if (isClick) {
                view.setEnabled(true);
            } else {
                view.setEnabled(false);
            }
        }
    }

    public void setChooseType(int type) {
        this.mChooseType = type;

        if (mChooseType == TYPE_MULTI) {
            setMultiSelectBg();
        }
    }

    private void setMultiSelectBg() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            ViewHolder holder = (ViewHolder) getChildAt(i).getTag();
            ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.multi_unselect));
        }
    }

    public void setSelectItemListener(onItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setSelect(int position) {
        setSelect(position, false);
    }

    private void setSelect(int position, boolean isCallBack) {
        int count = this.getChildCount();
        if (position >= count) {
            return;
        }
        for (int i = 0; i < count; i++) {
            View chileView = this.getChildAt(i);
            ViewHolder holder = (ViewHolder) chileView.getTag();
            if (i == position) {
                if (holder.mSelect) {
                    setItemUnSelect(holder);
                    if (isCallBack) {
                        if (mChooseType == TYPE_SINGLE) {
                            mAnswerList.clear();
                        }
                        if (mAnswerList.contains(String.valueOf(position))) { //已有，删除
                            mAnswerList.remove(String.valueOf(position));
                        }
                        onClick(i, false);
                    }
                } else {
                    if (mChooseType == TYPE_SINGLE) {
                        setItemSelect(holder);
                        if (isCallBack) {
                            if (mChooseType == TYPE_SINGLE) {
                                mAnswerList.clear();
                            }
                            //没有，添加
                            if (!mAnswerList.contains(String.valueOf(position))) {
                                mAnswerList.add(String.valueOf(position));
                            }
                            onClick(i, true);
                        }
                    } else {
                        if (mData.getMaxSelectNum() >= mAnswerList.size() + 1) {
                            setItemSelect(holder);
                            if (isCallBack) {
                                //没有，添加
                                if (!mAnswerList.contains(String.valueOf(position))) {
                                    mAnswerList.add(String.valueOf(position));
                                }
                                onClick(i, true);
                            }
                        } else {
                            if (isCallBack) {
                                ToastUtil.showToast(getContext(), "本题最多只能选择" + mData.getMaxSelectNum() + "项（如需修改先取消之前的选择)");
                            }
                        }
                    }

                }
            } else {
                if (mChooseType == TYPE_SINGLE) {
                    setItemUnSelect(holder);
                }
            }
        }
    }

    private void setItemUnSelect(ViewHolder holder) {
        holder.mSelect = false;
        if (mChooseType == TYPE_MULTI) {
            ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.multi_unselect));
        } else {
            ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.single_unselect));
        }
        holder.mQuestionContentView.setTextColor(getResources().getColor(R.color.color_666666));
    }

    private void setItemSelect(ViewHolder holder) {
        holder.mSelect = true;
        if (mChooseType == TYPE_MULTI) {
            ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.multi_select));
        } else {
            ViewCompat.setBackground(holder.mQuestionSelectView, ContextCompat.getDrawable(mContext, R.drawable.single_select));
        }
        holder.mQuestionContentView.setTextColor(getResources().getColor(R.color.color_1da1f2));
    }

    private void onClick(int position, boolean isSelected) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onChooseItemClick(position, isSelected);
        }
    }

    @Override
    public void onClick(View v) {
        if (mIsClick) {
            ViewHolder holder = (ViewHolder) v.getTag();
            setSelect(holder.position, true);
        }
    }
}
