/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.editor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.common.project.PasterDescriptor;
import com.aliyun.common.utils.DensityUtil;
import com.aliyun.common.utils.FileUtils;
import com.aliyun.common.utils.StorageUtils;
import com.aliyun.common.utils.ToastUtil;
import com.aliyun.demo.editor.timeline.TimelineBar;
import com.aliyun.demo.effects.control.BottomAnimation;
import com.aliyun.demo.effects.control.EditorService;
import com.aliyun.demo.effects.control.EffectInfo;
import com.aliyun.demo.effects.control.OnDialogButtonClickListener;
import com.aliyun.demo.effects.control.OnEffectChangeListener;
import com.aliyun.demo.effects.control.OnTabChangeListener;
import com.aliyun.demo.effects.control.TabGroup;
import com.aliyun.demo.effects.control.TabViewStackBinding;
import com.aliyun.demo.effects.control.UIEditorPage;
import com.aliyun.demo.effects.control.ViewStack;
import com.aliyun.demo.util.Common;
import com.aliyun.demo.widget.AliyunPasterWithImageView;
import com.aliyun.demo.widget.AliyunPasterWithTextView;
import com.aliyun.qupai.editor.AliyunIEditor;
import com.aliyun.qupai.editor.AliyunIExporter;
import com.aliyun.qupai.editor.AliyunIPlayer;
import com.aliyun.qupai.editor.AliyunPasterBaseView;
import com.aliyun.qupai.editor.AliyunPasterController;
import com.aliyun.qupai.editor.AliyunPasterManager;
import com.aliyun.qupai.editor.AliyunPasterRender;
import com.aliyun.qupai.editor.OnComposeCallback;
import com.aliyun.qupai.editor.OnPasterRestored;
import com.aliyun.qupai.editor.OnPasterResumeAndSave;
import com.aliyun.qupai.editor.OnPlayCallback;
import com.aliyun.qupai.editor.OnPreparedListener;
import com.aliyun.qupai.editor.impl.AliyunEditorFactory;
import com.aliyun.struct.common.AliyunVideoParam;
import com.aliyun.struct.common.ScaleMode;
import com.aliyun.struct.common.VideoDisplayMode;
import com.aliyun.struct.effect.EffectBean;
import com.aliyun.struct.effect.EffectPaster;
import com.aliyun.struct.effect.EffectText;

import java.io.File;
import java.util.List;

/**
 * Created by apple on 2017/3/1.
 */


public class EditorActivity extends AppCompatActivity implements
        OnTabChangeListener, OnEffectChangeListener, BottomAnimation, View.OnClickListener {
    private static final String TAG = "EditorActivity";
    public static final String KEY_VIDEO_PARAM = "video_param";
    public static final String KEY_PROJECT_JSON_PATH = "project_json_path";

    private LinearLayout mBottomLinear;
    private SurfaceView mSurfaceView;
    private TabGroup mTabGroup;
    private ViewStack mViewStack;//底部5个button的view堆栈
    private EditorService mEditorService;

    private AliyunIEditor mAliyunIEditor;
    private AliyunIPlayer mAliyunIPlayer;
    private AliyunPasterManager mPasterManager;
    private AliyunPasterRender mAliyunPasterRender;
    private RecyclerView mThumbnailView;
    private TimelineBar mTimelineBar;
    private RelativeLayout mActionBar;
    private FrameLayout resCopy;

    private FrameLayout mPasterContainer;
    private FrameLayout mGlSurfaceContainer;
    private Uri mUri;
    private int mScreenWidth;
    private ImageView mIvLeft;
    private ImageView mIvRight;
    private TextView mTvCenter;
    private LinearLayout mBarLinear;
    private ImageView mPlayImage;
    private TextView mTvCurrTime;
    private AliyunVideoParam mVideoParam;
    private boolean mIsComposing = false; //当前是否正在合成视频
    private boolean isFullScreen = true; //导入视频是否全屏显示
    private ProgressDialog dialog;
    private MediaScannerConnection mMediaScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        copyAssets();
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        mScreenWidth = point.x;
        setContentView(R.layout.activity_editor);
        Intent intent = getIntent();

        //获取传递过来的 project_json_path，mVideoParam
        if (intent.getStringExtra(KEY_PROJECT_JSON_PATH) != null) {
            mUri = Uri.fromFile(new File(intent.getStringExtra(KEY_PROJECT_JSON_PATH)));
        }
        if (intent.getSerializableExtra(KEY_VIDEO_PARAM) != null) {
            mVideoParam = (AliyunVideoParam) intent.getSerializableExtra(KEY_VIDEO_PARAM);
        }
        initView();
        initListView();
        add2Control();
        initEditor();
        mMediaScanner = new MediaScannerConnection(this, null);
        mMediaScanner.connect();

    }

    private void initView() {
        resCopy = (FrameLayout) findViewById(R.id.copy_res_tip);
        mBarLinear = (LinearLayout) findViewById(R.id.bar_linear);
        mBarLinear.bringToFront();
        mActionBar = (RelativeLayout) findViewById(R.id.action_bar);
        mIvLeft = (ImageView) findViewById(R.id.iv_left);
        mTvCenter = (TextView) findViewById(R.id.tv_center);
        mIvRight = (ImageView) findViewById(R.id.iv_right);
        mIvLeft.setImageResource(R.mipmap.icon_back);
        mTvCenter.setText(getString(R.string.edit_nav_edit));
        mIvRight.setImageResource(R.mipmap.icon_next);
        mIvLeft.setVisibility(View.VISIBLE);
        mIvRight.setVisibility(View.VISIBLE);
        mTvCenter.setVisibility(View.VISIBLE);
        mIvLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mTvCurrTime = (TextView) findViewById(R.id.tv_curr_duration);

        mGlSurfaceContainer = (FrameLayout) findViewById(R.id.glsurface_view);
        mSurfaceView = (SurfaceView) findViewById(R.id.play_view);
        mBottomLinear = (LinearLayout) findViewById(R.id.edit_bottom_tab);

        mPasterContainer = (FrameLayout) findViewById(R.id.pasterView);

        mPlayImage = (ImageView) findViewById(R.id.play_button);
        mPlayImage.setOnClickListener(this);

        final GestureDetector gesture = new GestureDetector(this,
                new MyOnGestureListener());
        mPasterContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gesture.onTouchEvent(event);
                return true;
            }
        });

        final GestureDetector mGesture = new GestureDetector(this,
                new MyOnGestureListener());
        View.OnTouchListener pasterTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGesture.onTouchEvent(event);
                return true;
            }
        };

        mPasterContainer.setOnTouchListener(pasterTouchListener);//去掉手势滑动
    }

    private void initGlSurfaceView() {
        if (mAliyunIPlayer != null) {
            if (mVideoParam == null) {
                return;
            }
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mGlSurfaceContainer.getLayoutParams();
            int rotation = mAliyunIPlayer.getRotation();
            int outputWidth = mVideoParam.getOutputWidth();
            int outputHeight = mVideoParam.getOutputHeight();
            if(mVideoParam.getOutputWidth()>mVideoParam.getOutputHeight()){
                outputWidth = mVideoParam.getOutputHeight();
                outputHeight = mVideoParam.getOutputWidth();
            }

//            rotation == 0；现在做的写死了在 recorderDEMO

            if (rotation == 90 || rotation == 270) {
                int temp = outputWidth;
                outputWidth = outputHeight;
                outputHeight = temp;
            }
            float percent;
            if (outputWidth >= outputHeight) {
                percent = (float) outputWidth / outputHeight;
            } else {
                percent = (float) outputHeight / outputWidth;
            }
            if (percent < 1.5 || rotation == 90 || rotation == 270) {
                layoutParams.height = Math.round((float) outputHeight * mScreenWidth / outputWidth);
                layoutParams.addRule(RelativeLayout.BELOW, R.id.bar_linear);
            } else {
                layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                isFullScreen = true;
                mBottomLinear.setBackgroundColor(getResources().getColor(R.color.tab_bg_color_50pct));
                mActionBar.setBackgroundColor(getResources().getColor(R.color.action_bar_bg_50pct));
//            }
            mGlSurfaceContainer.setLayoutParams(layoutParams);
        }
    }}

    private void initListView() {
        mEditorService = new EditorService();
        mTabGroup = new TabGroup();
        mViewStack = new ViewStack(this);//底部5个button的view堆栈
        mViewStack.setEditorService(mEditorService);
        mViewStack.setEffectChange(this);//各种效果改变后 监听
        mViewStack.setBottomAnimation(this);
        mViewStack.setDialogButtonClickListener(mDialogButtonClickListener);

        //添加底部的5个 butn
        mTabGroup.addView(findViewById(R.id.tab_effect_filter));
        mTabGroup.addView(findViewById(R.id.tab_effect_overlay));
        mTabGroup.addView(findViewById(R.id.tab_effect_caption));
        mTabGroup.addView(findViewById(R.id.tab_effect_mv));
        mTabGroup.addView(findViewById(R.id.tab_effect_audio_mix));
    }

    private void add2Control() {
        //底部5个布局的绑定
        TabViewStackBinding tabViewStackBinding = new TabViewStackBinding();
        tabViewStackBinding.setViewStack(mViewStack);
        //tab切换mTabGroup 添加点击事件
        mTabGroup.setOnCheckedChangeListener(tabViewStackBinding);
        mTabGroup.setOnTabChangeListener(this);
    }

    private void initEditor() {
        //视频编辑 工具
        mAliyunIEditor = AliyunEditorFactory.creatAliyunEditor(mUri);
        mAliyunIEditor.init(mSurfaceView);
        mAliyunIPlayer = mAliyunIEditor.createAliyunPlayer(); //视频播放 工具

        if (mAliyunIPlayer == null) {
            ToastUtil.showToast(this, "Create AliyunPlayer failed");
            finish();
            return;
        }
        initGlSurfaceView();
        mEditorService.setFullScreen(isFullScreen);
        mEditorService.addTabEffect(UIEditorPage.MV, mAliyunIEditor.getMVLastApplyId());
        mEditorService.addTabEffect(UIEditorPage.FILTER_EFFECT, mAliyunIEditor.getFilterLastApplyId());
        mEditorService.addTabEffect(UIEditorPage.AUDIO_MIX, mAliyunIEditor.getMusicLastApplyId());//音乐
        mAliyunIPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared() {
                ScaleMode mode = mVideoParam.getScaleMode();
                if (mode != null) {
                    switch (mode) {
                        case LB:
                            mAliyunIPlayer.setDisplayMode(VideoDisplayMode.SCALE);
                            break;
                        case PS:
                            mAliyunIPlayer.setDisplayMode(VideoDisplayMode.FILL);
                            break;
                    }
                }
                if (mTimelineBar == null) {//时间轴
                    mTimelineBar = new TimelineBar(
                            mAliyunIPlayer.getDuration(),
                            DensityUtil.dip2px(EditorActivity.this, 50),
                            new TimelineBar.TimelinePlayer() {
                                @Override
                                public long getCurrDuration() {
                                    return mAliyunIPlayer.getCurrentPosition();
                                }
                            });
                    mTimelineBar.setThumbnailView(new TimelineBar.ThumbnailView() {
                        @Override
                        public RecyclerView getThumbnailView() {
                            return mThumbnailView;
                        }

                        @Override
                        public ViewGroup getThumbnailParentView() {
                            return (ViewGroup) mThumbnailView.getParent();
                        }

                        @Override
                        public void updateDuration(long duration) {
                            mTvCurrTime.setText(convertDuration2Text(duration));
                        }
                    });
                    ViewGroup.MarginLayoutParams layoutParams =
                            (ViewGroup.MarginLayoutParams) mThumbnailView.getLayoutParams();
                    layoutParams.width = mScreenWidth;
                    mTimelineBar.setTimelineBarDisplayWidth(mScreenWidth);
                    mTimelineBar.setBarSeekListener(new TimelineBar.TimelineBarSeekListener() {//mTimelineBar滑动事件
                        @Override
                        public void onTimelineBarSeek(long duration) {
                            Log.e(TAG, "onTimelineBarSeek duration..." + duration);
                            mAliyunIPlayer.seek(duration);
                            mTimelineBar.pause();
                            mPlayImage.setSelected(true);
                            mPlayImage.setEnabled(false);
                            Log.d(TimelineBar.TAG, "OnTimelineSeek duration = " + duration);
                            if (currentEdit != null
                                    && !currentEdit.isEditCompleted()) {
                                if (!currentEdit.isVisibleInTime(duration)) {//隐藏
                                    currentEdit.mPasterView.setVisibility(View.GONE);
                                } else {//显示
                                    currentEdit.mPasterView.setVisibility(View.VISIBLE);
                                }
                            }
                        }

                        @Override
                        public void onTimelineBarSeekFinish(long duration) {
                            Log.e(TAG, "onTimelineBarSeekFinish duration..." + duration);
                            mAliyunIPlayer.seek(duration);
                            mTimelineBar.pause();
                            mPlayImage.setSelected(true);
                        }
                    });
                }
//                if (!mAliyunIPlayer.isPlaying()) {//播放视频
//                    mAliyunIPlayer.start();
//                }
//                mTimelineBar.start();//开始滑动
                mPasterManager.setDisplaySize(mPasterContainer.getWidth(),
                        mPasterContainer.getHeight());
                mPasterManager.setOnPasterRestoreListener(mOnPasterRestoreListener);//贴图手势回调
                addMusic();//添加默认音乐
            }
        });

        mAliyunIPlayer.setOnPlayCallbackListener(new OnPlayCallback() {//播放视频 回调

            @Override
            public void onPlayStarted() {
                if (mTimelineBar.isPausing() && !mIsComposing) {
                    mTimelineBar.resume();
                }
                //添加水印
                File waterMark = new File(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunEditorDemo/tail/logo.png");
                if (waterMark.exists()) {
                    Bitmap wmBitmap = BitmapFactory.decodeFile(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunEditorDemo/tail/logo.png");
                    if (wmBitmap != null) {
                        /**
                         * 水印例子 水印的大小为 ：水印图片的宽高和显示区域的宽高比，注意保持图片的比例，不然显示不完全  水印的位置为 ：以水印图片中心点为基准，显示区域宽高的比例为偏移量，0,0为左上角，1,1为右下角
                         */
                        mAliyunIEditor.applyWaterMark(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunEditorDemo/tail/logo.png",
                                (float) wmBitmap.getWidth() * 0.5f * 0.8f / mSurfaceView.getWidth(),
                                (float) wmBitmap.getHeight() * 0.5f * 0.8f / mSurfaceView.getHeight(),
                                1f - (float) wmBitmap.getWidth() / 1.5f / mSurfaceView.getWidth() / 2,
                                0f + (float) wmBitmap.getHeight() / 1.5f / mSurfaceView.getHeight() / 2);
                    }
                }
            }

            @Override
            public void onError() {
                ToastUtil.showToast(EditorActivity.this, R.string.play_video_error);
                mAliyunIPlayer.stop();
            }

            @Override
            public void onSeekDone() {
                mPlayImage.setEnabled(true);//播放按钮
            }

            @Override
            public void onPlayCompleted() {
                //重播时必须先掉stop，再调用start
                mAliyunIPlayer.stop();
                mAliyunIPlayer.start();
                mTimelineBar.restart();
                Log.d(TimelineBar.TAG, "TailView play restart");
            }
        });

        mPasterManager = mAliyunIEditor.createPasterManager();//贴图管理器
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        final int height = dm.heightPixels;
        final int width = dm.widthPixels;
        mPasterManager.setDisplaySize(width,height);//设置坐标系长宽

        mAliyunPasterRender = mAliyunIEditor.getPasterRender();
        mThumbnailView = (RecyclerView) findViewById(R.id.rv_thumbnail);//视频的截图显示
        mThumbnailView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mThumbnailView.setAdapter(new ThumbnailAdapter(8, mAliyunIEditor.getAliyunThumbnailFetcher(), mScreenWidth));//适配器，adapter与mAliyunIEditor.get
        mIvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//合成中
                dialog = new ProgressDialog(EditorActivity.this);
                dialog.setTitle("合成中");
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMax(100);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mIsComposing = false;
                        mAliyunIEditor.getExporter().cancel();
                    }
                });
                dialog.show();
                mTimelineBar.pause();
                AliyunIExporter exporter = mAliyunIEditor.getExporter();//获取导出
                File tailImg = new File(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunEditorDemo/tail/logo.png");
                if (tailImg.exists()) {//尾部水印
                    exporter.setTailWatermark(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunEditorDemo/tail/logo.png",
                            90.f / mSurfaceView.getMeasuredHeight(),
                            280.0f / mSurfaceView.getMeasuredWidth(),
                            0, 0);
                }
                long time = System.currentTimeMillis();
                final String path = "/mnt/sdcard/outputVideo" + time + ".mp4";
                exporter.startCompose(path, new OnComposeCallback() {//开始合成

                    @Override
                    public void onError() {
                        mIsComposing = false;
                        Log.e("COMPOSE", "compose error");
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "合成失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                        Log.e("COMPOSE", "compose finished");
                        dialog.dismiss();
                        if (mIsComposing) {
                            if (mMediaScanner != null) {
                                mMediaScanner.scanFile(path, "video/mp4");//合成后换成合成的再次播放
                            }
                            Toast.makeText(getApplicationContext(), "合成完成", Toast.LENGTH_SHORT).show();
                        } else {
                            FileUtils.deleteFile(path);
                        }
                        mAliyunIPlayer.start();
                        mTimelineBar.resume();
                        mIsComposing = false;
                    }

                    @Override
                    public void onProgress(int progress) {
                        Log.d(TAG, "compose progress " + progress);
                        dialog.setProgress(progress);
                    }
                });
                mIsComposing = true;
            }
        });


    }

    //贴图 回调
    private final OnPasterRestored mOnPasterRestoreListener = new OnPasterRestored() {

        @Override
        public void onPasterRestored(List<AliyunPasterController> controllers) {
//            for (AliyunPasterController c : controllers) {
//                if (!c.isPasterExists()) {
//                    continue;
//                }
//                if (c.getPasterType() == EffectPaster.PASTER_TYPE_GIF) {
//                    currentEdit = addPaster(c);
//                } else if (c.getPasterType() == EffectPaster.PASTER_TYPE_TEXT) {
//                    currentEdit = addSubtitle(c, true);
//                } else if (c.getPasterType() == EffectPaster.PASTER_TYPE_CAPTION) {
//                    currentEdit = addCaption(c);
//                }
//
//                currentEdit.showTimeEdit();
//                currentEdit.editTimeCompleted();
//                currentEdit.moveToCenter();
//            }
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
//        mAliyunIPlayer.resume();
//        mPlayImage.setSelected(false);
//        if (mTimelineBar != null) {
//            mTimelineBar.resume();
//        }
//        mAliyunIEditor.onResume();
        checkAndRemovePaster();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mAliyunIEditor.onPause();
        mAliyunIPlayer.pause();
        mTimelineBar.pause();
        mPlayImage.setSelected(true);
        if (dialog != null && dialog.isShowing()) {
            mIsComposing = false;
            dialog.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAliyunIPlayer != null) {
            mAliyunIEditor.onDestroy();
        }
        if (mTimelineBar != null) {
            mTimelineBar.stop();
        }
        if (mMediaScanner != null) {
            mMediaScanner.disconnect();
        }
    }

    @Override
    public void onTabChange() {
        //暂停播放
//        if (mAliyunIPlayer.isPlaying()) {
//            playingPause();
//        }

        //tab切换时通知
        hideBottomView();
        UIEditorPage index = UIEditorPage.get(mTabGroup.getCheckedIndex());
        int ix = mEditorService.getEffectIndex(index);
        switch (index) {
            case FILTER_EFFECT:
                break;
            case OVERLAY:
                break;
            default:
        }
        Log.e("editor", "====== onTabChange " + ix + " " + index);
    }

    public void addMusic() {
        addPaster();//添加贴图
        addText();//添加字

        EffectInfo effectInfo = new EffectInfo();
        effectInfo.type = UIEditorPage.AUDIO_MIX;//返回数据封装
        effectInfo.setPath(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunDemo/乌兰姑娘.mp3");
        effectInfo.isLocalMusic = true;
        effectInfo.musicWeight = 100;
        effectInfo.id = 0;
        mAliyunIEditor.applyMusicMixWeight(effectInfo.musicWeight);
        EffectBean effect = new EffectBean();
        effect.setId(effectInfo.id);
        effect.setPath(effectInfo.getPath());

        mAliyunIEditor.applyMusicMixWeight(effectInfo.musicWeight);
        mAliyunIEditor.applyMusic(effect);

        mTimelineBar.start();//开始滑动播放视频
//        if (!mAliyunIPlayer.isPlaying()) {//播放视频
//            mAliyunIPlayer.start();
//        }
//        mTimelineBar.stop();


//        mAliyunIPlayer.stop();
//        mAliyunIPlayer.start();
//        mTimelineBar.restart();
    }

    public void addText() {
        AliyunPasterController c = mPasterManager.addSubtitle("蒙古歌舞曲", null);
        c.setPasterStartTime(0);
        c.setPasterDuration(mAliyunIPlayer.getDuration());
        PasterUITextImpl textui = addSubtitle(c, true);
        if (currentEdit != null && !currentEdit.isPasterRemoved()) {
            currentEdit.editTimeCompleted();
        }
        textui.editTimeStart();
        textui.editTimeCompleted();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        final int height = dm.heightPixels;
        final int width = dm.widthPixels;
        final AliyunPasterController mController = mPasterManager.addSubtitle("表演者：someone", null);
        mController.setPasterStartTime(0);
        mController.setPasterDuration(mAliyunIPlayer.getDuration());
        mPasterManager.setDisplaySize(width,height);
        PasterUITextImpl2 textui2 = addSubtitle2(mController, false);
        textui2.moveToCenter();
        textui2.editTimeCompleted();


//        //添加静态图片
//        EffectPaster mEffectPaster = new EffectPaster("/storage/emulated/0/android.jpg");
//        mEffectPaster.width = 200;
//        mEffectPaster.height = 300;
//        mEffectPaster.x = 0;
//        mEffectPaster.y = 0;
//        mEffectPaster.start = 0;
//        mEffectPaster.rotation = 0;//旋转角度
//        mEffectPaster.end = mAliyunIPlayer.getDuration();
//
//        /**
//         * 设置贴图恢复和保存的回调，因为页面被系统回收后，渲染层资源已经释放，
//         * 之前添加的贴图资源也已经不存在，需要在恰当的时机保存当前已经使用的贴图资源，
//         * 当页面恢复时再恢复贴纸，贴图保存时会回调{@link OnPasterResumeAndSave}的onPasterSave方法
//         * 该回调的参数是贴图列表，因为sdk需要实现保存的贴图与屏幕无关，
//         * 所以使用了{@link PasterDescriptor}类来保存贴图到文件
//         * @param listener
//         */
//        mAliyunPasterRender.setOnPasterResumeAndSave(new OnPasterResumeAndSave() {
//
//            @Override
//            public void onPasterResume(List<PasterDescriptor> list) {
//                mlist = list;
//
//            }
//
//            @Override
//            public List<PasterDescriptor> onPasterSave(List<EffectPaster> list) {
//                mmlist = list;
//                return mlist;
//            }
//        });
//        Bitmap wmBitmap = BitmapFactory.decodeFile(StorageUtils.getCacheDirectory(EditorActivity.this) + "/AliyunEditorDemo/tail/logo.png");
//        mAliyunPasterRender.setDisplaySize(30,30);
//        int code = mAliyunPasterRender.addSubtitle(wmBitmap,new EffectText("/storage/emulated/0/android.jpg"));
//        mAliyunPasterRender.showTextPaster(wmBitmap,new EffectText("/storage/emulated/0/android.jpg"));
//        mAliyunPasterRender.addEffectPaster(mEffectPaster);
//        mAliyunPasterRender.showPaster(mEffectPaster);
    }
    List<PasterDescriptor> mlist ;
    List<EffectPaster> mmlist ;
    //  添加贴图
    public void addPaster() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        final int height = dm.heightPixels;
        final int width = dm.widthPixels;
        mPasterManager.setDisplaySize(width,height);

        EffectInfo kaitou = new EffectInfo();
        kaitou.type = UIEditorPage.OVERLAY;//图像
        kaitou.setPath("/storage/emulated/0/Android/data/cn.a218.newproject/cache/AliyunDemo/overlay/tianxie");


        AliyunPasterController ckaitou = mPasterManager.addPaster(kaitou.getPath());

        ckaitou.setPasterStartTime(0);
        ckaitou.setPasterDuration(mAliyunIPlayer.getDuration()/2);//显示时间

        AliyunPasterBaseView mAliyunPasterBaseView = new AliyunPasterBaseView(){

            @Override
            public String getText() {
                return null;
            }

            @Override
            public int getTextColor() {
                return Color.RED;
            }

            @Override
            public int getTextStrokeColor() {
                return Color.BLACK;
            }

            @Override
            public boolean isTextHasStroke() {
                return true;
            }

            @Override
            public boolean isTextHasLabel() {
                return false;
            }

            @Override
            public int getTextBgLabelColor() {
                return 0;
            }

            @Override
            public int getPasterTextOffsetX() {
                return width/2;
            }

            @Override
            public int getPasterTextOffsetY() {
                return height/2;
            }

            @Override
            public int getPasterTextWidth() {
                return 0;
            }

            @Override
            public int getPasterTextHeight() {
                return 0;
            }

            @Override
            public float getPasterTextRotation() {
                return 90;
            }

            @Override
            public String getPasterTextFont() {
                return null;
            }

            @Override
            public int getPasterWidth() {
                return width;
            }

            @Override
            public int getPasterHeight() {
                return height;
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
            public float getPasterRotation() {
                return 0;
            }

            @Override
            public Bitmap transToImage() {
                return null;
            }

            @Override
            public View getPasterView() {
                return null;
            }

            @Override
            public boolean isPasterMirrored() {
                return false;
            }
        };
        ckaitou.setPasterView(mAliyunPasterBaseView);
        ckaitou.editCompleted();

//        PasterUIGifImpl gifuickaitou = addPaster(ckaitou);
//        if (currentEdit != null && !currentEdit.isPasterRemoved()) {
//            currentEdit.editTimeCompleted();
//        }
//        gifuickaitou.editTimeStart();
//        gifuickaitou.moveToCenter();
//        gifuickaitou.editTimeCompleted();


        EffectInfo effectInfo = new EffectInfo();
        effectInfo.type = UIEditorPage.OVERLAY;//图像
        effectInfo.setPath("/storage/emulated/0/Android/data/cn.a218.newproject/cache/AliyunEditorDemo/overlay/baiyang");
        AliyunPasterController c = mPasterManager.addPaster(effectInfo.getPath());
        c.setPasterStartTime(1500000);
        c.setPasterDuration(mAliyunIPlayer.getDuration());//显示时间

        PasterUIGifImpl gifui = addPaster(c);
        if (currentEdit != null && !currentEdit.isPasterRemoved()) {
            currentEdit.editTimeCompleted();
        }
        gifui.editTimeStart();
        gifui.moveToCenter();
        gifui.editTimeCompleted();

//        EffectInfo effectInfo2 = new EffectInfo();
//        effectInfo2.type = UIEditorPage.OVERLAY;//图像
//        effectInfo2.setPath("/storage/emulated/0/Android/data/cn.a218.newproject/cache/AliyunEditorDemo/overlay/baiyang");
//
//        AliyunPasterController vc = mPasterManager.addPaster(effectInfo2.getPath());
//        vc.setPasterStartTime(0);
//        vc.setPasterDuration(mAliyunIPlayer.getDuration());//显示时间
//
//        PasterUIGifImpl gifui2 = addPaster(vc);
//        if (currentEdit != null && !currentEdit.isPasterRemoved()) {
//            currentEdit.editTimeCompleted();
//        }
//        gifui2.editTimeStart();
//        gifui2.moveToCenter();
//        gifui2.editTimeCompleted();


//        AliyunPasterController mm = mPasterManager.addPaster("/storage/emulated/0/Android/data/cn.a218.newproject/cache/AliyunEditorDemo/overlay/baiyang/baiyang1");
//        mm.setPasterStartTime(mAliyunIPlayer.getCurrentPosition());
//        mm.setPasterDuration(mAliyunIPlayer.getDuration());//显示时间
//        PasterUICaptionImpl pp = addCaption(mm);
//        if (currentEdit != null && !currentEdit.isPasterRemoved()) {
//            currentEdit.editTimeCompleted();
//        }
//        pp.editTimeStart();
//        pp.moveToCenter();
//        pp.editTimeCompleted();
    }

    //各种特效 改变监听
    @Override
    public void onEffectChange(EffectInfo effectInfo) {
        Log.e("editor", "====== onEffectChange ");
        //返回素材属性

        EffectBean effect = new EffectBean();
        effect.setId(effectInfo.id);
        effect.setPath(effectInfo.getPath());
        UIEditorPage type = effectInfo.type;
        AliyunPasterController c;
        Log.d(TAG, "effect path " + effectInfo.getPath());
        switch (type) {
            case AUDIO_MIX://音乐
                mAliyunIEditor.applyMusicMixWeight(effectInfo.musicWeight);
                if (!effectInfo.isAudioMixBar) {
                    mAliyunIEditor.applyMusic(effect);
                    mTimelineBar.resume();
                    mPlayImage.setSelected(false);
                }
                break;
            case FILTER_EFFECT:
                mAliyunIEditor.applyFilter(effect);
                break;
            case MV:
                //添加处理的效果视频
                String path = null;
                if (effectInfo.list != null) {
                    path = Common.getMVPath(effectInfo.list, mAliyunIPlayer.getVideoWidth(), mAliyunIPlayer.getVideoHeight());
                }
                effect.setPath(path);
                mAliyunIEditor.applyMV(effect);
                mTimelineBar.resume();
                mPlayImage.setSelected(false);
                break;
            case CAPTION://字幕
                c = mPasterManager.addPaster(effectInfo.getPath());
                c.setPasterStartTime(mAliyunIPlayer.getCurrentPosition());
                PasterUICaptionImpl cui = addCaption(c);//添加  表面的view
                if (currentEdit != null && !currentEdit.isPasterRemoved()) {
                    currentEdit.editTimeCompleted();
                }
                playingPause();
                currentEdit = cui;
                currentEdit.showTimeEdit();
                break;
            case OVERLAY:
                c = mPasterManager.addPaster(effectInfo.getPath());
                c.setPasterStartTime(mAliyunIPlayer.getCurrentPosition());
                PasterUIGifImpl gifui = addPaster(c);
                if (currentEdit != null && !currentEdit.isPasterRemoved()) {
                    currentEdit.editTimeCompleted();
                }
                playingPause();
                currentEdit = gifui;
                currentEdit.showTimeEdit();
                break;
            case FONT:
                c = mPasterManager.addSubtitle("子标题", effectInfo.fontPath + "/font.ttf");
                c.setPasterStartTime(mAliyunIPlayer.getCurrentPosition());
                PasterUITextImpl textui = addSubtitle(c, false);
                if (currentEdit != null && !currentEdit.isPasterRemoved()) {
                    currentEdit.editTimeCompleted();
                }
                playingPause();
                currentEdit = textui;
                currentEdit.showTimeEdit();
                textui.showTextEdit();
                break;
        }
    }

    private void checkAndRemovePaster() {
        int count = mPasterContainer.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View pv = mPasterContainer.getChildAt(i);
            PasterUISimpleImpl uic = (PasterUISimpleImpl) pv.getTag();
            if (!uic.isPasterExists()) {
                Log.e(TAG, "removePaster");
                uic.removePaster();
            }
        }
    }

    private void pauseAllPaster() {
        int count = mPasterContainer.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View pv = mPasterContainer.getChildAt(i);
            PasterUISimpleImpl uic = (PasterUISimpleImpl) pv.getTag();
            uic.editTimeCompleted();
        }
    }

    protected void playingPause() {
        if (mAliyunIPlayer.isPlaying()) {
            mAliyunIPlayer.pause();
            mTimelineBar.pause();
            mPlayImage.setSelected(true);
        }
    }

    protected void playingResume() {
        if (!mAliyunIPlayer.isPlaying()) {
            mAliyunIPlayer.resume();
            mTimelineBar.resume();
            mPlayImage.setSelected(false);
        }
    }

    private PasterUIGifImpl addPaster(AliyunPasterController controller) {
        final AliyunPasterWithImageView pasterView = (AliyunPasterWithImageView) View.inflate(this,
                R.layout.qupai_paster_gif, null);
        //加载动图缩略图（关键帧图片）
//        final long startTime = System.currentTimeMillis();
//        Log.d(TAG, "Start load paster image");
//        Glide.with(getApplicationContext())
//                .load("file://" + controller.getPasterIconPath())
//                .listener(new RequestListener<String, GlideDrawable>() {
//                    @Override
//                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                        Log.d(TAG, "load paster image cost time：" + (System.currentTimeMillis() - startTime));
//                        return false;
//                    }
//                })
//                .override(controller.getPasterWidth(), controller.getPasterHeight())
//                .dontTransform()
//                .into((ImageView) pasterView.getContentView());
        mPasterContainer.addView(pasterView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        return new PasterUIGifImpl(pasterView, controller, mTimelineBar,true);
    }


    //加载 字  表面
    private PasterUICaptionImpl addCaption(AliyunPasterController controller) {
        AliyunPasterWithImageView captionView = (AliyunPasterWithImageView) View.inflate(this,
                R.layout.qupai_paster_caption, null);
//        ImageView content = (ImageView) captionView.findViewById(R.id.qupai_overlay_content_animation);
//        Glide.with(getApplicationContext())
//                .load("file://" + controller.getPasterIconPath())
//                .into(content);
        mPasterContainer.addView(captionView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        return new PasterUICaptionImpl(captionView, controller, mTimelineBar);
    }


    private PasterUITextImpl addSubtitle(AliyunPasterController controller, boolean restore) {
        AliyunPasterWithTextView captionView = (AliyunPasterWithTextView) View.inflate(this,
                R.layout.qupai_paster_text, null);
        mPasterContainer.addView(captionView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        return new PasterUITextImpl(this,captionView, controller, mTimelineBar, restore);
    }

    private PasterUITextImpl2 addSubtitle2(AliyunPasterController controller, boolean restore) {
        AliyunPasterWithTextView captionView = (AliyunPasterWithTextView) View.inflate(this,
                R.layout.qupai_paster_text, null);
        mPasterContainer.addView(captionView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        return new PasterUITextImpl2(this,captionView, controller, mTimelineBar, restore);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mViewStack.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showBottomView() {
//        ViewCompat.animate(mBottomLinear)
//                .translationYBy(-mBottomLinear.getMeasuredHeight())
//                .alpha(1f)
//                .setDuration(300).start();

        mBottomLinear.setVisibility(View.VISIBLE);
        mActionBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideBottomView() {
//        ViewCompat.animate(mBottomLinear)
//                .translationYBy(mBottomLinear.getMeasuredHeight())
//                .alpha(0f)
//                .setDuration(300).start();

        mBottomLinear.setVisibility(View.GONE);
        mActionBar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        if (mAliyunIPlayer != null) {
            if (mAliyunIPlayer.isPlaying()) {
                playingPause();
            } else {
                playingResume();
                if (currentEdit != null && !currentEdit.isPasterRemoved()) {
                    currentEdit.editTimeCompleted();
                }
            }
        }
    }

    private PasterUISimpleImpl currentEdit;

//控制手势，显示贴图
private class MyOnGestureListener extends
        GestureDetector.SimpleOnGestureListener {
    float mPosX;
    float mPosY;
    boolean shouldDrag = true;

    boolean shouldDrag() {
        return shouldDrag;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return super.onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d("MOVE", "onDoubleTapEvent");
        return super.onDoubleTapEvent(e);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d("MOVE", "onSingleTapConfirmed");
        if (!shouldDrag) {
            boolean outside = true;
            int count = mPasterContainer.getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                View pv = mPasterContainer.getChildAt(i);
                PasterUISimpleImpl uic = (PasterUISimpleImpl) pv.getTag();
                if (uic.isVisibleInTime(mAliyunIPlayer.getCurrentPosition())
                        && uic.contentContains(e.getX(), e.getY())) {
                    outside = false;
                    if (currentEdit != null && currentEdit != uic
                            && !currentEdit.isEditCompleted()) {
                        currentEdit.editTimeCompleted();
                    }
                    currentEdit = uic;
                    if (uic.isEditCompleted()) {
                        playingPause();
                        uic.editTimeStart();
                    }
                    break;
                } else {
                    if (currentEdit != uic && uic.isVisibleInTime(mAliyunIPlayer.getCurrentPosition())) {
                        uic.editTimeCompleted();
                        playingResume();
                    }
                }
            }

            if (outside) {
                if (currentEdit != null && !currentEdit.isEditCompleted()) {
                    Log.d("LLLL", "CurrPosition = " + mAliyunIPlayer.getCurrentPosition());
                    currentEdit.editTimeCompleted();
                }
            }
        } else {
            playingPause();
            currentEdit.showTextEdit();
        }

        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return super.onSingleTapUp(e);
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d("MOVE", "onShowPress");
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {
        if (shouldDrag()) {
            if (mPosX == 0 || mPosY == 0) {
                mPosX = e1.getX();
                mPosY = e1.getY();
            }
            float x = e2.getX();
            float y = e2.getY();

            float a = x - mPosX;
            float b = y - mPosY;
            System.out.println("a="+a+"   ---------------------   b="+b);
            currentEdit.moveContent(x - mPosX, y - mPosY);

            mPosX = x;
            mPosY = y;
        }

        Log.d("MOVE", "onScroll" + " shouldDrag : " + shouldDrag
                + " x : " + mPosX + " y : " + mPosY + " dx : "
                + distanceX + " dy : " + distanceY);

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d("MOVE", "onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2,
                           float velocityX, float velocityY) {
        Log.d("MOVE", "onFling");
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.d("MOVE", "onDown" + " (" + e.getX() + " : " + e.getY()
                + ")");
        if (currentEdit != null && currentEdit.isPasterRemoved()) {
            currentEdit = null;
        }

        if (currentEdit != null) {
            shouldDrag = !currentEdit.isEditCompleted()
                    && currentEdit.contentContains(e.getX(), e.getY())
                    && currentEdit.isVisibleInTime(mAliyunIPlayer.getCurrentPosition());
        } else {
            shouldDrag = false;
        }

        mPosX = 0;
        mPosY = 0;
        return false;
    }

}

    private String convertDuration2Text(long duration) {
        int sec = Math.round(((float) duration) / (1000 * 1000));// us -> s
        int min = (sec % 3600) / 60;
        sec = (sec % 60);
        return String.format(getString(R.string.timeline_curr_time),
                min,
                sec);
    }

    private void copyAssets() {
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Common.copyAll(EditorActivity.this, resCopy);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
//                resCopy.setVisibility(View.GONE);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public AliyunIPlayer getPlayer() {
        return this.mAliyunIPlayer;
    }

    public void showMessage(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(id);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private OnDialogButtonClickListener mDialogButtonClickListener = new OnDialogButtonClickListener() {
        @Override
        public void onPositiveClickListener(int index) {

        }

        @Override
        public void onNegativeClickListener(int index) {
            UIEditorPage in = UIEditorPage.get(index);
            int count = mPasterContainer.getChildCount();
            switch (in) {
                case OVERLAY://清除所有动图
                    for (int i = count - 1; i >= 0; i--) {
                        View pv = mPasterContainer.getChildAt(i);
                        PasterUISimpleImpl uic = (PasterUISimpleImpl) pv.getTag();
                        if (uic.mController.getPasterType() == EffectPaster.PASTER_TYPE_GIF) {
                            uic.removePaster();
                        }
                    }
                    break;
                case CAPTION:
                    for (int i = count - 1; i >= 0; i--) {
                        View pv = mPasterContainer.getChildAt(i);
                        PasterUISimpleImpl uic = (PasterUISimpleImpl) pv.getTag();
                        if (uic.mController.getPasterType() == EffectPaster.PASTER_TYPE_CAPTION
                                || uic.mController.getPasterType() == EffectPaster.PASTER_TYPE_TEXT) {
                            uic.removePaster();
                        }
                    }
                    break;
            }
        }
    };

}