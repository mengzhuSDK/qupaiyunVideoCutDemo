/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.recorder;

import android.animation.Animator;
import android.content.ActivityNotFoundException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.common.project.Project;
import com.aliyun.common.utils.CommonUtil;
import com.aliyun.common.utils.StorageUtils;
import com.aliyun.common.utils.ToastUtil;
import com.aliyun.demo.R;
import com.aliyun.demo.importer.MediaActivity;
import com.aliyun.demo.importer.Transcoder;
import com.aliyun.demo.recorder.util.Common;
import com.aliyun.demo.recorder.util.OrientationDetector;
import com.aliyun.qupai.editor.impl.AliyunEditorFactory;
import com.aliyun.qupai.import_core.AliyunIImport;
import com.aliyun.qupai.import_core.AliyunImportCreator;
import com.aliyun.quview.ProgressDialog;
import com.aliyun.quview.RecordTimelineView;
import com.aliyun.recorder.AliyunRecorderCreator;

import com.aliyun.recorder.supply.AliyunIClipManager;
import com.aliyun.recorder.supply.AliyunIRecorder;
import com.aliyun.recorder.supply.RecordCallback;

import com.aliyun.struct.common.AliyunDisplayMode;
import com.aliyun.struct.common.AliyunVideoParam;
import com.aliyun.struct.common.ScaleMode;
import com.aliyun.struct.common.VideoQuality;
import com.aliyun.struct.effect.EffectBase;
import com.aliyun.struct.effect.EffectFilter;
import com.aliyun.struct.effect.EffectPaster;
import com.aliyun.struct.recorder.CameraParam;
import com.aliyun.struct.recorder.CameraType;
import com.aliyun.struct.recorder.FlashType;
import com.aliyun.struct.recorder.MediaInfo;
import com.qu.preview.callback.OnFrameCallBack;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class RecorderDemo extends Activity
        implements View.OnClickListener, View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {
    private static final int RATIO_MODE_3_4 = 0;
    private static final int RATIO_MODE_1_1 = 1;
    private static final int RATIO_MODE_9_16 = 2;

    public static final int RESOLUTION_360P = 0;
    public static final int RESOLUTION_480P = 1;
    public static final int RESOLUTION_540P = 2;
    public static final int RESOLUTION_720P = 3;

    private static final int BEAUTY_LEVEL = 20;//美颜度
    private static final int TIMELINE_HEIGHT = 20;

    public static final String VIDEO_RESOLUTION = "video_resolution";
    public static final String MIN_DURATION = "min_duration";
    public static final String MAX_DURATION = "max_duration";
    public static final String VIDEO_QUALITY = "video_quality";
    public static final String GOP = "gop";

    private static final int MAX_SWITCH_VELOCITY = 2000;

    private static final float FADE_IN_START_ALPHA = 0.3f;
    private static final int FILTER_ANIMATION_DURATION = 1000;

    public static final int IntentCode = 0002;

    String[] eff_dirs;

    private int mResolutionMode;
    private int minDuration;
    private int maxDuration;
    private int gop;
    private int rotation;
    private int recordRotation;
    private int filterIndex = 0;
    private VideoQuality videoQuality;
    //    private int mRatioMode = RATIO_MODE_3_4;
    private int mRatioMode = RATIO_MODE_9_16;//显示全屏
    private AliyunIRecorder mRecorder;
    private AliyunIClipManager mClipManager;
    private GLSurfaceView mGlSurfaceView;
    private boolean isBeautyOn = true;
    private boolean isSelected = false;
    private RecordTimelineView mRecordTimelineView;
    private ImageView mSwitchRatioBtn, mSwitchBeautyBtn, mSwitchCameraBtn, mSwitchLightBtn, mBackBtn, mRecordBtn, mDeleteBtn, mCompleteBtn, mIconDefault;
    private TextView mRecordTimeTxt, filterTxt;
    ;
    private FrameLayout mToolBar, mRecorderBar, resCopy;
    private FlashType mFlashType = FlashType.OFF;
    private CameraType mCameraType = CameraType.BACK;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor;
    private float lastScaleFactor;
    private float exposureCompensationRatio = 0.5f;
    private boolean isOnMaxDuration;
    private boolean isOpenFailed;
    private AliyunVideoParam mVideoParam;
    private OrientationDetector orientationDetector;
    private boolean isRecording = false;
    private long downTime;
    private boolean isNeedFinish = false;
    private boolean isRecordError;
    private MediaScannerConnection mMediaScanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_recorder_demo);
        initOritationDetector();
        getData();
        initView();
        initSDK();
        reSizePreview();
        initAssetPath();
        copyAssets();
        mMediaScanner = new MediaScannerConnection(this, null);
        mMediaScanner.connect();
    }

    private void reSizePreview() {
        RelativeLayout.LayoutParams previewParams = null;
        RelativeLayout.LayoutParams timeLineParams = null;
        RelativeLayout.LayoutParams durationTxtParams = null;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        switch (mRatioMode) {
            case RATIO_MODE_1_1:
                previewParams = new RelativeLayout.LayoutParams(screenWidth, screenWidth);
                previewParams.addRule(RelativeLayout.BELOW, R.id.tools_bar);
                timeLineParams = new RelativeLayout.LayoutParams(screenWidth, TIMELINE_HEIGHT);
                timeLineParams.addRule(RelativeLayout.BELOW, R.id.preview);
                durationTxtParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                durationTxtParams.addRule(RelativeLayout.ABOVE, R.id.record_timeline);
                mToolBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRecorderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRecordTimelineView.setColor(R.color.record_fill_progress, R.color.color_red, android.R.color.white, R.color.editor_overlay_line);
                mSwitchRatioBtn.setSelected(false);
                mSwitchRatioBtn.setActivated(true);
                break;
            case RATIO_MODE_3_4:
                int barHeight = getVirtualBarHeigh();
                float ratio = (float) screenHeight / screenWidth;
                previewParams = new RelativeLayout.LayoutParams(screenWidth, screenWidth * 4 / 3);
                if (barHeight > 0 || ratio < (16f / 9.2f)) {
                    mToolBar.setBackgroundColor(getResources().getColor(R.color.tools_bar_color));
                } else {
                    previewParams.addRule(RelativeLayout.BELOW, R.id.tools_bar);
                    mToolBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                }
                timeLineParams = new RelativeLayout.LayoutParams(screenWidth, TIMELINE_HEIGHT);
                timeLineParams.addRule(RelativeLayout.BELOW, R.id.preview);
                durationTxtParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                durationTxtParams.addRule(RelativeLayout.ABOVE, R.id.record_timeline);
                mRecorderBar.setBackgroundColor(getResources().getColor(R.color.transparent));
                mRecordTimelineView.setColor(R.color.record_fill_progress, R.color.color_red, android.R.color.white, R.color.editor_overlay_line);
                mSwitchRatioBtn.setSelected(false);
                mSwitchRatioBtn.setActivated(false);
                break;
            case RATIO_MODE_9_16:
                previewParams = new RelativeLayout.LayoutParams(screenWidth, screenWidth * 16 / 9);
                if (previewParams.height > screenHeight) {
                    previewParams.height = screenHeight;
                }
                timeLineParams = new RelativeLayout.LayoutParams(screenWidth, TIMELINE_HEIGHT);
                timeLineParams.addRule(RelativeLayout.ABOVE, R.id.record_layout);
                durationTxtParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                durationTxtParams.addRule(RelativeLayout.ABOVE, R.id.record_timeline);
                mToolBar.setBackgroundColor(getResources().getColor(R.color.tools_bar_color));
                mRecorderBar.setBackgroundColor(getResources().getColor(R.color.tools_bar_color));
                mRecordTimelineView.setColor(R.color.record_fill_progress, R.color.color_red, android.R.color.white, R.color.qupai_transparent);
                mSwitchRatioBtn.setSelected(true);
                mSwitchRatioBtn.setActivated(true);
                break;
        }
        if (previewParams != null) {
            mGlSurfaceView.setLayoutParams(previewParams);
        }
        if (timeLineParams != null) {
            mRecordTimelineView.setLayoutParams(timeLineParams);
        }
        if (durationTxtParams != null) {
            mRecordTimeTxt.setLayoutParams(durationTxtParams);
        }
    }

    //横竖屏幕切换
    private void initOritationDetector() {
        orientationDetector = new OrientationDetector(getApplicationContext());
        orientationDetector.setOrientationChangedListener(new OrientationDetector.OrientationChangedListener() {
            @Override
            public void onOrientationChanged() {
                rotation = getPictureRotation();
                Log.e("rotation",rotation+"");
            }
        });
    }

    private void reOpenCamera(int width, int height) {
        mRecorder.stopPreview();
        MediaInfo info = new MediaInfo();
        info.setVideoWidth(width);
        info.setVideoHeight(height);
        info.setHWAutoSize(true);//硬编时自适应宽高为16的倍数
        mRecorder.setMediaInfo(info);
        mRecorder.startPreview();
    }

    @Override
    public void onBackPressed() {
        if (!isRecording) {
            super.onBackPressed();
        }
    }

    private void initAssetPath() {
        String path = StorageUtils.getCacheDirectory(this).getAbsolutePath() + File.separator + Common.QU_NAME + File.separator;
        eff_dirs = new String[]{
                null,
                path + "filter/chihuang",
                path + "filter/fentao",
                path + "filter/hailan",
                path + "filter/hongrun",
                path + "filter/huibai",
                path + "filter/jingdian",
                path + "filter/maicha",
                path + "filter/nonglie",
                path + "filter/rourou",
                path + "filter/shanyao",
                path + "filter/xianguo",
                path + "filter/xueli",
                path + "filter/yangguang",
                path + "filter/youya",
                path + "filter/zhaoyang"
        };
    }

    private void copyAssets() {
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Common.copyAll(RecorderDemo.this);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                resCopy.setVisibility(View.GONE);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initView() {
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.preview);
//        mGlSurfaceView.setOnTouchListener(this);
        mSwitchRatioBtn = (ImageView) findViewById(R.id.switch_ratio);
        mSwitchRatioBtn.setOnClickListener(this);
        mSwitchBeautyBtn = (ImageView) findViewById(R.id.switch_beauty);
        mSwitchBeautyBtn.setOnClickListener(this);
        mSwitchCameraBtn = (ImageView) findViewById(R.id.switch_camera);
        mSwitchCameraBtn.setOnClickListener(this);
        mSwitchLightBtn = (ImageView) findViewById(R.id.switch_light);
        mSwitchLightBtn.setImageResource(R.mipmap.icon_light_dis);
        mSwitchLightBtn.setOnClickListener(this);
        mBackBtn = (ImageView) findViewById(R.id.back);
        mBackBtn.setOnClickListener(this);
        mRecordBtn = (ImageView) findViewById(R.id.record_btn);//开始录制
//        mRecordBtn.setOnTouchListener(this);
        mRecordBtn.setOnClickListener(this);
        mDeleteBtn = (ImageView) findViewById(R.id.delete_btn);
        mDeleteBtn.setOnClickListener(this);
        mCompleteBtn = (ImageView) findViewById(R.id.complete_btn);
        mCompleteBtn.setOnClickListener(this);
        mRecordTimelineView = (RecordTimelineView) findViewById(R.id.record_timeline);
        mRecordTimelineView.setColor(R.color.record_fill_progress, R.color.colorPrimary, R.color.qupai_black_opacity_70pct, R.color.editor_overlay_line);
        mRecordTimeTxt = (TextView) findViewById(R.id.record_time);
        filterTxt = (TextView) findViewById(R.id.filter_txt);
        resCopy = (FrameLayout) findViewById(R.id.copy_res_tip);
        mIconDefault = (ImageView) findViewById(R.id.icon_default);
        mToolBar = (FrameLayout) findViewById(R.id.tools_bar);
        mRecorderBar = (FrameLayout) findViewById(R.id.record_layout);
        mIconDefault.setOnClickListener(this);
        scaleGestureDetector = new ScaleGestureDetector(this, this);
        gestureDetector = new GestureDetector(this, this);
    }

    private void initSDK() {
        mRecorder = AliyunRecorderCreator.getRecorderInstance(this);
        mRecorder.setDisplayView(mGlSurfaceView);
        mRecorder.setOnFrameCallback(new OnFrameCallBack() {
            @Override
            public void onFrameBack(byte[] bytes, int width, int height, Camera.CameraInfo info) {
                isOpenFailed = false;
            }

            @Override
            public void openFailed() {
                isOpenFailed = true;
            }
        });
        mClipManager = mRecorder.getClipManager();
        mClipManager.setMinDuration(minDuration);
        mClipManager.setMaxDuration(maxDuration);
        mRecordTimelineView.setMaxDuration(mClipManager.getMaxDuration());
        mRecordTimelineView.setMinDuration(mClipManager.getMinDuration());
        int[] resolution = getResolution();
        //MediaInfo 设置视频宽度
        MediaInfo info = new MediaInfo();
        info.setVideoWidth(resolution[0]);
        info.setVideoHeight(resolution[1]);
        info.setHWAutoSize(true);//硬编时自适应宽高为16的倍数
        mRecorder.setMediaInfo(info);
        mCameraType = mRecorder.getCameraCount() == 1 ? CameraType.BACK : mCameraType;
        mRecorder.setCamera(mCameraType);
        mRecorder.setBeautyLevel(BEAUTY_LEVEL);//美颜度
        mRecorder.setBeautyStatus(isBeautyOn);
        mRecorder.setGop(gop);//关键帧间隔
        mRecorder.setBeautyStatus(isBeautyOn);//设置美颜开／关，true：表示开，false：表示关
        mRecorder.setVideoQuality(videoQuality);

        //添加 贴图   需要第三方人脸识别支持
//        DisplayMetrics dm = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//        int height = dm.heightPixels;
//        int width = dm.widthPixels;
//        EffectPaster mEffectPaster = new EffectPaster("/storage/emulated/0/Android/data/cn.a218.newproject/cache/AliyunEditorDemo/overlay/tianxie");
//        mEffectPaster.width = width;
//        mEffectPaster.height = height;
//        mEffectPaster.start = 0;
//        mEffectPaster.end = 25000000;
//        mRecorder.addPaster(mEffectPaster);
//        mRecorder.setEffectView(0f,0f,1f,1f,effectBase);
        //;//设置图片的信息（位置，尺寸），其中xRatio，yRatio表示其起始坐标在屏幕中的相对位置百分比，widthRatio，heightRatio表示图片宽高和屏幕宽高的比例值

        mRecorder.setRecordCallBack(new RecordCallback() {//录制回调
            @Override
            public void onComplete(boolean validClip, long clipDuration) {
                handleRecordCallback(validClip, clipDuration);
//                if (isOnMaxDuration || isNeedFinish) {//录制完成
//                    isRecording = false;
//                    ToEditPage();
//                }
                toFinishRecordandNewMp4();
            }

            @Override
            public void onFinish(String outputPath) {
                mMediaScanner.scanFile(outputPath, "video/mp4");
            }

            @Override
            public void onProgress(final long duration) {
//                Logger.getDefaultLogger().d("duration..." + duration);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecordTimelineView.setDuration((int) duration);
                        int time = (int) (mClipManager.getDuration() + duration) / 1000;
                        int min = time / 60;
                        int sec = time % 60;
                        mRecordTimeTxt.setText(String.format("%1$02d:%2$02d", min, sec));
                        if (mRecordTimeTxt.getVisibility() != View.VISIBLE) {
                            mRecordTimeTxt.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }

            @Override
            public void onMaxDuration() {
                isOnMaxDuration = true;
//                toFinishRecordandNewMp4();
                stopRecording();
                isRecording = false;
            }

            @Override
            public void onError(int errorCode) {
                Log.e("record", "record eoor" + errorCode);
                isRecordError = true;
                handleRecordCallback(false, 0);
            }

            @Override
            public void onInitReady() {

            }

            @Override
            public void onPictureBack(Bitmap bitmap) {

            }

            @Override
            public void onPictureDataBack(byte[] data) {

            }
        });

        mRecorder.setFocusMode(CameraParam.FOCUS_MODE_CONTINUE);//自动识别，并且手动聚焦
        mRecorder.setExposureCompensationRatio(exposureCompensationRatio);//当前曝光度的比例值
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 部分android4.4机型会出现跳转Activity gl为空的问题，如果不需要适配，显示视图代码可以去掉
         */
        mGlSurfaceView.setVisibility(View.VISIBLE);
        mRecorder.startPreview();
        if (orientationDetector != null && orientationDetector.canDetectOrientation()) {
            orientationDetector.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecorder.stopPreview();
        if (isRecording) {
            mRecorder.cancelRecording();
            isRecording = false;
        }
        /**
         * 部分android4.4机型会出现跳转Activity gl为空的问题，如果不需要适配，隐藏视图代码可以去掉
         */
        mGlSurfaceView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orientationDetector != null) {
            orientationDetector.disable();
        }
    }

    private void getData() {
        mResolutionMode = getIntent().getIntExtra(VIDEO_RESOLUTION, RESOLUTION_540P);
        minDuration = getIntent().getIntExtra(MIN_DURATION, 10);//录制最短时间
        maxDuration = getIntent().getIntExtra(MAX_DURATION, 50000);//录制最长时间
        gop = getIntent().getIntExtra(GOP, 125);
        videoQuality = (VideoQuality) getIntent().getSerializableExtra(VIDEO_QUALITY);
        if (videoQuality == null) {
            videoQuality = VideoQuality.SSD;
        }
        mVideoParam = new AliyunVideoParam.Builder()
                .gop(gop)
                .frameRate(25)
                .videoQuality(videoQuality)
                .build();
        int[] resolutions = getResolution();
        mVideoParam.setScaleMode(ScaleMode.LB);
        mVideoParam.setOutputWidth( resolutions[0]);
        mVideoParam.setOutputHeight( resolutions[1]);
        if(resolutions[0]>resolutions[1]){
            mVideoParam.setOutputWidth( resolutions[1]);
            mVideoParam.setOutputHeight( resolutions[0]);
        }
    }

    public int getVirtualBarHeigh() {
        int vh = 0;
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            vh = dm.heightPixels - windowManager.getDefaultDisplay().getHeight();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vh;
    }


    private int[] getResolution() {
        int[] resolution = new int[2];
        int width = 0;
        int height = 0;
        switch (mResolutionMode) {
            case RESOLUTION_360P:
                width = 360;
                break;
            case RESOLUTION_480P:
                width = 480;
                break;
            case RESOLUTION_540P:
                width = 540;
                break;
            case RESOLUTION_720P:
                width = 720;
                break;
        }
        switch (mRatioMode) {
            case RATIO_MODE_1_1:
                height = width;
                break;
            case RATIO_MODE_3_4:
                height = width * 4 / 3;
                break;
            case RATIO_MODE_9_16:
                height = width * 16 / 9;
                break;
        }
        resolution[0] = width;
        resolution[1] = height;
        return resolution;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecorder.destroy();
        if (orientationDetector != null) {
            orientationDetector.setOrientationChangedListener(null);
        }
        AliyunRecorderCreator.destroyRecorderInstance();
        mMediaScanner.disconnect();
    }

    @Override
    public void onClick(View v) {
        if (v == mSwitchBeautyBtn) {
            if (isBeautyOn) {
                isBeautyOn = false;
                mSwitchBeautyBtn.setImageResource(R.drawable.switch_beauty);
            } else {
                isBeautyOn = true;
                mSwitchBeautyBtn.setImageResource(R.drawable.switch_beauty_selected);
            }
            mRecorder.setBeautyStatus(isBeautyOn);
        } else if (v == mSwitchCameraBtn) {
            int type = mRecorder.switchCamera();
            if (type == CameraType.BACK.getType()) {
                mCameraType = CameraType.BACK;
                mSwitchLightBtn.setEnabled(true);
                mSwitchLightBtn.setImageResource(R.drawable.switch_light_selector);
            } else if (type == CameraType.FRONT.getType()) {
                mCameraType = CameraType.FRONT;
                mSwitchLightBtn.setEnabled(false);
                mSwitchLightBtn.setImageResource(R.mipmap.icon_light_dis);
            }
        } else if (v == mSwitchLightBtn) {
            if (mFlashType == FlashType.OFF) {
                mFlashType = FlashType.AUTO;
            } else if (mFlashType == FlashType.AUTO) {
                mFlashType = FlashType.ON;
            } else if (mFlashType == FlashType.ON) {
                mFlashType = FlashType.OFF;
            }
            switch (mFlashType) {
                case AUTO:
                    v.setSelected(false);
                    v.setActivated(true);
                    break;
                case ON:
                    v.setSelected(true);
                    v.setActivated(false);
                    break;
                case OFF:
                    v.setSelected(true);
                    v.setActivated(true);
                    break;
            }
            mRecorder.setLight(mFlashType);
        } else if (v == mSwitchRatioBtn) {
            mRatioMode++;
            if (mRatioMode > RATIO_MODE_9_16) {
                mRatioMode = RATIO_MODE_3_4;
            }
            reSizePreview();
            int[] resolution = getResolution();
            reOpenCamera(resolution[0], resolution[1]);

        } else if (v == mBackBtn) {
            onBackPressed();
        } else if (v == mRecordBtn) {//中间录制按钮
            if (isOpenFailed) {
                Toast.makeText(this, R.string.camera_permission_tip, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isRecording) {
                if (!checkIfStartRecording()) {
                    return;
                }
                mRecordBtn.setPressed(true);
                startRecording();
                mRecordBtn.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mRecordBtn.isPressed()) {
                            mRecordBtn.setSelected(true);
                            mRecordBtn.setHovered(false);
                        }
                    }
                }, 200);
                isRecording = true;
            } else {
                stopRecording();
                isRecording = false;
            }
        } else if (v == mCompleteBtn) {//完成录制
//            if (mClipManager.getDuration() >= mClipManager.getMinDuration()) {
//                isNeedFinish = true;
//                toFinishRecordandNewMp4();
//            }
            ToEditPage();

        } else if (v == mDeleteBtn) {
            if (!isSelected) {
                mRecordTimelineView.selectLast();
//                mDeleteBtn.setActivated(true);
                isSelected = true;
            } else {
                mRecordTimelineView.deleteLast();
//                mDeleteBtn.setActivated(false);
                mClipManager.deletePart();
                isSelected = false;
                if (mClipManager.getDuration() == 0) {
                    mIconDefault.setVisibility(View.VISIBLE);
                    mSwitchRatioBtn.setEnabled(true);
                    mCompleteBtn.setVisibility(View.GONE);
//                    mDeleteBtn.setVisibility(View.GONE);
                }
            }
        } else if (v == mIconDefault) {//选择 短视频
            try {
                Intent intent = new Intent("com.duanqu.qupai.action.import");
                startActivityForResult(intent, IntentCode);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_import_moudle, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private com.aliyun.demo.importer.media.MediaInfo theBackInfo;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == IntentCode) {
            if (data.getExtras().get("video_param") != null) {
                theBackInfo = (com.aliyun.demo.importer.media.MediaInfo) data.getExtras().get("video_param");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private int getPictureRotation() {
        int orientation = orientationDetector.getOrientation();
        int rotation = 90;
        if ((orientation >= 45) && (orientation < 135)) {
            rotation = 180;
        }
        if ((orientation >= 135) && (orientation < 225)) {
            rotation = 270;
        }
        if ((orientation >= 225) && (orientation < 315)) {
            rotation = 0;
        }
        if (mCameraType == CameraType.FRONT) {
            if (rotation != 0) {
                rotation = 360 - rotation;
            }
        }
        return rotation;
    }

    //跳转合成编辑页面
    private void toFinishRecordandNewMp4() {

        mRecorder.stopPreview();
        //设置生成视频的信息
        mRecorder.finishRecording();
//        Uri projectUri = mRecorder.finishRecordingForEdit();//这里生成一个temp文件，1496989333512_1496989333552.mp4
    }

    private void ToEditPage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressDialog progressDialog = ProgressDialog.show(RecorderDemo.this, null, getResources().getString(R.string.wait));
                progressDialog.show();

                if(theBackInfo == null){
                    Uri projectUri = mRecorder.finishRecordingForEdit();
                    Intent intent = new Intent("com.duanqu.qupai.action.editor");
                    intent.putExtra("video_param", mVideoParam);
                    intent.putExtra("project_json_path", projectUri.getPath());
                    progressDialog.cancel();
                    startActivity(intent);
                    return;
                }

//                progressDialog.setCancelable(true);
////      下面是开始跳转 到 视频编辑界面
                AliyunIImport mImport = AliyunImportCreator.getImportInstance(RecorderDemo.this);
                mImport.setVideoParam(mVideoParam);

                if (theBackInfo != null) {
                    mImport.addVideo(theBackInfo.filePath, 0, AliyunDisplayMode.DEFAULT);
                }
                if (videoPaths.size() == 0) {
                    Toast.makeText(RecorderDemo.this, "没有录制视频信息", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (String path : videoPaths) {
                    mImport.addVideo(path, 0, AliyunDisplayMode.DEFAULT);
                }
                String projectJsonPath = mImport.generateProjectConfigure();//生成项目配置path
                progressDialog.cancel();
                if (projectJsonPath != null) {
                    Intent intent = new Intent("com.duanqu.qupai.action.editor");
                    intent.putExtra("video_param", mVideoParam);
                    intent.putExtra("project_json_path", projectJsonPath);
                    startActivity(intent);
                }
            }
        });
    }

    //停止录制
    private void stopRecording() {
        mRecorder.stopRecording();
        handleRecordStop();
    }

    //录制视频的路径
    List<String> videoPaths = new ArrayList<>();

    //开始录
    private void startRecording() {
        String videoPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_MOVIES + File.separator + System.currentTimeMillis() + ".mp4";
        mRecorder.setOutputPath(videoPath);
        videoPaths.add(videoPath);
        int tempRotation = getPictureRotation();
        if (tempRotation == 90 || tempRotation == 270) {
            recordRotation = (getPictureRotation() + 90) % 360;
            if (mCameraType == CameraType.BACK) {
                recordRotation += 180;
                recordRotation %= 360;
            }
        } else if (tempRotation == 0 || tempRotation == 180) {
            recordRotation = (getPictureRotation() + 270) % 360;
        }
        handleRecordStart();
//        mRecorder.setRotation(recordRotation);
        mRecorder.setRotation(270);//0为横屏时候播放也是横屏
        isRecordError = false;
        mRecorder.startRecording();
        if (mFlashType == FlashType.ON && mCameraType == CameraType.BACK) {
            mRecorder.setLight(FlashType.TORCH);
        }
    }

    private boolean checkIfStartRecording() {
        if (mRecordBtn.isActivated()) {
            return false;
        }
        if (CommonUtil.SDFreeSize() < 50 * 1000 * 1000) {
            Toast.makeText(this, R.string.no_free_memory, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mRecordBtn) {//开始录制
            if (isOpenFailed) {
                Toast.makeText(this, R.string.camera_permission_tip, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downTime = System.currentTimeMillis();
                if (!isRecording) {
                    if (!checkIfStartRecording()) {
                        return false;
                    }
                    mRecordBtn.setPressed(true);
                    startRecording();
                    mRecordBtn.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mRecordBtn.isPressed()) {
                                mRecordBtn.setSelected(true);
                                mRecordBtn.setHovered(false);
                            }
                        }
                    }, 200);
                    isRecording = true;
                } else {
                    stopRecording();
                    isRecording = false;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                long timeOffset = System.currentTimeMillis() - downTime;
                mRecordBtn.setPressed(false);
                if (timeOffset > 1000) {
                    if (isRecording) {
                        stopRecording();
                        isRecording = false;
                    }
                } else {
                    if (!isRecordError) {
                        mRecordBtn.setSelected(false);
                        mRecordBtn.setHovered(true);
                    } else {
                        isRecording = false;
                    }
                }
            }
        } else if (v == mGlSurfaceView) {
            if (event.getPointerCount() >= 2) {
//                scaleGestureDetector.onTouchEvent(event);
            } else if (event.getPointerCount() == 1) {
//                gestureDetector.onTouchEvent(event);
            }


        }
        return true;
    }

    private void handleRecordStart() {
        isSelected = false;
        mRecordBtn.setActivated(true);
        mIconDefault.setVisibility(View.GONE);
        mCompleteBtn.setVisibility(View.VISIBLE);
//        mDeleteBtn.setVisibility(View.VISIBLE);
        mSwitchRatioBtn.setEnabled(false);
        mSwitchBeautyBtn.setEnabled(false);
        mSwitchCameraBtn.setEnabled(false);
        mSwitchLightBtn.setEnabled(false);
        mCompleteBtn.setEnabled(false);
        mDeleteBtn.setEnabled(false);
//        mDeleteBtn.setActivated(false);
    }

    //停止录制 关灯
    private void handleRecordStop() {
        if (mFlashType == FlashType.ON && mCameraType == CameraType.BACK) {
            mRecorder.setLight(FlashType.OFF);
        }
    }

    //录制 回调
    private void handleRecordCallback(final boolean validClip, final long clipDuration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordBtn.setActivated(false);
                mRecordBtn.setHovered(false);
                mRecordBtn.setSelected(false);
                if (validClip) {//录制错误
                    mRecordTimelineView.setDuration((int) clipDuration);
                    mRecordTimelineView.clipComplete();
                } else {
                    mRecordTimelineView.setDuration(0);
                }
                mRecordTimeTxt.setVisibility(View.GONE);
                mSwitchBeautyBtn.setEnabled(true);
                mSwitchCameraBtn.setEnabled(true);
                mSwitchLightBtn.setEnabled(true);
                mCompleteBtn.setEnabled(true);
//                mDeleteBtn.setEnabled(true);

            }
        });

    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float factorOffset = detector.getScaleFactor() - lastScaleFactor;
        scaleFactor += factorOffset;
        lastScaleFactor = detector.getScaleFactor();
        if (scaleFactor < 0) {
            scaleFactor = 0;
        }
        if (scaleFactor > 1) {
            scaleFactor = 1;
        }
        mRecorder.setZoom(scaleFactor);
        return false;

    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        lastScaleFactor = detector.getScaleFactor();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onDown(MotionEvent e) {

        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mRecorder.setFocus(null);
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (Math.abs(distanceX) > 20) {
            return false;
        }
        exposureCompensationRatio += (distanceY / mGlSurfaceView.getHeight());
        if (exposureCompensationRatio > 1) {
            exposureCompensationRatio = 1;
        }
        if (exposureCompensationRatio < 0) {
            exposureCompensationRatio = 0;
        }
        mRecorder.setExposureCompensationRatio(exposureCompensationRatio);
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (resCopy.getVisibility() == View.VISIBLE) {
            return true;
        }
        if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) {
            return true;
        }
        if (mRecordBtn.isActivated()) {
            return true;
        }
        if (velocityX > MAX_SWITCH_VELOCITY) {
            filterIndex++;
            if (filterIndex >= eff_dirs.length) {
                filterIndex = 0;
            }
        } else if (velocityX < -MAX_SWITCH_VELOCITY) {
            filterIndex--;
            if (filterIndex < 0) {
                filterIndex = eff_dirs.length - 1;
            }
        } else {
            return true;
        }
//        if(!new File(eff_dirs[filterIndex]).exists()){
//            return false;
//        }
        EffectFilter effectFilter = new EffectFilter(eff_dirs[filterIndex]);
        mRecorder.applyFilter(effectFilter);
        showFilter(effectFilter.getName());
        return false;
    }


    private void showFilter(String name) {
        if (name == null || name.isEmpty()) {
            name = getString(R.string.filter_null);
        }
        filterTxt.animate().cancel();
        filterTxt.setText(name);
        filterTxt.setVisibility(View.VISIBLE);
        filterTxt.setAlpha(FADE_IN_START_ALPHA);
        txtFadeIn();
    }

    private void txtFadeIn() {
        filterTxt.animate().alpha(1).setDuration(FILTER_ANIMATION_DURATION / 2).setListener(
                new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        txtFadeOut();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
    }

    private void txtFadeOut() {
        filterTxt.animate().alpha(0).setDuration(FILTER_ANIMATION_DURATION / 2).start();
        filterTxt.animate().setListener(null);
    }
}
