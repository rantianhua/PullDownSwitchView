package com.example.rth.pulldownswitchview.view.interfaces;

import android.view.View;

/**
 * Created by rth on 16/11/5.
 */
public interface PullDownSwitchListener {

    /**
     * 返回当前选中的View
     * @param view 显示的View
     */
    void onViewSelected(View view);

    /**
     * 下拉过程中的状态监听
     * @param state 当前的状态
     */
    void onSwitchState(int state);

    /**
     * 拖动过程中拖动的距离变化信息
     * @param incrementVertical 下拉/上拉的距离
     * @param onePageHeight 一页的高度(即可以下拉的最大高度)
     */
    void onPullDownChanged(int incrementVertical, int onePageHeight);
}
