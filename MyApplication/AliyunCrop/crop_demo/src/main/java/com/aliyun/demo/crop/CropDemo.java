/*
 * Copyright (C) 2010-2013 Alibaba Group Holding Limited
 */

package com.aliyun.demo.crop;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aliyun.common.utils.FileUtils;
import com.aliyun.common.utils.ToastUtil;
import com.aliyun.crop.AliyunCropCreator;
import com.aliyun.crop.struct.CropParam;
import com.aliyun.crop.supply.CropCallback;
import com.aliyun.crop.supply.AliyunICrop;
import com.aliyun.demo.crop.media.FrameExtractor10;
import com.aliyun.demo.crop.media.VideoTrimAdapter;
import com.aliyun.querrorcode.AliyunErrorCode;
import com.aliyun.quview.HorizontalListView;
import com.aliyun.quview.ProgressDialog;
import com.aliyun.quview.SizeChangedNotifier;
import com.aliyun.quview.VideoSliceSeekBar;
import com.aliyun.quview.VideoTrimFrameLayout;
import com.aliyun.struct.common.ScaleMode;
import com.aliyun.struct.common.VideoQuality;

import java.io.File;
import java.io.IOException;


/**
 * Copyright (C) 2010-2013 Alibaba Group Holding Limited
 *
 * Created by Administrator on 2017/1/16.
 */

public class CropDemo extends Activity implements TextureView.SurfaceTextureListener,
        VideoSliceSeekBar.SeekBarChangeListener, HorizontalListView.OnScrollCallBack, SizeChangedNotifier.Listener,
        MediaPlayer.OnVideoSizeChangedListener, VideoTrimFrameLayout.OnVideoScrollCallBack, View.OnClickListener, CropCallback, Handler.Callback {

    public static final int RESOLUTION_1_1 = 0;
    public static final int RESOLUTION_3_4 = 1;
    public static final int RESOLUTION_9_16 = 2;

    public static final String VIDEO_PATH = "video_path";
    public static final String VIDEO_DURATION = "video_duration";
    public static final String VIDEO_RESOLUTION = "video_resolution";
    public static final String VIDEO_SCALE = "video_scale";
    public static final String VIDEO_QUALITY = "video_quality";
    public static final String VIDEO_FRAMERATE = "video_framerate";
    public static final String VIDEO_GOP = "video_gop";
    private static final int[][] resolutions = new int[][]{new int[]{540, 540}, new int[]{540, 720}, new int[]{540, 960}};

    public static final ScaleMode SCALE_CROP = ScaleMode.PS;
    public static final ScaleMode SCALE_FILL = ScaleMode.LB;

    public static final String RESULT_KEY_CROP_PATH = "crop_path";
    public static final String RESULT_KEY_DURATION = "duration";


    private static final int PLAY_VIDEO = 1000;
    private static final int PAUSE_VIDEO = 1001;
    private static final int END_VIDEO = 1003;

    private int playState = END_VIDEO;

    private static final int MIN_CROP_DURATION = 2000;

    private AliyunICrop crop;

    private HorizontalListView listView;
    private VideoTrimFrameLayout frame;
    private TextureView textureview;
    private Surface mSurface;


    private MediaPlayer mPlayer;
    private ImageView cancelBtn, nextBtn, transFormBtn;
    private TextView dirationTxt;

    private VideoTrimAdapter adapter;

    private VideoSliceSeekBar seekBar;

    private ProgressDialog progressDialog;

    private long videoPos;
    private long lastVideoSeekTime;


    private String path;
    private String outputPath;
    private int duration;
    private int resolutionMode;
    private VideoQuality quality = VideoQuality.HD;
    private int frameRate;
    private int gop;

    private int screenWidth;
    private int screenHeight;
    private int frameWidth;
    private int frameHeight;
    private int mScrollX;
    private int mScrollY;
    private int videoWidth;
    private int videoHeight;

    private ScaleMode scaleMode = ScaleMode.LB;

    private long startTime;
    private long endTime;

    private int maxDuration = Integer.MAX_VALUE;

    private FrameExtractor10 kFrame;

    private MediaScannerConnection msc;

    private Handler playHandler = new Handler(this);

    private int currentPlayPos;

    private boolean isPause = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_crop);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        crop = AliyunCropCreator.getCropInstance(this);
        crop.setCropCallback(this);
        getData();
        initView();
        initSurface();
        msc = new MediaScannerConnection(this, null);
        msc.connect();
    }

    private void getData() {
        path = getIntent().getStringExtra(VIDEO_PATH);
        try {
            duration = (int) crop.getVideoDuration(path) / 1000;
        } catch (Exception e) {
            ToastUtil.showToast(this, R.string.video_crop_erroe);
        }//获取精确的视频时间
        resolutionMode = getIntent().getIntExtra(VIDEO_RESOLUTION, RESOLUTION_3_4);
        scaleMode = (ScaleMode) getIntent().getSerializableExtra(VIDEO_SCALE);
        scaleMode = scaleMode == null ? ScaleMode.PS : scaleMode;
        quality = (VideoQuality) getIntent().getSerializableExtra(VIDEO_QUALITY);
        quality = quality == null ? VideoQuality.HD : quality;
        gop = getIntent().getIntExtra(VIDEO_GOP, 125);
        frameRate = getIntent().getIntExtra(VIDEO_FRAMERATE, 25);
    }

    private void initView() {
        kFrame = new FrameExtractor10();
        kFrame.setDataSource(path);
        seekBar = (VideoSliceSeekBar) findViewById(R.id.seek_bar);
        seekBar.setSeekBarChangeListener(this);
        int minDiff = (int) (MIN_CROP_DURATION / (float) duration * 100) + 1;
        seekBar.setProgressMinDiff(minDiff > 100 ? 100 : minDiff);
        listView = (HorizontalListView) findViewById(R.id.video_tailor_image_list);
        listView.setOnScrollCallBack(this);
        adapter = new VideoTrimAdapter(this, duration, maxDuration, kFrame, seekBar);
        listView.setAdapter(adapter);
        transFormBtn = (ImageView) findViewById(R.id.transform);
        transFormBtn.setOnClickListener(this);
        nextBtn = (ImageView) findViewById(R.id.next);
        nextBtn.setOnClickListener(this);
        cancelBtn = (ImageView) findViewById(R.id.back);
        cancelBtn.setOnClickListener(this);
        dirationTxt = (TextView) findViewById(R.id.duration_txt);
        dirationTxt.setText((float) duration / 1000 + "");
        setListViewHeight();
    }

    private void setListViewHeight() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.height = screenWidth / 8;
        listView.setLayoutParams(layoutParams);
        seekBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,screenWidth/8));
    }

    public void initSurface() {
        frame = (VideoTrimFrameLayout) findViewById(R.id.video_surfaceLayout);
        frame.setOnSizeChangedListener(this);
        frame.setOnScrollCallBack(this);
        resizeFrame();
        textureview = (TextureView) findViewById(R.id.video_textureview);
        textureview.setSurfaceTextureListener(this);
    }

    private void resizeFrame() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) frame.getLayoutParams();
        switch (resolutionMode) {
            case RESOLUTION_1_1:
                layoutParams.width = screenWidth;
                layoutParams.height = screenWidth;
                break;
            case RESOLUTION_3_4:
                layoutParams.width = screenWidth;
                layoutParams.height = screenWidth * 4 / 3;
                break;
            case RESOLUTION_9_16:
                layoutParams.width = screenWidth;
                layoutParams.height = screenWidth * 16 / 9;
                break;
        }
        frame.setLayoutParams(layoutParams);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mPlayer == null) {
            mSurface = new Surface(surface);
            mPlayer = new MediaPlayer();
            mPlayer.setSurface(mSurface);
            try {
                mPlayer.setDataSource(path);
                mPlayer.setLooping(true);
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if (!isPause) {
                            playVideo();
                            playState = PLAY_VIDEO;
                        } else {
                            isPause = false;
                            mPlayer.start();
                            mPlayer.seekTo(currentPlayPos);
                            playHandler.sendEmptyMessageDelayed(PAUSE_VIDEO,100);
                        }
                    }
                });
                mPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mPlayer.setOnVideoSizeChangedListener(this);

        }
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            mSurface = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void SeekBarValueChanged(float leftThumb, float rightThumb, int whitchSide) {
        int seekPos = 0;
        if (whitchSide == 0) {
            seekPos = (int) (duration * leftThumb / 100);
            startTime = seekPos * 1000;
        } else if (whitchSide == 1) {
            seekPos = (int) (duration * rightThumb / 100);
            endTime = seekPos * 1000;
        }
        dirationTxt.setText((float) (endTime - startTime) / 1000 / 1000 + "");
        mPlayer.seekTo(seekPos);
    }

    @Override
    public void onSeekStart() {
        pauseVideo();
    }

    @Override
    public void onSeekEnd() {
        if (playState == PLAY_VIDEO) {
            playVideo();
        }
    }

    private void resetScroll() {
        mScrollX = 0;
        mScrollY = 0;
    }

    @Override
    public void onScrollDistance(Long count, int distanceX) {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (playState == PLAY_VIDEO) {
            pauseVideo();
            playState = PAUSE_VIDEO;
        }
        isPause = true;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        msc.disconnect();
        AliyunCropCreator.destroyCropInstance();
    }

    private void scaleFill(int videoWidth, int videoHeight) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureview.getLayoutParams();
        int s = Math.min(videoWidth,videoHeight);
        int b = Math.max(videoWidth,videoHeight);
        float videoRatio = (float)b/s;
        float ratio = 1f;
        switch (resolutionMode){
            case RESOLUTION_1_1:
                ratio = 1f;
                break;
            case RESOLUTION_3_4:
                ratio = (float)4/3;
                break;
            case RESOLUTION_9_16:
                ratio = (float)16/9;
                break;
        }
        if(videoRatio > ratio){
            if (videoHeight > videoWidth) {
                layoutParams.height = frameHeight;
                layoutParams.width = frameHeight * videoWidth / videoHeight;
            } else {
                layoutParams.width = frameWidth;
                layoutParams.height = frameWidth * videoHeight / videoWidth;
            }
        }else{
            if (videoHeight > videoWidth) {
                layoutParams.width = frameWidth;
                layoutParams.height = frameWidth * videoHeight / videoWidth;
            } else {
                layoutParams.height = frameHeight;
                layoutParams.width = frameHeight * videoWidth / videoHeight;
            }
        }
        layoutParams.setMargins(0, 0, 0, 0);
        textureview.setLayoutParams(layoutParams);
        scaleMode = SCALE_FILL;
        transFormBtn.setActivated(false);
        resetScroll();
    }

    private void scaleCrop(int videoWidth, int videoHeight) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textureview.getLayoutParams();
        int s = Math.min(videoWidth,videoHeight);
        int b = Math.max(videoWidth,videoHeight);
        float vidoeRatio = (float)b/s;
        float ratio = 1f;
        switch (resolutionMode){
            case RESOLUTION_1_1:
                ratio = 1f;
                break;
            case RESOLUTION_3_4:
                ratio = (float)4/3;
                break;
            case RESOLUTION_9_16:
                ratio = (float)16/9;
                break;
        }
        if(vidoeRatio > ratio){
            if (videoHeight > videoWidth) {
                layoutParams.width = frameWidth;
                layoutParams.height = frameWidth * videoHeight / videoWidth;
            } else {
                layoutParams.height = frameHeight;
                layoutParams.width = frameHeight * videoWidth / videoHeight;
            }
        }else{
            if (videoHeight > videoWidth) {
                layoutParams.height = frameHeight;
                layoutParams.width = frameHeight * videoWidth / videoHeight;
            } else {
                layoutParams.width = frameWidth;
                layoutParams.height = frameWidth * videoHeight / videoWidth;
            }
        }
        layoutParams.setMargins(0, 0, 0, 0);
        textureview.setLayoutParams(layoutParams);
        scaleMode = SCALE_CROP;
        transFormBtn.setActivated(true);
        resetScroll();
    }

    private void scanFile() {
        msc.scanFile(outputPath, "video/mp4");
    }

    private void playVideo() {
        if(mPlayer == null){
            return;
        }
        mPlayer.seekTo((int) (startTime / 1000));
        mPlayer.start();
        videoPos = startTime / 1000;
        lastVideoSeekTime = System.currentTimeMillis();
        playHandler.sendEmptyMessage(PLAY_VIDEO);
    }

    private void pauseVideo() {
        if(mPlayer == null){
            return;
        }
        mPlayer.pause();
        playHandler.removeMessages(PLAY_VIDEO);
        seekBar.showFrameProgress(false);
        seekBar.invalidate();
    }

    private void resumeVideo() {
        if(mPlayer == null){
            return;
        }
        mPlayer.start();
        playHandler.sendEmptyMessage(PLAY_VIDEO);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mp.setOnVideoSizeChangedListener(null);
        frameWidth = frame.getWidth();
        frameHeight = frame.getHeight();
        videoWidth = width;
        videoHeight = height;
        startTime = 0;
        if (crop != null) {
            try{
                endTime = crop.getVideoDuration(path);
                ToastUtil.showToast(CropDemo.this, endTime+"");
            }catch (Exception e){
                ToastUtil.showToast(this,R.string.video_crop_erroe);
            }
        } else {
            endTime = Integer.MAX_VALUE;
        }
        if (scaleMode == SCALE_CROP) {
            scaleCrop(width, height);
        } else if (scaleMode == SCALE_FILL) {
            scaleFill(width, height);
        }

    }

    @Override
    public void onSizeChanged(View view, int w, int h, int oldw, int oldh) {

    }

    @Override
    public void onVideoScroll(float distanceX, float distanceY) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textureview.getLayoutParams();
        int width = lp.width;
        int height = lp.height;

        if (width > frameWidth || height > frameHeight) {
            int maxHorizontalScroll = width - frameWidth;
            int maxVerticalScroll = height - frameHeight;
            if (maxHorizontalScroll > 0) {
                maxHorizontalScroll = maxHorizontalScroll / 2;
                mScrollX += distanceX;
                if (mScrollX > maxHorizontalScroll) {
                    mScrollX = maxHorizontalScroll;
                }
                if (mScrollX < -maxHorizontalScroll) {
                    mScrollX = -maxHorizontalScroll;
                }
            }
            if (maxVerticalScroll > 0) {
                maxVerticalScroll = maxVerticalScroll / 2;
                mScrollY += distanceY;
                if (mScrollY > maxVerticalScroll) {
                    mScrollY = maxVerticalScroll;
                }
                if (mScrollY < -maxVerticalScroll) {
                    mScrollY = -maxVerticalScroll;
                }
            }
            lp.setMargins(0, 0, mScrollX, mScrollY);
        }

        textureview.setLayoutParams(lp);
    }

    @Override
    public void onVideoSingleTapUp() {
        if (playState == END_VIDEO) {
            playVideo();
            playState = PLAY_VIDEO;
        } else if (playState == PLAY_VIDEO) {
            pauseVideo();
            playState = PAUSE_VIDEO;
        } else if (playState == PAUSE_VIDEO) {
            resumeVideo();
            playState = PLAY_VIDEO;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == transFormBtn) {
            if (scaleMode == SCALE_FILL) {
                scaleCrop(videoWidth, videoHeight);
            } else if (scaleMode == SCALE_CROP) {
                scaleFill(videoWidth, videoHeight);
            }
        } else if (v == nextBtn) {
            startCrop();
        } else if (v == cancelBtn) {
            finish();
        }
    }

    private void startCrop() {
        if (frameWidth == 0 || frameHeight == 0) {
            ToastUtil.showToast(this, R.string.video_crop_erroe);
            return;
        }
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textureview.getLayoutParams();
        int posX;
        int posY;
        int outputWidth;
        int outputHeight;
        int cropWidth;
        int cropHeight;
        float videoRatio = (float) videoHeight/videoWidth;
        float outputRatio = (float) resolutions[resolutionMode][1]/resolutions[resolutionMode][0];

        outputPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator + "crop_" + System.currentTimeMillis() + ".mp4";
        if (videoRatio > outputRatio) {
            posX = 0;
            posY = ((lp.height - frameHeight) / 2 + mScrollY) * videoWidth / frameWidth;
            while (posY % 4 != 0) {
                posY++;
            }
            outputWidth = resolutions[resolutionMode][0];
            outputHeight = resolutions[resolutionMode][1];
            cropWidth = videoWidth;
            cropHeight = 0;
            switch (resolutionMode) {
                case RESOLUTION_1_1:
                    cropHeight = videoWidth;
                    break;
                case RESOLUTION_3_4:
                    cropHeight = videoWidth * 4 / 3;
                    break;
                case RESOLUTION_9_16:
                    cropHeight = videoWidth * 16 / 9;
                    break;
            }
        } else {
            posX = ((lp.width - frameWidth) / 2 + mScrollX) * videoHeight / frameHeight;
            posY = 0;
            while (posX % 4 != 0) {
                posX++;
            }
            outputWidth = resolutions[resolutionMode][0];
            outputHeight = resolutions[resolutionMode][1];
            cropWidth = 0;
            cropHeight = videoHeight;
            switch (resolutionMode) {
                case RESOLUTION_1_1:
                    cropWidth = videoHeight;
                    break;
                case RESOLUTION_3_4:
                    cropWidth = videoHeight * 3 / 4;
                    break;
                case RESOLUTION_9_16:
                    cropWidth = videoHeight * 9 / 16;
                    break;
            }
        }
        CropParam cropParam = new CropParam();
        cropParam.setOutputPath(outputPath);
        cropParam.setVideoPath(path);
        cropParam.setOutputWidth(outputWidth);
        cropParam.setOutputHeight(outputHeight);
        Rect cropRect = new Rect(posX,posY,posX+cropWidth,posY+cropHeight);
        cropParam.setCropRect(cropRect);
        cropParam.setStartTime(startTime);
        cropParam.setEndTime(endTime);
        cropParam.setScaleMode(scaleMode);
        cropParam.setFrameRate(frameRate);
        cropParam.setGop(gop);
        cropParam.setQuality(quality);
        if ((endTime - startTime) / 1000 / 1000 / 60 >= 5) {
            ToastUtil.showToast(this, R.string.video_duration_5min_tip);
            return;
        }
        crop.setCropParam(cropParam);
        progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.wait));
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                crop.cancel();
                deleteFile();
                setResult(Activity.RESULT_CANCELED, getIntent());
            }
        });
        crop.startCrop();

    }

    private void deleteFile() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                FileUtils.deleteFile(outputPath);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onProgress(final int percent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgress(percent);
            }
        });
    }

    @Override
    public void onError(final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (code != AliyunErrorCode.ERROR_LICENSE_FAILED) {
                    ToastUtil.showToast(CropDemo.this, R.string.video_crop_erroe);
                }
                progressDialog.dismiss();
                setResult(Activity.RESULT_CANCELED, getIntent());
            }
        });

    }

    @Override
    public void onComplete(long duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanFile();
                Intent intent = getIntent();
                intent.putExtra(RESULT_KEY_CROP_PATH, outputPath);
                intent.putExtra(RESULT_KEY_DURATION, (endTime - startTime) / 1000);
                setResult(Activity.RESULT_OK, intent);
                finish();
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public void onCancelComplete() {
        //取消完成
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case PAUSE_VIDEO:
                pauseVideo();
                playState = PAUSE_VIDEO;
                break;
            case PLAY_VIDEO:
                if (mPlayer != null) {
                    currentPlayPos = (int) (videoPos + System.currentTimeMillis() - lastVideoSeekTime);
                    if (currentPlayPos < endTime / 1000) {
                        seekBar.showFrameProgress(true);
                        seekBar.setFrameProgress(currentPlayPos / (float) duration);
                        playHandler.sendEmptyMessageDelayed(PLAY_VIDEO, 100);
                    } else {
                        playVideo();
                    }
                }
                break;
        }
        return false;
    }
}
