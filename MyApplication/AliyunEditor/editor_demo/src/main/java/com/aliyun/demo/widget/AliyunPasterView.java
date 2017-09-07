/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import com.aliyun.demo.editor.R;
import com.aliyun.demo.util.MatrixUtil;

public abstract class AliyunPasterView extends ViewGroup {

    public interface OnChangeListener {

        void onChange(AliyunPasterView overlay);

    }

	/**
	 * normalized 2d coordinates: x y: (0, 0) right bottom: (1, 1) object
	 * coordinates: (0, 0) (1, 1)
	 */
	public static final int MODE_NORMALIZED = 1;

	/**
	 * device coordinates: x y: (- CanvasWidth / 2, - CanvasHeight / 2)
	 * right bottom: (+ CanvasWidth / 2, + CanvasHeight / 2) object coordinates:
	 * (- ContentWidth / 2, - ContentHeight / 2) (+ ContentWidth / 2, +
	 * ContentHeight / 2)
	 */
	public static final int MODE_DEVICE = 2;

	/**
	 * viewport coordinates: x y: (- ViewportWidth / 2, - ViewportHeight /
	 * 2) right bottom: (+ ViewportWidth / 2, + ViewportWidth / 2) object
	 * coordinates: (- ContentWidth / 2, - ContentHeight / 2) (+ ContentWidth /
	 * 2, + ContentHeight / 2)
	 */
	public static final int MODE_VIEWPORT = 3;

	public static class LayoutParams extends MarginLayoutParams {

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);

			TypedArray ta = c.obtainStyledAttributes(attrs,
					R.styleable.EditOverlay_Layout);

			gravity = ta.getInteger(
					R.styleable.EditOverlay_Layout_android_layout_gravity,
					Gravity.LEFT | Gravity.TOP);

			ta.recycle();
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public int gravity;
	}

	private static final String TAG = "EditOverlay";

	public AliyunPasterView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		post(new Runnable() {
			@Override
			public void run() {
				ViewGroup parent = (ViewGroup) getParent();
				width = parent.getWidth();
				height = parent.getHeight();
			}
		});
	}

	public AliyunPasterView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AliyunPasterView(Context context) {
		this(context, null);
	}

	private int _ViewportWidth = 640;
	private int _ViewportHeight = 640;

	private int width;
	private int height;

	private boolean isMirror;

	public int getLayoutWidth(){
		return width;
	}

	public int getLayoutHeight(){
		return height;
	}

	public void setViewport(int w, int h) {
		_ViewportWidth = w;
		_ViewportHeight = h;
	}

	public boolean isMirror() {
		return isMirror;
	}

	public void setMirror(boolean mirror) {
		isMirror = mirror;
	}

	private OnChangeListener _Listener;

	public void setOnChangeListener(OnChangeListener listener) {
		_Listener = listener;
	}

	public float[] getScale(){
		float[] scale = new float[2];
		scale[0] = MATRIX_UTIL.scaleX;
		scale[1] = MATRIX_UTIL.scaleY;
		return scale;
	}

	public float getRotation(){
		return MATRIX_UTIL.getRotation();
	}

	public abstract int getContentWidth();

    public abstract int getContentHeight();

	public abstract void setContentWidth(int width);

	public abstract void setContentHeight(int height);

    public abstract View getContentView();

	public boolean isCouldShowLabel(){
		return false;
	}

	public void setShowTextLabel(boolean isShow){

	}

	public View getTextLabel(){
		return null;
	}

	public void setEditCompleted(boolean isEditCompleted){

	}

	protected void validateTransform() {
		Log.d("TRANSFORM", "before validateTransform : " + _Transform.toString() + "mode : " + _TransformMode);
		if(getContentWidth() == 0 ||  getContentHeight() == 0){
			return;
		}
		switch (_TransformMode) {
		case MODE_DEVICE:
			return;
		case MODE_NORMALIZED:
			_Transform.preTranslate(0.5f, 0.5f);
			Log.d("VALIDATE", "content_width : " + getContentWidth() + " content_height : " + getContentHeight());
			_Transform.preScale(1.0f / getContentWidth(), 1.0f / getContentHeight());
			_Transform.postTranslate(-0.5f, -0.5f);
			//Log.d("VALIDATE", "getWidth : " + getWidth() + " getHeight : " + getHeight());

			_Transform.postScale(getWidth(), getHeight());
			break;
		case MODE_VIEWPORT:
			_Transform.postScale((float) getWidth() / _ViewportWidth,
					(float) getHeight() / _ViewportHeight);
			break;
		}

		Log.d("TRANSFORM", "after validateTransform : " + _Transform.toString() + "mode : " + _TransformMode);

		_TransformMode = MODE_DEVICE;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		validateTransform();

		onLayoutContent();

		float hw, hh;
		int base_x;
		int base_y;

		hw = getContentWidth() / 2;
		hh = getContentHeight() / 2;

		// lt, rt, lb, rb
		POINT_LIST[0] = -hw;
		POINT_LIST[1] = -hh;
		POINT_LIST[2] = hw;
		POINT_LIST[3] = -hh;
		POINT_LIST[4] = -hw;
		POINT_LIST[5] = hh;
		POINT_LIST[6] = hw;
		POINT_LIST[7] = hh;
		base_x = getWidth() / 2;
		base_y = getHeight() / 2;

		Matrix together = converge();
		together.mapPoints(POINT_LIST);

		for (int i = 0, count = getChildCount(); i < count; i++) {
			View child = getChildAt(i);

			if (child == getContentView()) {
				continue;
			}

			int gravity = ((LayoutParams) child.getLayoutParams()).gravity;

			int ix = getPointFromMatrix(gravity);

			int center_x = (int) POINT_LIST[ix];
			int center_y = (int) POINT_LIST[ix + 1];

			int left = base_x + center_x;
			int top = base_y + center_y;
			int right = base_x + center_x;
			int bottom = base_y + center_y;

			int half_w = child.getMeasuredWidth() / 2;
			int half_h = child.getMeasuredHeight() / 2;
			int childLeft = left - half_w;
			int childTop = top - half_h;
			int childRight = right + half_w;
			int childBottom = bottom + half_h;

			Log.d("DIY_FRAME", "child_left : " + childLeft + " child_top : " + childTop +
					" child_right : " + childRight + " child_bottom : " + childBottom);

			child.layout(childLeft, childTop, childRight, childBottom);
		}

	}

	private int getPointFromMatrix(int gravity){
		int x = 0, y = 0;
		switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
			case Gravity.TOP:
				y = 0;
				break;
			case Gravity.BOTTOM:
				y = 1;
				break;
		}
		switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
			case Gravity.LEFT:
				x = 0;
				break;
			case Gravity.RIGHT:
				x = 1;
				break;
		}
		return (x + y * 2) * 2;
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	/**
	 * bitmap to editor
	 */
	protected final Matrix _Transform = new Matrix();
	private int _TransformMode = MODE_DEVICE;
	private final Matrix _InverseTransform = new Matrix();
	private boolean _InverseTransformInvalidated = false;

	public void invalidateTransform() {
		_InverseTransformInvalidated = true;
		validateTransform();
		requestLayout();
	}

	private void onLayoutContent() {
		MATRIX_UTIL.decomposeTSR(_Transform);
		Log.d("EDIT", "Content " + MATRIX_UTIL.toString());

		float hw = MATRIX_UTIL.scaleX * getContentWidth() / 2;
		float hh = MATRIX_UTIL.scaleY * getContentHeight() / 2;

		float[] center = getCenter();
		float cx = center[0];
		float cy = center[1];

		getContentView().setRotation(MATRIX_UTIL.getRotationDeg());

		getContentView().layout((int) (cx - hw), (int) (cy - hh), (int) (cx + hw),
				(int) (cy + hh));
	}

	public float[] getCenter(){
		int w = getWidth();
		int h = getHeight();

		if(w == 0 || h == 0){
			return null;
		}
		float[] center = new float[2];
		center[0] = getWidth() / 2 + MATRIX_UTIL.translateX;
		center[1] = getHeight() / 2 + MATRIX_UTIL.translateY;
		return center;
	}

	public void reset() {
		_Transform.reset();
		_TransformMode = MODE_DEVICE;

		invalidateTransform();
	}

	private void commit() {
		if (_Listener != null) {
			_Listener.onChange(AliyunPasterView.this);
		}
	}

	public boolean contentContains(float x, float y) {
		fromEditorToContent(x, y);

		float ix = POINT_LIST[0];
		float iy = POINT_LIST[1];

		boolean isContains;
		isContains = Math.abs(ix) <= getContentWidth() / 2
				&& Math.abs(iy) <= getContentHeight() / 2;
		return isContains;

	}

	private Matrix converge(){
		return _Transform;
	}


	/**
	 * from Editor coordinates (lt as (0, 0)) to Content coordinates (center as
	 * (0, 0))
	 */
	protected final void fromEditorToContent(float x, float y) {
		if (_InverseTransformInvalidated) {
			Matrix together = converge();
			together.invert(_InverseTransform);
			_InverseTransformInvalidated = false;
		}
		POINT_LIST[2] = x - width / 2;
		POINT_LIST[3] = y - height / 2;

		_InverseTransform.mapPoints(POINT_LIST, 0, POINT_LIST, 2, 1);
	}

	protected final void fromContentToEditor(float x, float y) {
		POINT_LIST[2] = x;
		POINT_LIST[3] = y;

		_Transform.mapPoints(POINT_LIST, 0, POINT_LIST, 2, 1);

		POINT_LIST[0] += width / 2;
		POINT_LIST[1] += height / 2;
	}

	// shared, ui thread only

	protected final MatrixUtil MATRIX_UTIL = new MatrixUtil();

	private static final float[] POINT_LIST = new float[8];

	public void moveContent(float dx, float dy) {
		_Transform.postTranslate(dx, dy);

		invalidateTransform();
	}

	public void scaleContent(float sx, float sy){
		Matrix m = new Matrix();
		m.set(_Transform);
		m.preScale(sx, sy);

		MATRIX_UTIL.decomposeTSR(m);
		int width = (int) (MATRIX_UTIL.scaleX * getContentWidth());
		int height = (int) (MATRIX_UTIL.scaleY * getContentHeight());

		float thold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
		float minScale = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
		if((width >= getWidth() + thold || height >= getHeight() + thold)
				&& sx > 1){
		}else if((width <= minScale || height <= minScale) && sx < 1){
		}else{
			_Transform.set(m);
		}

		invalidateTransform();
	}

	public void rotateContent(float rot){
		_Transform.postRotate((float) Math.toDegrees(rot));

		MATRIX_UTIL.decomposeTSR(_Transform);
		invalidateTransform();
	}

}
