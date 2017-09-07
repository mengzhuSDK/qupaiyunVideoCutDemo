/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.effects.control;

import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;

public class TabGroup implements OnClickListener {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(TabGroup control, int checkedIndex);
    }

    private OnCheckedChangeListener mOnCheckedChangeistener;
    private OnTabChangeListener mOnTabChangeListener;

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeistener = listener;
    }

    public void setOnTabChangeListener(OnTabChangeListener listener) {
        mOnTabChangeListener = listener;
    }

    private final ArrayList<View> _ViewList = new ArrayList<>();

    public void addView(View view) {
        view.setOnClickListener(this);
        _ViewList.add(view);
    }

    private int _CheckedIndex = -1;

    public int getCheckedIndex() {
        return _CheckedIndex;
    }

    public void setCheckedView(View item) {
        setCheckedIndex(_ViewList.indexOf(item));
    }

    public void setCheckedIndex(int index) {
        if (_CheckedIndex >= 0) {
            _ViewList.get(_CheckedIndex).setActivated(false);
        }
        _CheckedIndex = index;
        if (_CheckedIndex >= 0) {
            _ViewList.get(_CheckedIndex).setActivated(true);
        }

        if (mOnCheckedChangeistener != null) {
            mOnCheckedChangeistener.onCheckedChanged(this, _CheckedIndex);
        }
    }

    @Override
    public void onClick(View v) {
        setCheckedView(v);
        if(mOnTabChangeListener != null) {
            mOnTabChangeListener.onTabChange();
        }
    }

}
