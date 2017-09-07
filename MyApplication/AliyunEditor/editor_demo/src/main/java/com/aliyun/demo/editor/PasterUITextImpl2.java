/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.editor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Gravity;

import com.aliyun.demo.editor.timeline.TimelineBar;
import com.aliyun.demo.widget.AliyunPasterView;
import com.aliyun.demo.widget.AutoResizingTextView;
import com.aliyun.qupai.editor.AliyunPasterController;

/**
 * Created by Administrator on 2017/3/13.
 */
public class PasterUITextImpl2 extends PasterUIGifImpl{

    int height = 0;
    int width = 0;

    public PasterUITextImpl2(Activity mActivity , AliyunPasterView pasterView, AliyunPasterController controller, TimelineBar timelineBar){
        this(mActivity,pasterView, controller, timelineBar, false);
    }

    public PasterUITextImpl2(Activity mActivity , AliyunPasterView pasterView, AliyunPasterController controller, TimelineBar timelineBar, boolean completed) {
        super(pasterView, controller, timelineBar,completed);

        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        height = dm.heightPixels;
        width = dm.widthPixels;

        if(mText == null ){
            mText = (AutoResizingTextView) mPasterView.getContentView();
        }

//        mPasterView.rotateContent(1.58f);
        mText.setText(controller.getText());
        mText.setTextOnly(true);
        mText.setFontPath(controller.getPasterTextFont());
        mText.setTextAngle(controller.getPasterTextRotation());
        mText.setGravity(Gravity.CENTER);
        mText.setTextWidth(controller.getPasterWidth());
        mText.setTextHeight(controller.getPasterHeight());
        if(completed){
            mText.setTextStrokeColor(Color.BLUE);
            mText.setCurrentColor(Color.RED);
            mText.setEditCompleted(true);
            pasterView.setEditCompleted(true);
        }else{
            mText.setTextStrokeColor(Color.GREEN);
            mText.setCurrentColor(Color.BLACK);
            mText.setEditCompleted(true);
            pasterView.setEditCompleted(true);
        }

    }

    @Override
    public int getPasterWidth() {
        return 700;
    }

    @Override
    public int getPasterHeight() {
        return 240;
    }

    @Override
    public int getPasterCenterY() {
        return height/2;
    }

    @Override
    public int getPasterCenterX() {
        return width/2;
    }

    @Override
    public void mirrorPaster(boolean mirror) {

    }

    @Override
    protected void playPasterEffect() {

    }

    @Override
    protected void stopPasterEffect() {

    }

    @Override
    public String getText() {
        return mText.getText().toString();
    }

    @Override
    public int getTextColor() {
        return mText.getTextColor();
    }

    @Override
    public String getPasterTextFont() {
        return mText.getFontPath();
    }

    @Override
    public int getTextStrokeColor() {
        return mText.getTextStrokeColor();
    }

    @Override
    public boolean isTextHasStroke() {
        return getTextStrokeColor() == 0;
    }

    @Override
    public boolean isTextHasLabel() {
        return mPasterView.getTextLabel() != null;
    }

    @Override
    public int getTextBgLabelColor() {
        return super.getTextBgLabelColor();
    }

    @Override
    public Bitmap transToImage() {
        return mText.layoutToBitmap();
    }

}
