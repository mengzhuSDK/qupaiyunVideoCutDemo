/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.importer;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.aliyun.common.media.AliyunMediaExtractor;
import com.aliyun.common.utils.FileUtils;
import com.aliyun.crop.AliyunCropCreator;
import com.aliyun.crop.struct.CropParam;
import com.aliyun.crop.supply.CropCallback;
import com.aliyun.crop.supply.AliyunICrop;
import com.aliyun.struct.common.ScaleMode;
import com.aliyun.struct.common.VideoQuality;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aliyun.demo.importer.media.MediaInfo;

/**
 * Created by apple on 2017/4/1.
 * 对于大于540P的视频要先走转码
 */

public class Transcoder {

    private static final String TAG = "Transcoder";
    private List<MediaInfo> mOriginalVideos = new ArrayList<>();
    private List<CropParam> mTranscodeVideos = new ArrayList<>();
    private static final String TRANSCODE_SUFFIX = "_transcode";
    private AliyunICrop mQuCrop;
    private TransCallback mTransCallback;
    private int mTranscodeIndex = 0;
    private int mTranscodeTotal = 0;
    private AsyncTask<Void, Long, List<MediaInfo>> mTranscodeTask;
    private int width = 540,height = 540;

    public void addVideo(MediaInfo mediaInfo) {
        mOriginalVideos.add(mediaInfo);
    }

    public List<MediaInfo> getVideo() {
        return mOriginalVideos;
    }

    public void addVideo(int index,MediaInfo mediaInfo){
        mOriginalVideos.add(index,mediaInfo);
    }

    public int removeVideo(MediaInfo mediaInfo) {
        int index = mOriginalVideos.indexOf(mediaInfo);
        mOriginalVideos.remove(mediaInfo);
        return index;
    }

    public void setTransResolution(int width,int height){
        if(width > 0){
            this.width = width;
        }
        if(height > 0){
            this.height = height;
        }
    }

    public void swap(int pos1, int pos2) {
        if (pos1 != pos2 && pos1 < mOriginalVideos.size() && pos2 < mOriginalVideos.size()) {
            Collections.swap(mOriginalVideos, pos1, pos2);
        }
    }

    public int getVideoCount(){
        return mOriginalVideos.size();
    }

    public void init(Context context) {
        mQuCrop = AliyunCropCreator.getCropInstance(context);
    }

    public void setTransCallback(TransCallback callback) {
        this.mTransCallback = callback;
    }

    public void transcode(final int[] resolution, final VideoQuality videoQuality, final ScaleMode scaleMode) {
        mTranscodeTotal = 0;
        mTranscodeIndex = 0;
        mTranscodeVideos.clear();
        if (mQuCrop == null) {
            return ;
        }
        mTranscodeTask = new AsyncTask<Void, Long, List<MediaInfo>>() {

            @Override
            protected List<MediaInfo> doInBackground(Void... params) {
                for (MediaInfo info : mOriginalVideos) {
                    AliyunMediaExtractor extractor = new AliyunMediaExtractor();
                    extractor.setDataSource(info.filePath);
                    int vWidth = extractor.getVideoWidth();
                    int vHeight = extractor.getVideoHeight();
                    int rate = extractor.getFrameRate();
                    int rotation = extractor.getRotation();
                    long duration = 0;
                    try {
                        duration = mQuCrop.getVideoDuration(info.filePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (vWidth * vHeight > width * height || rate >30) {
                        Log.d(TAG,"need transcode...path..."+ info.filePath);
                        CropParam cropParam = new CropParam();
                        cropParam.setVideoPath(info.filePath);
                        StringBuilder sb = new StringBuilder(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DCIM).getAbsolutePath())
                                .append(File.separator).append(System.currentTimeMillis()).append(".mp4_transcode");
                        cropParam.setOutputPath(sb.toString());//info.filePath + TRANSCODE_SUFFIX);
                        int outputWidth  = 0;
                        int outputHeight = 0;
//                        if (resolution != null && resolution.length >= 2) {
                        if(vWidth * vHeight > width * height){
                            if(vWidth > vHeight){
                                outputWidth = Math.min(width,height);
                                outputHeight = (int) ((float)vHeight /vWidth *outputWidth);
                            }else{
                                outputHeight = Math.min(width,height);
                                outputWidth = (int) ((float)vWidth / vHeight * outputHeight);
                            }
                        }
                        if( rate >30){
                            outputWidth  = vWidth;
                            outputHeight  = vHeight;
                        }

                        int temp;
                        if(rotation == 90 || rotation == 270) {
                            temp = outputWidth;
                            outputWidth = outputHeight;
                            outputHeight = temp;
                        }
//                        if(vWidth >= width && vHeight >= height){
//                            cropParam.setOutputWidth(width);
//                            cropParam.setOutputHeight(height);
//                        }else if(vWidth >= height && vHeight >= width){
//                            cropParam.setOutputHeight(width);
//                            cropParam.setOutputWidth(height);
//                        }else{
//                            continue;
//                        }
                        cropParam.setOutputHeight(outputHeight);
                        cropParam.setOutputWidth(outputWidth);
                        if(rotation == 90 || rotation == 270) {
                            cropParam.setCropRect(new Rect(0,0,vHeight, vWidth));
                        } else {
                            cropParam.setCropRect(new Rect(0,0,vWidth,vHeight));
                        }
                        cropParam.setScaleMode(scaleMode);
                        cropParam.setQuality(videoQuality);
                        cropParam.setFrameRate(30);
                        cropParam.setStartTime(0);
                        cropParam.setEndTime(duration);
                        mTranscodeVideos.add(cropParam);
                        mTranscodeTotal++;
                    }
                }
                if(isCancelled()){
                    return null;
                }
                if(mTranscodeVideos.size() > 0){
                    transcodeVideo(0);
                }else{
                    if(mTransCallback != null){
                        mTransCallback.onComplete(mOriginalVideos);
                    }
                }
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return ;
    }

    public void cancel() {
        mTranscodeTask.cancel(true);
        mQuCrop.cancel();
        if(mTransCallback != null){
            mTransCallback.onCancelComplete();
        }
    }

    private void transcodeVideo(int index) {
        CropParam cropParam = mTranscodeVideos.get(index);
        mQuCrop.setCropParam(cropParam);
        mQuCrop.setCropCallback(mtranscodeCallback);
        mQuCrop.startCrop();
        Log.d(TAG,"startCrop...path..."+ cropParam.getVideoPath());
        mTranscodeIndex++;
    }

    public interface TransCallback {
        void onError(Throwable e);

        void onProgress(int progress);

        void onComplete(List<MediaInfo> resultVideos);

        void onCancelComplete();
    }

    private CropCallback mtranscodeCallback = new CropCallback() {
        @Override
        public void onProgress(int percent) {
            int progress = (int) ((mTranscodeIndex - 1) / (float)mTranscodeTotal * 100 + percent / (float)mTranscodeTotal);
            Log.d(TAG,"progress..."+ progress);
            if(mTransCallback != null){
                mTransCallback.onProgress(progress);
            }
        }

        @Override
        public void onError(int code) {
            if(mTransCallback != null){
                mTransCallback.onError(new Throwable("transcode error, error code = "+code));
            }
        }

        @Override
        public void onComplete(long duration) {
            if(mTranscodeIndex < mTranscodeVideos.size()){
                transcodeVideo(mTranscodeIndex);
            }else{
                if(mTransCallback != null){
                    replaceOutputPath();
                    mTransCallback.onComplete(mOriginalVideos);
                }
            }
        }

        @Override
        public void onCancelComplete() {

        }
    };

    private void replaceOutputPath(){
        for(CropParam cropParam : mTranscodeVideos){
            for(MediaInfo mediaInfo : mOriginalVideos){
                if(cropParam.getVideoPath().equals(mediaInfo.filePath)){
                    mediaInfo.filePath = cropParam.getOutputPath();
                }
            }
        }
    }

    public void release() {
        AliyunCropCreator.destroyCropInstance();
    }
}
