/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.crop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aliyun.demo.crop.media.GalleryDirChooser;
import com.aliyun.demo.crop.media.GalleryMediaChooser;
import com.aliyun.demo.crop.media.MediaDir;
import com.aliyun.demo.crop.media.MediaInfo;
import com.aliyun.demo.crop.media.MediaStorage;
import com.aliyun.demo.crop.media.ThumbnailGenerator;
import com.aliyun.jasonparse.JSONSupportImpl;
import com.aliyun.struct.common.ScaleMode;
import com.aliyun.struct.common.VideoQuality;

/**
 * Created by Administrator on 2017/1/13.
 */

public class MediaActivity extends Activity implements View.OnClickListener{
    private MediaStorage storage;
    private GalleryDirChooser galleryDirChooser;
    private ThumbnailGenerator thumbnailGenerator;
    private GalleryMediaChooser galleryMediaChooser;
    private RecyclerView galleryView;
    private ImageButton back;
    private TextView title;
    private int resolutionMode;
    private ScaleMode scaleMode = ScaleMode.LB;
    private int frameRate;
    private int gop;
    private VideoQuality quality = VideoQuality.SSD;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        getData();
        init();
    }
    private void getData(){
        resolutionMode = getIntent().getIntExtra(CropDemo.VIDEO_RESOLUTION,0);
        scaleMode = (ScaleMode) getIntent().getSerializableExtra(CropDemo.VIDEO_SCALE);
        frameRate = getIntent().getIntExtra(CropDemo.VIDEO_FRAMERATE,25);
        gop = getIntent().getIntExtra(CropDemo.VIDEO_GOP,5);
        quality = (VideoQuality) getIntent().getSerializableExtra(CropDemo.VIDEO_QUALITY);
    }
    private void init(){
        galleryView = (RecyclerView) findViewById(R.id.gallery_media);
        title = (TextView) findViewById(R.id.gallery_title);
        title.setText(R.string.gallery_all_media);
        back = (ImageButton) findViewById(R.id.gallery_closeBtn);
        back.setOnClickListener(this);
        storage = new MediaStorage(this, new JSONSupportImpl());
        thumbnailGenerator = new ThumbnailGenerator(this);
        galleryDirChooser = new GalleryDirChooser(this, findViewById(R.id.topPanel), thumbnailGenerator, storage);
        galleryMediaChooser = new GalleryMediaChooser(galleryView,galleryDirChooser,storage,thumbnailGenerator);
        storage.setSortMode(MediaStorage.SORT_MODE_VIDEO);
        storage.startFetchmedias();
        storage.setOnMediaDirChangeListener(new MediaStorage.OnMediaDirChange() {
            @Override
            public void onMediaDirChanged() {
                MediaDir dir = storage.getCurrentDir();
                if (dir.id == -1) {
                    title.setText(getString(R.string.gallery_all_media));
                } else {
                    title.setText(dir.dirName);
                }
                galleryMediaChooser.changeMediaDir(dir);
            }
        });
        storage.setOnCurrentMediaInfoChangeListener(new MediaStorage.OnCurrentMediaInfoChange() {
            @Override
            public void onCurrentMediaInfoChanged(MediaInfo info) {
                Intent intent = new Intent(MediaActivity.this,CropDemo.class);
                intent.putExtra(CropDemo.VIDEO_PATH,info.filePath);
                intent.putExtra(CropDemo.VIDEO_DURATION,info.duration);
                intent.putExtra(CropDemo.VIDEO_RESOLUTION,resolutionMode);
                intent.putExtra(CropDemo.VIDEO_SCALE,scaleMode);
                intent.putExtra(CropDemo.VIDEO_QUALITY,quality);
                intent.putExtra(CropDemo.VIDEO_GOP,gop);
                intent.putExtra(CropDemo.VIDEO_FRAMERATE,frameRate);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        storage.saveCurrentDirToCache();
        storage.cancelTask();
        thumbnailGenerator.cancelAllTask();
    }

    @Override
    public void onClick(View v) {
        if(v ==  back){
            finish();
        }
    }
}