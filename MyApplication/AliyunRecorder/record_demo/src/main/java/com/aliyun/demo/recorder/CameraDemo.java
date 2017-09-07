/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.recorder;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
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
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.common.logger.Logger;
import com.aliyun.common.utils.CommonUtil;
import com.aliyun.common.utils.DensityUtil;
import com.aliyun.common.utils.MySystemParams;
import com.aliyun.common.utils.StorageUtils;
import com.aliyun.common.utils.ToastUtil;
import com.aliyun.demo.R;
import com.aliyun.downloader.FileDownloaderCallback;
import com.aliyun.downloader.zipprocessor.DownloadFileUtils;
import com.aliyun.demo.recorder.util.Common;
import com.aliyun.demo.recorder.util.OrientationDetector;
import com.aliyun.demo.recorder.util.STLicenseUtils;

import com.aliyun.downloader.DownloaderManager;
import com.aliyun.downloader.FileDownloaderModel;
import com.aliyun.jasonparse.JSONSupportImpl;
import com.aliyun.querrorcode.AliyunErrorCode;
import com.aliyun.recorder.AliyunRecorderCreator;

import com.aliyun.qupaiokhttp.HttpRequest;
import com.aliyun.qupaiokhttp.StringHttpRequestCallback;
import com.aliyun.quview.CenterLayoutManager;
import com.aliyun.quview.CircleProgressBar;
import com.aliyun.quview.FanProgressBar;
import com.aliyun.recorder.supply.AliyunIClipManager;
import com.aliyun.struct.effect.EffectFilter;
import com.aliyun.struct.effect.EffectPaster;

import com.aliyun.struct.form.PreviewPasterForm;
import com.aliyun.struct.form.PreviewResourceForm;
import com.aliyun.struct.recorder.CameraType;
import com.aliyun.struct.recorder.FlashType;
import com.aliyun.struct.recorder.MediaInfo;
import com.aliyun.recorder.supply.RecordCallback;

import com.google.gson.reflect.TypeToken;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.qu.preview.callback.OnFrameCallBack;
import com.aliyun.recorder.supply.AliyunIRecorder;
import com.qu.preview.callback.OnTextureIdCallBack;
import com.sensetime.stmobile.FileUtils;
import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.STRotateType;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CameraDemo extends Activity implements View.OnClickListener, GestureDetector.OnGestureListener,
        View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {
    private static final String TAG = "CameraDemo";
    private static final String LOCAL_SETTING_NAME = "sdk_record_download_paster";

    private GLSurfaceView glSurfaceView;
    private ImageView switchCameraBtn, switchLightBtn, backBtn;
    private TextView recordDuration, filterTxt;
    private AliyunIRecorder recorder;
    private FlashType flashType = FlashType.OFF;
    private CameraType cameraType = CameraType.FRONT;
    private RecyclerView pasterView;
    private PasterAdapter adapter;
    private FanProgressBar fanProgressBar;
    private FrameLayout recordBg, resCopy;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private PreviewPasterForm currentPasterForm;


    private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
    private LinearSnapHelper linearSnapHelper;
    private LinearLayoutManager linearLayoutManager;
    private static int TEST_VIDEO_WIDTH = 540;
    private static int TEST_VIDEO_HEIGHT = 960;
    private static final int MAX_ITEM_COUNT = 5;
    private static final int MIN_RECORD_TIME = 500;
    private static final int MAX_RECORD_TIME = 8000;
    private static final int MAX_SWITCH_VELOCITY = 2000;

    private static final float FADE_IN_START_ALPHA = 0.3f;
    private static final int FILTER_ANIMATION_DURATION = 1000;

    private static final int LEFT_EYE_X = 0;
    private static final int LEFT_EYE_Y = 1;
    private static final int RIGHT_EYE_X = 2;
    private static final int RIGHT_EYE_Y = 3;
    private static final int MOUTH_X = 4;
    private static final int MOUTH_Y = 5;


    private static int OUT_STROKE_WIDTH;

    private CircleProgressBar progressBar;
    private int itemWidth;
    private EffectPaster effect;
    private int filterIndex = 0;
    private int currentScrollPos = 0;
    private String videoPath;
    private int recordTime;
    private int beautyLevel = 80;
    private boolean recordStopped = true;
    private float lastScaleFactor;
    private float scaleFactor;

    private boolean isOpenFailed;

    private boolean isFaceDetectOpen;

    long downTime = 0;

    private AliyunIClipManager clipManager;

    private OrientationDetector orientationDetector;
    private int rotation;
    private int recordRotation;

    private List<PreviewPasterForm> resources = new ArrayList<>();
    String[] eff_dirs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MySystemParams.getInstance().init(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_demo);
        getData();
        initOritationDetector();
//        copyAndCheckForST();
        initFaceDetector();
        initView();
        initSDK();
        initAssetPath();
        copyAssets();
    }

    private void initAssetPath(){
        String path = StorageUtils.getCacheDirectory(this).getAbsolutePath() + File.separator+Common.QU_NAME + File.separator;
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

    private void initOritationDetector(){
        orientationDetector = new OrientationDetector(getApplicationContext());
        orientationDetector.setOrientationChangedListener(new OrientationDetector.OrientationChangedListener() {
            @Override
            public void onOrientationChanged() {
                rotation = getPictureRotation();
//                            Log.e("rotation",rotation+"");
            }
        });
    }

    private void copyAndCheckForST() {
        FileUtils.copyModelFiles(CameraDemo.this);
        if (!STLicenseUtils.checkLicense(CameraDemo.this)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "You should be authorized first!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void getData() {
        TEST_VIDEO_WIDTH = getIntent().getIntExtra("width", TEST_VIDEO_WIDTH);
        TEST_VIDEO_HEIGHT = getIntent().getIntExtra("height", TEST_VIDEO_HEIGHT);
        isFaceDetectOpen = getIntent().getBooleanExtra("face", true);
        beautyLevel = getIntent().getBooleanExtra("beauty", true) ? beautyLevel : 0;
    }

    private void initHumanAction() {
        int result = mSTHumanActionNative.createInstance(FileUtils.getTrackModelPath(this),
                STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO);
    }

    private void initFaceDetector() {

//        SpeechUtility.createUtility(this,"appid=" + getString(com.iflytek.facedemo.R.string.app_id));
//        mFaceDetector = FaceDetector.createDetector(this,null);
//        initHumanAction();
    }

    private void initSDK() {
        recorder = AliyunRecorderCreator.getRecorderInstance(this);
        recorder.setDisplayView(glSurfaceView);
        clipManager = recorder.getClipManager();
        clipManager.setMaxDuration(MAX_RECORD_TIME);
        clipManager.setMinDuration(MIN_RECORD_TIME);
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setVideoWidth(TEST_VIDEO_WIDTH);
        mediaInfo.setVideoHeight(TEST_VIDEO_HEIGHT);
        mediaInfo.setHWAutoSize(true);//硬编时自适应宽高为16的倍数
        recorder.setMediaInfo(mediaInfo);
        cameraType = recorder.getCameraCount() == 1 ? CameraType.BACK : cameraType;
        recorder.setCamera(cameraType);
        recorder.setBeautyLevel(beautyLevel);
        recorder.setOnFrameCallback(new OnFrameCallBack() {
            @Override
            public void onFrameBack(byte[] bytes, int width, int height,Camera.CameraInfo info) {

                if (!isFaceDetectOpen) {
                    return;
                }

                int orient;
                if(Camera.CameraInfo.CAMERA_FACING_FRONT == info.facing) {
                    orient = (info.orientation + (rotation - 270)+360)%360;
                } else {
                    orient = (info.orientation + (rotation - 90)+360)%360;
                }
                int orientation = getOrientation(orient);

                float[][] point;
//                String result = mFaceDetector.trackNV21(bytes,width,height,1,orientation);
//                point = parsePoints(result,width,height);

//                STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(bytes, STCommon.ST_PIX_FMT_NV21,
//                        STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_DETECT, orientation,
//                        width, height);
//                if (humanAction == null) {
//                    return;
//                }
//                point = new float[humanAction.faceCount][6];
//                for (int i = 0; i < humanAction.faceCount; i++) {
//                    STPoint[] stPoints = humanAction.getMobileFaces()[i].getPoints_array();
//                    point[i][LEFT_EYE_X] = stPoints[STHumanAction.POS_LEFT_EYE].getX() / width;
//                    point[i][LEFT_EYE_Y] = stPoints[STHumanAction.POS_LEFT_EYE].getY() / height;
//                    point[i][RIGHT_EYE_X] = stPoints[STHumanAction.POS_RIGHT_EYE].getX() / width;
//                    point[i][RIGHT_EYE_Y] = stPoints[STHumanAction.POS_RIGHT_EYE].getY() / height;
//                    point[i][MOUTH_X] = stPoints[STHumanAction.POS_MOUTH].getX() / width;
//                    point[i][MOUTH_Y] = stPoints[STHumanAction.POS_MOUTH].getY() / height;
//                }
                point = new float[1][6];
                if (effect != null) {
                    recorder.setFaces(point);
                }
            }

            @Override
            public void openFailed() {
                isOpenFailed = true;
            }
        });

        recorder.setRecordCallBack(new RecordCallback() {

            @Override
            public void onComplete(boolean validClip,long clipDuration) {
                Log.d("QuCore", "call onComplete");
                handleStopCallback();
            }

            @Override
            public void onFinish(String outputPath) {
                clipManager.deleteAllPart();
            }

            @Override
            public void onProgress(final long duration) {
                Log.d("CameraDemo","duration..."+ duration);
                recordTime = (int) duration;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (recordStopped) {
                            return;
                        }
                        int progress = (int) ((float) duration/ clipManager.getMaxDuration() * 100);
                        if (progress >= 100) {
                            recorder.stopRecording();
                            handleRecordStop();
                            return;
                        }
                        fanProgressBar.setProgress(progress);
                        String result = String.format("%.2f", duration / 1000f);
                        recordDuration.setText(result);
                    }
                });
            }

            @Override
            public void onMaxDuration() {

            }

            @Override
            public void onError(int errorCode) {
                if(errorCode == AliyunErrorCode.ERROR_LICENSE_FAILED){
                    // TODO: 2017/2/17
                }
                handleStopCallback();
            }

            @Override
            public void onInitReady() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(effect != null){
                            addEffectToRecord(effect.getPath());     //因为底层在onpause的时候会做资源回收，所以初始化完成的时候要做资源的恢复
                        }
                    }
                });
            }

            @Override
            public void onPictureBack(final Bitmap bitmap) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ImageView)findViewById(R.id.test)).setImageBitmap(bitmap);
                    }
                });

            }

            @Override
            public void onPictureDataBack(byte[] data) {

            }
        });

        recorder.setOnTextureIdCallback(new OnTextureIdCallBack() {
            @Override
            public int onTextureIdBack(int textureId,int textureWidth,int textureHeight,float[] matrix) {
                //if(test == null){
                //    test = new OpenGLTest();
                //}
                //if(isUseNative){
                //    return textureId;
                //}else{
                //    int testId = test.renderWithTexture(textureId,textureWidth, textureHeight, matrix);
                //    return testId;
                //}
                return textureId;

            }
        });
        switchLightBtnState();
    }
    boolean isUseNative = true;
    //OpenGLTest test;

//    private float[][] parsePoints(String result,int width,int height){
//
//        float[][] points = new float[1][6];
//        try {
//            JSONObject jsonResult = new JSONObject(result);
//            JSONArray faces = jsonResult.optJSONArray("face");han
//            if(faces == null || faces.length() == 0){
//                return points;
//            }
//            points = new float[faces.length()][6];
//            for(int i = 0 ; i < points.length; i++){
//                JSONObject face = faces.getJSONObject(i).getJSONObject("landmark");
//                if(face == null){
//                    continue;
//                }
//                JSONObject leftEye = face.optJSONObject(ParseResult.POS_LEFT_EYE);
//                if(leftEye != null){
//                    points[i][0] = (float) (leftEye.optDouble("x") / width);
//                    points[i][1] = (float) (leftEye.optDouble("y") / height);
//                }
//                JSONObject rightEye = face.optJSONObject(ParseResult.POS_RIGHT_EYE);
//                if(rightEye != null){
//                    points[i][2] = (float) (rightEye.optDouble("x")/width);
//                    points[i][3] = (float) (rightEye.optDouble("y")/height);
//                }
//                JSONObject mouth = face.optJSONObject(ParseResult.POS_MOUTH);
//                if(mouth != null){
//                    points[i][4] = (float) (mouth.optDouble("x")/width);
//                    points[i][5] = (float) (mouth.optDouble("y")/height);
//                }
//
//            }
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return points;
//    }

    private int getOrientation(int rotate) {
        int dir = 0;
        switch (rotate) {
            case 0:
                dir = STRotateType.ST_CLOCKWISE_ROTATE_0;
                break;
            case 90:
                dir = STRotateType.ST_CLOCKWISE_ROTATE_90;
                break;
            case 180:
                dir = STRotateType.ST_CLOCKWISE_ROTATE_180;
                break;
            case 270:
                dir = STRotateType.ST_CLOCKWISE_ROTATE_270;
                break;
        }
        return dir;
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
        if (cameraType == CameraType.FRONT) {
            if (rotation != 0) {
                rotation = 360 - rotation;
            }
        }
        Log.d("MyOrientationDetector","generated rotation ..."+rotation);
        return rotation;
    }

    private void initPasterResource() {
        if (CommonUtil.hasNetwork(this)) {
            initPasterResourceOnLine();
        } else {
            initPasterResourceLocal();
        }
    }

    private void addConstantPaster(){
        String path = Common.QU_DIR + "maohuzi";
        File file = new File(path);
        File iconFile = new File(path +"/icon.png");
        if(file.exists() && iconFile.exists()){
            PreviewPasterForm form = new PreviewPasterForm();
            form.setUrl(file.getAbsolutePath());
            form.setIcon(file.getAbsolutePath() + File.separator + "icon.png");
            form.setLocalRes(true);
            resources.add(0,form);
        }

    }

    private void initPasterResourceLocal() {
        File aseetFile = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File[] files = null;
        if (aseetFile.isDirectory()) {
            files = aseetFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory() && pathname.getName().contains("-")) {
                        return true;
                    }
                    return false;
                }
            });
        }
        if (files == null) {
            return;
        }
        for (File file : files) {
            PreviewPasterForm form = new PreviewPasterForm();
            form.setIcon(file.getAbsolutePath() + File.separator + "icon.png");
            String fileName = file.getName();
            String[] strs = fileName.split("-");
            if (strs.length == 2) {
                String name = strs[0];
                String id = strs[1];
                form.setName(name);
                form.setUrl(getLocalResUrl(id));
                try {
                    form.setId(Integer.parseInt(id));
                    resources.add(form);
                } catch (Exception e) {
                    continue;
                }
            } else {
                continue;
            }
        }
        addConstantPaster();
        initPasterView();
    }

    private void initPasterResourceOnLine() {
        String api = Common.BASE_URL + "/api/res/prepose";
        String category = "?bundleId="+getApplicationInfo().packageName;
        Logger.getDefaultLogger().d("pasterUrl url = " + api + category);
        HttpRequest.get(api + category,
                new StringHttpRequestCallback() {
                    @Override
                    protected void onSuccess(String s) {
                        super.onSuccess(s);
                        JSONSupportImpl jsonSupport = new JSONSupportImpl();

                        try {
                            List<PreviewResourceForm> resourceList = jsonSupport.readListValue(s,
                                    new TypeToken<List<PreviewResourceForm>>(){}.getType());
                            if (resourceList != null && resourceList.size() > 0) {
                                for(int i = 0; i < resourceList.size();i++){
                                    resources.addAll(resourceList.get(i).getPasterList());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        addConstantPaster();
                        initPasterView();
                    }

                    @Override
                    public void onFailure(int errorCode, String msg) {
                        super.onFailure(errorCode, msg);
                    }
                });
    }

    private void initPasterView() {
        if (resources != null) {
            fillItemBlank();
            adapter = new PasterAdapter(CameraDemo.this, resources, itemWidth);
            pasterView.setAdapter(adapter);
            adapter.setOnitemClickListener(new PasterAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    pasterView.smoothScrollToPosition(position);
                }
            });
            linearLayoutManager = new CenterLayoutManager(CameraDemo.this, LinearLayoutManager.HORIZONTAL, false);
            pasterView.setLayoutManager(linearLayoutManager);
        }
    }

    private void copyAssets() {
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Common.copyAll(CameraDemo.this);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                resCopy.setVisibility(View.GONE);
                initPasterResource();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fillItemBlank() {
        for (int i = 0; i < MAX_ITEM_COUNT / 2; i++) {
            resources.add(0, new PreviewPasterForm());
            resources.add(new PreviewPasterForm());
        }
        resources.add(0, new PreviewPasterForm());
    }

    private void initView() {
        OUT_STROKE_WIDTH = DensityUtil.dip2px(10);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.preview);
        glSurfaceView.setOnTouchListener(this);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) glSurfaceView.getLayoutParams();
        Rect rect = new Rect();
        getWindowManager().getDefaultDisplay().getRectSize(rect);
        layoutParams.width = rect.width();
        layoutParams.height = rect.height();
        glSurfaceView.setLayoutParams(layoutParams);
        switchCameraBtn = (ImageView) findViewById(R.id.switch_camera);
        switchCameraBtn.setOnClickListener(this);
        switchLightBtn = (ImageView) findViewById(R.id.switch_light);
        switchLightBtn.setOnClickListener(this);
        pasterView = (RecyclerView) findViewById(R.id.pasterView);
        pasterView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                pasterView.setVisibility(View.INVISIBLE);
                pasterView.setVisibility(View.VISIBLE);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (effect != null) {
                        recorder.removePaster(effect);
                        effect = null;
                    }
                    View centerView = linearSnapHelper.findSnapView(linearLayoutManager);
                    if (centerView.getTag() != null && centerView.getTag() instanceof PreviewPasterForm) {
                        currentPasterForm = (PreviewPasterForm) centerView.getTag();
                        if (currentPasterForm != null && !currentPasterForm.getUrl().isEmpty()) {
                            addPasterResource(currentPasterForm);
                        }
                    }else{

                    }
                }else if(newState == RecyclerView.SCROLL_STATE_DRAGGING){
                    currentPasterForm = null;
                    progressBar.setVisibility(View.GONE);
                }
            }

        });
        fanProgressBar = (FanProgressBar) findViewById(R.id.record_progress);
        progressBar = (CircleProgressBar) findViewById(R.id.download_progress);
        recordBg = (FrameLayout) findViewById(R.id.record_bg);
        recordBg.setOnTouchListener(this);
        resCopy = (FrameLayout) findViewById(R.id.copy_res_tip);
        linearSnapHelper = new LinearSnapHelper();
        linearSnapHelper.attachToRecyclerView(pasterView);
        calculateItemWidth();
        fanProgressBar.setOutRadius(itemWidth / 2 - OUT_STROKE_WIDTH / 2);
        fanProgressBar.setOffset(OUT_STROKE_WIDTH / 2, OUT_STROKE_WIDTH / 2);
        fanProgressBar.setOutStrokeWidth(OUT_STROKE_WIDTH);
        FrameLayout.LayoutParams recordBgLp = (FrameLayout.LayoutParams) recordBg.getLayoutParams();
        recordBgLp.width = itemWidth;
        recordBgLp.height = itemWidth;
        recordBg.setLayoutParams(recordBgLp);
        FrameLayout.LayoutParams downloadLp = (FrameLayout.LayoutParams) progressBar.getLayoutParams();
        downloadLp.width = itemWidth;
        downloadLp.height = itemWidth;
        progressBar.setLayoutParams(downloadLp);
        progressBar.setBackgroundWidth(itemWidth, itemWidth);
        progressBar.setProgressWidth(itemWidth - DensityUtil.dip2px(this, 20));
        progressBar.isFilled(true);
        backBtn = (ImageView) findViewById(R.id.back);
        backBtn.setOnClickListener(this);
        recordDuration = (TextView) findViewById(R.id.record_duration);
        recordDuration.setVisibility(View.GONE);
        filterTxt = (TextView) findViewById(R.id.filter_txt);
        filterTxt.setVisibility(View.GONE);
        gestureDetector = new GestureDetector(this, this);
        scaleGestureDetector = new ScaleGestureDetector(this, this);
    }

    private void calculateItemWidth() {
        itemWidth = getResources().getDisplayMetrics().widthPixels / MAX_ITEM_COUNT;
    }

    private void addPasterResToLocal(String url, String id) {
        SharedPreferences sharedPreferences = getSharedPreferences(LOCAL_SETTING_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit().putString(id, url);
        editor.commit();
    }

    private String getLocalResUrl(String id) {
        SharedPreferences sharedPreferences = getSharedPreferences(LOCAL_SETTING_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(id, "");
    }

    private void addPasterResource(final PreviewPasterForm pasterForm) {
        if (pasterForm != null && !pasterForm.getIcon().isEmpty()) {
            if ((DownloadFileUtils.isPasterExist(this, pasterForm.getName(), pasterForm.getId()) && !getLocalResUrl(String.valueOf(pasterForm.getId())).isEmpty()) || pasterForm.isLocalRes()) {
                String path;
                if(pasterForm.isLocalRes()){
                    path = pasterForm.getUrl();
                }else{
                    path = DownloadFileUtils.getAssetPackageDir(this,
                            pasterForm.getName(), pasterForm.getId()).getAbsolutePath();
                }
                Logger.getDefaultLogger().d("faces add downloaded res ..." + path);
                addEffectToRecord(path);
            } else {

                FileDownloaderModel fileDownloaderModel = new FileDownloaderModel();
                fileDownloaderModel.setUrl(pasterForm.getUrl());
                fileDownloaderModel.setPath(DownloadFileUtils.getAssetPackageDir(this,
                        pasterForm.getName(), pasterForm.getId()).getAbsolutePath());
                fileDownloaderModel.setId(fileDownloaderModel.getId());
                fileDownloaderModel.setIsunzip(1);

                final FileDownloaderModel model = DownloaderManager.getInstance().addTask(fileDownloaderModel, fileDownloaderModel.getUrl());
                if(DownloaderManager.getInstance().isDownloading(model.getTaskId(), model.getPath())){
                    return;
                }
                DownloaderManager.getInstance().startTask(model.getTaskId(), new FileDownloaderCallback() {
                    @Override
                    public void onProgress(int downloadId, long soFarBytes, long totalBytes, long speed, int progress) {
                        if (pasterForm == currentPasterForm) {
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setProgress(progress);
                        }else{
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFinish(int downloadId, String path) {
                        Log.e("faces", "onItemDownloadCompleted ...");
                        progressBar.setVisibility(View.GONE);
                        File file = new File(path);
                        if(!file.exists() || !file.isDirectory()) {
                            return;
                        }
                        addPasterResToLocal(pasterForm.getUrl(), String.valueOf(pasterForm.getId()));
                        if (pasterForm == currentPasterForm) {
                            addEffectToRecord(path);
                        }
                    }

                    @Override
                    public void onError(BaseDownloadTask task, Throwable e) {
                        ToastUtil.showToast(CameraDemo.this, R.string.network_not_connect);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void addEffectToRecord(String path) {
        if(new File(path).exists()){
            if(effect != null) {
                recorder.removePaster(effect);
            }
            effect = new EffectPaster(path);
            recorder.addPaster(effect);
        }
//        if(((EffectPaster)effect).isPasterReady()){
//            recorder.addPaster(effect);
//            recorder.setViewSize(((EffectPaster) effect).getWidth() / (float) glSurfaceView.getWidth(),
//                    ((EffectPaster) effect).getHeight() / (float) glSurfaceView.getHeight(), effect);
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        recorder.startPreview();
        recorder.setZoom(scaleFactor);
        if (orientationDetector != null && orientationDetector.canDetectOrientation()) {
            orientationDetector.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorder.stopPreview();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orientationDetector != null) {
            orientationDetector.disable();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recorder.destroy();
        mSTHumanActionNative.destroyInstance();
        AliyunRecorderCreator.destroyRecorderInstance();
        if (orientationDetector != null) {
            orientationDetector.setOrientationChangedListener(null);
        }
       
        Log.d(TAG, "face detect task onDestroy");
    }

    private void switchLightBtnState() {
        if (cameraType == CameraType.FRONT) {
            switchLightBtn.setVisibility(View.INVISIBLE);
        } else if (cameraType == CameraType.BACK) {
            switchLightBtn.setVisibility(View.VISIBLE);
        }
    }

    private void switchBeauty() {
        if (cameraType == CameraType.FRONT) {
            recorder.setBeautyStatus(true);
        } else if (cameraType == CameraType.BACK) {
            recorder.setBeautyStatus(false);
        }
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

    private void recordBtnScale(float scaleRate) {
        FrameLayout.LayoutParams recordBgLp = (FrameLayout.LayoutParams) recordBg.getLayoutParams();
        recordBgLp.width = (int) (itemWidth * scaleRate);
        recordBgLp.height = (int) (itemWidth * scaleRate);
        recordBg.setLayoutParams(recordBgLp);
        FrameLayout.LayoutParams fanProgressBarBgLp = (FrameLayout.LayoutParams) fanProgressBar.getLayoutParams();
        fanProgressBarBgLp.width = (int) (itemWidth * scaleRate);
        fanProgressBarBgLp.height = (int) (itemWidth * scaleRate);
        fanProgressBar.setLayoutParams(fanProgressBarBgLp);
        fanProgressBar.setOffset((int) ((OUT_STROKE_WIDTH + itemWidth * (scaleRate - 1)) / 2), (int) ((OUT_STROKE_WIDTH + itemWidth * (scaleRate - 1)) / 2));
        fanProgressBar.setOutRadius((int) (itemWidth * scaleRate - OUT_STROKE_WIDTH) / 2);
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

    @Override
    public void onClick(View v) {
        if (v == switchCameraBtn) {
            int type = recorder.switchCamera();
            if (type == CameraType.BACK.getType()) {
                cameraType = CameraType.BACK;
            } else if (type == CameraType.FRONT.getType()) {
                cameraType = CameraType.FRONT;
            }
            switchLightBtnState();
        } else if (v == switchLightBtn) {
            if (flashType == FlashType.OFF) {
                flashType = FlashType.AUTO;
            } else if (flashType == FlashType.AUTO) {
                flashType = FlashType.ON;
            } else if (flashType == FlashType.ON) {
                flashType = FlashType.OFF;
            }
            switch (flashType) {
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
            recorder.setLight(flashType);
        } else if (v == backBtn) {
            finish();
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {

        //if(isUseNative){
        //    isUseNative = false;
        //}else{
        //    isUseNative = true;
        //}
        return false;
    }


    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        recorder.setFocus(null);
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

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
        recorder.applyFilter(effectFilter);
        showFilter(effectFilter.getName());
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (v == glSurfaceView) {
            gestureDetector.onTouchEvent(event);
            scaleGestureDetector.onTouchEvent(event);
        } else if (v == recordBg) {
            if(isOpenFailed){
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                downTime = System.currentTimeMillis();
                if (v.isActivated()) {
                    return false;
                } else {
                    if(CommonUtil.SDFreeSize() < 50 * 1000 *1000){
                        Toast.makeText(this, R.string.no_free_memory, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    videoPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator + System.currentTimeMillis() + ".mp4";
                    recorder.setOutputPath(videoPath);
                    int tempRotation = getPictureRotation();
                    if(tempRotation == 90 || tempRotation == 270){
                        recordRotation = (getPictureRotation() + 90) % 360;
                        if(cameraType == CameraType.BACK){
                            recordRotation += 180;
                            recordRotation %= 360;
                        }
                    }else if(tempRotation == 0 || tempRotation == 180){
                        recordRotation = (getPictureRotation() + 270) % 360;
                    }

                    recorder.setRotation(recordRotation);
                    recorder.startRecording();
                    if (flashType == FlashType.ON && cameraType == CameraType.BACK) {
                        recorder.setLight(FlashType.TORCH);
                    }
                    handleRecordStart();
                }
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                if(System.currentTimeMillis() - downTime <  500){
                    //recorder.takePhoto(true);
                }
                recorder.stopRecording();
                handleRecordStop();
            }
        }
        return true;
    }

    private void handleStopCallback() {
        recorder.finishRecording();
        Log.d("QuCore", "XXXXX handleStopCallback");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recordTime < clipManager.getMinDuration()) {
                    deleteVideoFile();
                } else {
                    Intent intent = new Intent(CameraDemo.this, VideoPlayActivity.class);
                    intent.putExtra(VideoPlayActivity.VIDEO_PATH, videoPath);
                    intent.putExtra(VideoPlayActivity.VIDEO_ROTATION, recordRotation);
                    startActivity(intent);

                }
                Log.d("QuCore", "XXXXX recorfBg.setActivated false");
                recordBg.setActivated(false);
            }
        });
    }

    private void handleRecordStop() {
        recordStopped = true;
        recordBtnScale(1f);
        fanProgressBar.setProgress(0);
        pasterView.setVisibility(View.VISIBLE);
        recordDuration.setVisibility(View.GONE);
        if (flashType == FlashType.ON &&cameraType == CameraType.BACK) {
            recorder.setLight(FlashType.OFF);
        }

    }

    private void handleRecordStart() {
        recordTime = 0;
        recordDuration.setText("");
        recordBtnScale(1.2f);
        recordBg.setActivated(true);
        pasterView.setVisibility(View.INVISIBLE);
        recordDuration.setVisibility(View.VISIBLE);
        recordStopped = false;
    }

    private void deleteVideoFile() {
        File file = new File(videoPath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        Log.e(TAG, "factor..." + detector.getScaleFactor());
        float factorOffset = detector.getScaleFactor() - lastScaleFactor;
        scaleFactor += factorOffset;
        lastScaleFactor = detector.getScaleFactor();
        if (scaleFactor < 0) {
            scaleFactor = 0;
        }
        if (scaleFactor > 1) {
            scaleFactor = 1;
        }
        recorder.setZoom(scaleFactor);
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

    public int getVirtualBarHeigh() {
        int vh = 0;
        WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
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


}
