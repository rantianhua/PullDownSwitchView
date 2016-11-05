package com.example.rth.pulldownswitchview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.example.rth.pulldownswitchview.R;

/**
 * Created by rth on 16/11/5.
 */
public class NormalView extends RelativeLayout {


    public NormalView(Context context) {
        super(context);
        init(context);
    }

    public NormalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        inflate(context, R.layout.normal_view,this);
    }

}
