package com.example.rth.pulldownswitchview.view;

/**
 * Created by rth on 16/11/5.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.example.rth.pulldownswitchview.view.interfaces.PullDownSwitchListener;

import java.util.ArrayList;
import java.util.List;


public class PullDownSwitchView extends ViewGroup {

    private float preY;
    private boolean isDragging = false;
    private List<View> childrens = new ArrayList<>();
    //当前选中展示的View
    private View showingView;
    //自动滚动的时间
    private int autoScrollDuration = 500;
    //决定在移动的过程中是否拦截Touch事件
    private static int TOUCH_SLOP = 3;
    //第一次按下的y位置
    private float startY;
    //垂直方向上布局的偏移量
    private int incrementVertical = 0;
    //展示一页View时的高度
    private int onePageHeight;
    //可以切换View的有效滑动距离相对于 PullDownSwitchView 高度的比例
    private float validScrollHeightFactor = 1/3f;
    //有效滑动距离
    private int validDistance;
    //处理自动滚动
    //关于PullDownSwitchView的监听
    private PullDownSwitchListener pullDownSwitchListener;
    //第一个按下的手指的id
    private int pointerId;
    private boolean isScrolling = false;

    public PullDownSwitchView(Context context) {
        super(context);
        init(context);
    }

    public PullDownSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullDownSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        TOUCH_SLOP = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onLayout(boolean c, int l, int t, int r, int b) {
        for (int i = 0; i < childrens.size(); i++) {
            View view = childrens.get(i);
            if (view.getVisibility() == GONE) continue;
            boolean isShowingView = childrens.get(i) == showingView;
            int top = (b - t) * (i - 1) / 2 + (isShowingView ? 0 : incrementVertical);
            int bottom = (b - t) * i / 2 + (isShowingView ? 0 : incrementVertical);
            view.layout(l,
                    top,
                    r,
                    bottom);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        onePageHeight = height;
        validDistance = (int) (height * validScrollHeightFactor);
        measureChildren(widthMeasureSpec,heightMeasureSpec);
        setMeasuredDimension(width, height * childrens.size());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEventCompat.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            // 该事件可能不是我们的
            return false;
        }
        boolean isIntercept = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isIntercept = isScrolling;
                startY = event.getY();
                pointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float slop = Math.abs(startY - event.getY());
                if(slop >= TOUCH_SLOP) {
                    isIntercept = true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int leaveIndex = event.getActionIndex();
                int leaveId = event.getPointerId(leaveIndex);
                if(leaveId == pointerId) {
                    int reIndex = leaveIndex == 0 ? 1 : 0;
                    pointerId = event.getPointerId(reIndex);
                    startY = event.getY(reIndex);
                }
                break;
        }
        if(isIntercept) {
            if(getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        return isIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float curY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(!isScrolling) {
                    preY = curY;
                    startY = event.getY();
                }else {
                    //此时不应该响应任何事件
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isScrolling) return false;
                final float interval = curY - preY;
                preY = curY;
                incrementVertical = (int) (curY - startY);
                if(incrementVertical < 0) {
                    //不支持在原位置的基础上上拉
                    incrementVertical = 0;
                }
                if (interval <= 0) {
                    if (!isDragging) {
                        //不支持直接上滑操作,除非在已经 处于drag中
                        incrementVertical = 0;
                        return false;
                    }
                }else {
                    if (!isDragging) {
                        isDragging = true;
                        childrens.get(0).bringToFront();
                    }
                }
                if(pullDownSwitchListener != null) {
                    pullDownSwitchListener.onPullDownChanged(incrementVertical,onePageHeight);
                }
                requestLayout();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                int restScrollDistance = 0;
                if (incrementVertical < 0 ) incrementVertical = 0;
                //Log.d("PullDownSwitchView","incrementVertical is " + incrementVertical + " validDistance is " + validDistance );
                if(incrementVertical >= validDistance && incrementVertical < onePageHeight) {
                    //没有下拉一整屏,但是已经达到最小切换距离,自动切换下一个View
                    restScrollDistance = onePageHeight - incrementVertical;
                    if(pullDownSwitchListener != null) {
                        pullDownSwitchListener.onSwitchState(SwitchState.STATE_PULL_DOWN);
                    }
                    startAnimator(incrementVertical, onePageHeight, true, autoScrollDuration);
                }else if(incrementVertical > 0 && incrementVertical < validDistance) {
                    //没有达到最小切换距离,退回到一开始的位置
                    if(pullDownSwitchListener != null) {
                        pullDownSwitchListener.onSwitchState(SwitchState.STATE_PULL_BACK);
                    }
                    startAnimator(incrementVertical, 0, false, autoScrollDuration);
                }else if (incrementVertical == onePageHeight){
                    //移动了一整屏,View已经切换了
                    switchViewLayout(true);
                }else {
                    switchViewLayout(false);
                }
                break;
        }
        return true;
    }

    private void switchViewLayout(boolean changeView) {
        incrementVertical = 0;
        if(changeView) {
            childrens.add(0,childrens.remove(childrens.size()-1));
            requestLayout();
            showingView = childrens.get(childrens.size() - 1);
            if (pullDownSwitchListener != null) pullDownSwitchListener.onViewSelected(showingView);
        }
        if (pullDownSwitchListener != null) pullDownSwitchListener.onSwitchState(SwitchState.STATE_IDLE);
    }

    public void addAFunctionView(View v) {
        addView(v);
        childrens.add(v);
        showingView = v;
    }

    public void setAutoScrollDuration(int duration) {
        this.autoScrollDuration = duration;
    }

    public void setValidScrollHeightFactor(float validScrollHeightFactor) {
        this.validScrollHeightFactor = validScrollHeightFactor;
    }

    public void setPullDownSwitchListener(PullDownSwitchListener listener) {
        this.pullDownSwitchListener = listener;
    }

    /**
     * @param start 开始的偏移量
     * @param end 最终的偏移量
     * @param changeView 是否切换了View
     * @param duration 动画时间
     */
    private void startAnimator(int start, int end, final boolean changeView, int duration) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(start,end);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                incrementVertical = (int) valueAnimator.getAnimatedValue();
                if(pullDownSwitchListener != null) {
                    pullDownSwitchListener.onPullDownChanged(incrementVertical,onePageHeight);
                }
                requestLayout();
            }
        });
        valueAnimator.setDuration(duration);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                switchViewLayout(changeView);
                isScrolling = false;
            }
        });
        isScrolling = true;
        valueAnimator.start();
    }

    /**
     * 指定当前要显示的View
     * @param v 要显示的View
     * @param duration 滚动时间
     * @return true 开始切换新的View
     */
    public boolean showView(View v,int duration) {
        if (v == null) {
            throw new IllegalArgumentException("view cannot be null");
        }
        if(v == showingView) return false;
        if (isDragging) return false;
        if (isScrolling) return false;
        if(v.getVisibility() != VISIBLE) v.setVisibility(VISIBLE);
        v.bringToFront();
        startAnimator(0,onePageHeight, true, duration);
        return true;
    }

    public boolean showView(View v) {
        return showView(v, 600);
    }

    public interface SwitchState {
        int STATE_IDLE = 0;   //停下来的状态
        int STATE_PULL_DOWN = 1;   //自动滚动到下一个View
        int STATE_PULL_BACK = 2;   //自动滚到初始位置,View没有变化
    }

}
