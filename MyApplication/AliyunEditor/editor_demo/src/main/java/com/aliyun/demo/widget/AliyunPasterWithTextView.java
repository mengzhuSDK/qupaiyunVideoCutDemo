/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.aliyun.demo.editor.R;

public class AliyunPasterWithTextView extends AliyunPasterView {

	private boolean isEditCompleted;
	private boolean isCouldShowLabel;

	public AliyunPasterWithTextView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public AliyunPasterWithTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}

	public AliyunPasterWithTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public boolean isEditCompleted() {
		return isEditCompleted;
	}

	public void setEditCompleted(boolean isEditCompleted) {
		int width = getContentWidth();
		int height = getContentHeight();
		this.isEditCompleted = isEditCompleted;
		if(width == 0 || height == 0){
			return;
		}
		if(isEditCompleted){
			content_width = width;
			content_height = height;

			MATRIX_UTIL.decomposeTSR(_Transform);
			float scaleX = 1 / MATRIX_UTIL.scaleX;
			float scaleY = 1 / MATRIX_UTIL.scaleY;
			_Transform.postScale(scaleX, scaleY);

            if(width > getWidth()){
                float scale = (float)getWidth() / (float)width;
				scale = scale == 0 ? 1 : scale;
                _Transform.postScale(scale, scale);
            }

			requestLayout();
		}
		Log.d("EDIT", "EditCompleted : " + isEditCompleted + "content_width : " + content_width
	            + " content_height : " + content_height);
	}

	@Override protected
    void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		validateTransform();
		MATRIX_UTIL.decomposeTSR(_Transform);
		if(isEditCompleted){
			int width = (int) (MATRIX_UTIL.scaleX * content_width);
			int height = (int) (MATRIX_UTIL.scaleY * content_height);

			Log.d("EDIT", "Measure width : " + width + "scaleX : " + MATRIX_UTIL.scaleX +
			        "content_width : " + content_width
	                + " content_height : " + content_height);
			int w = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
			int h = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			measureChildren(w, h);
		}else{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	        measureChildren(widthMeasureSpec, heightMeasureSpec);
		}

    }

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if(text_label == null){
			isCouldShowLabel = false;
			return ;
		}

		MATRIX_UTIL.decomposeTSR(_Transform);

		if(MATRIX_UTIL.getRotationDeg() == 0){
			isCouldShowLabel = true;
			float[] center = getCenter();
			float cy = center[1];

			float hh = MATRIX_UTIL.scaleY * getContentHeight() / 2;

			text_label.layout(0, (int)(cy - hh), getWidth(), (int)(cy + hh));
		}else{
			text_label.layout(0, 0, 0, 0);
			isCouldShowLabel = false;
		}
	}

	private View _ContentView;
	private View text_label;

	private int content_width;
	private int content_height;

    public void setContentWidth(int content_width) {
		this.content_width = content_width;
	}

	public void setContentHeight(int content_height) {
		this.content_height = content_height;
	}

	@Override
	public boolean isCouldShowLabel() {
		return isCouldShowLabel;
	}

	@Override protected
    void onFinishInflate() {
        super.onFinishInflate();

        _ContentView = findViewById(android.R.id.content);
		text_label = findViewById(R.id.qupai_bg_overlay_text_label);
    }

	@Override
	public void setShowTextLabel(boolean isShow) {
		if(text_label != null){
			text_label.setVisibility(isShow ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public View getTextLabel() {
		return text_label;
	}

	@Override
	public int getContentWidth() {
		if(isEditCompleted){
			return content_width;
		}
		return _ContentView.getMeasuredWidth();
	}

	@Override
	public int getContentHeight() {
		if(isEditCompleted){
			return content_height;
		}
		return _ContentView.getMeasuredHeight();
	}

	@Override
	public View getContentView() {
		// TODO Auto-generated method stub
		return _ContentView;
	}

}
