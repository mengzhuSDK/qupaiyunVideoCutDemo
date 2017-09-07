/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.effects.audiomix;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.aliyun.common.logger.Logger;
import com.aliyun.demo.editor.EditorActivity;
import com.aliyun.demo.editor.R;
import com.aliyun.demo.effects.control.BaseChooser;
import com.aliyun.demo.effects.control.EffectInfo;
import com.aliyun.demo.effects.control.OnItemClickListener;
import com.aliyun.demo.effects.control.UIEditorPage;
import com.aliyun.demo.util.Common;
import com.aliyun.jasonparse.JSONSupportImpl;
import com.aliyun.qupai.editor.AliyunIPlayer;
import com.aliyun.qupaiokhttp.HttpRequest;
import com.aliyun.qupaiokhttp.StringHttpRequestCallback;
import com.aliyun.quview.PagerSlidingTabStrip;
import com.aliyun.struct.form.MusicForm;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2017/3/9.
 */
//底部5个button  选择音乐
public class AudioMixChooserMediator extends BaseChooser implements OnItemClickListener, OnClickListener {

    private static final String MUSIC_WEIGHT = "music_weight";
    private static final String MUSIC_WEIGHT_KEY = "music_weight_key";

    private ViewPager mViewPager;
    private RecyclerView mOnlineMusicRecyclerView;//在线音乐
    private RecyclerView mLocalMusicRecyclerView;//本地音乐
    private SeekBar mMusicWeightSeekBar;
    private ImageView mVoiceBtn;
    private PagerSlidingTabStrip mTabPageIndicator;
    private MusicQuery mMusicQuery;
    private EffectInfo mMusicWeightInfo = new EffectInfo();
    private LocalAudioMixAdapter mLocalMusicAdapter;//本地音乐 adapter
    private OnlineAudioMixAdapter mOnlineAudioMixAdapter;//在线音乐 adapter
    private ArrayList<MusicForm> mMusicList = new ArrayList<>();


    public static AudioMixChooserMediator newInstance() {
        AudioMixChooserMediator dialog = new AudioMixChooserMediator();
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        initResourceOnLine();
    }
    private int getMusicWeight(){
        return getContext().getSharedPreferences(MUSIC_WEIGHT, Context.MODE_PRIVATE).getInt(MUSIC_WEIGHT_KEY,50);
    }
    private void saveMusicWeight(){
        SharedPreferences.Editor editor = getContext().getSharedPreferences(MUSIC_WEIGHT, Context.MODE_PRIVATE).edit();
        int weight = mMusicWeightSeekBar.getProgress();
        editor.putInt(MUSIC_WEIGHT_KEY,weight);
        editor.commit();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        saveMusicWeight();
        if (mMusicQuery != null) {
            mMusicQuery.cancel(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = LayoutInflater.from(getActivity()).inflate(R.layout.music_view, container);
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewPager = (ViewPager) view.findViewById(R.id.music_contet_container);
        mVoiceBtn = (ImageView) view.findViewById(R.id.voice_btn);
        mVoiceBtn.setOnClickListener(this);
        mMusicWeightSeekBar = (SeekBar) view.findViewById(R.id.music_weight);
        mMusicWeightSeekBar.setMax(100);
        int musicWeight = getMusicWeight();
        mMusicWeightSeekBar.setProgress(musicWeight);
        mMusicWeightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if(mOnEffectChangeListener != null){
                    mMusicWeightInfo.isAudioMixBar = true;
                    mMusicWeightInfo.type = UIEditorPage.AUDIO_MIX;
                    mMusicWeightInfo.musicWeight = seekBar.getMax() - i;
                    mOnEffectChangeListener.onEffectChange(mMusicWeightInfo);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mOnlineMusicRecyclerView = new RecyclerView(view.getContext());
        mOnlineMusicRecyclerView.setBackgroundColor(getResources().getColor(R.color.music_back_color));
        mOnlineMusicRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        mOnlineAudioMixAdapter = new OnlineAudioMixAdapter(getActivity(), mOnlineMusicRecyclerView);
        mOnlineAudioMixAdapter.setOnItemClickListener(this);//item点击
        mOnlineMusicRecyclerView.setAdapter(mOnlineAudioMixAdapter);
        mOnlineMusicRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mLocalMusicRecyclerView = new RecyclerView(view.getContext());
        mLocalMusicRecyclerView.setBackgroundColor(getResources().getColor(R.color.music_back_color));
        mLocalMusicRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        mLocalMusicAdapter = new LocalAudioMixAdapter(getActivity());
        mLocalMusicAdapter.setOnItemClickListener(this);//item点击
        mLocalMusicRecyclerView.setAdapter(mLocalMusicAdapter);
        mLocalMusicRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setAdapter(new MusicPagerAdapter());
        mTabPageIndicator = (PagerSlidingTabStrip) view.findViewById(R.id.music_contet_container_indicator);
        mTabPageIndicator.setTextColorResource(R.color.tab_text_color_selector);
        mTabPageIndicator.setTabViewId(R.layout.layout_tab_top);
        mTabPageIndicator.setViewPager(mViewPager);
        if(mEditorService != null && mEditorService.isFullScreen()) {
            mOnlineMusicRecyclerView.setBackgroundColor(getResources().getColor(R.color.action_bar_bg_50pct));
            mLocalMusicRecyclerView.setBackgroundColor(getResources().getColor(R.color.action_bar_bg_50pct));
            mTabPageIndicator.setBackgroundColor(getResources().getColor(R.color.tab_bg_color_50pct));
        }

        //本地音乐查询
        mMusicQuery = new MusicQuery(view.getContext());
        mMusicQuery.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mMusicQuery.setOnResProgressListener(new MusicQuery.OnResProgressListener() {
            @Override
            public void onResProgress(ArrayList<MusicQuery.MediaEntity> musics) {
                mLocalMusicAdapter.setData(musics);
                //得到最后一次的选择下标
                int index = getLocalLaseSelectIndex(mEditorService.getEffectIndex(UIEditorPage.AUDIO_MIX),musics);
                mLocalMusicAdapter.setSelectedIndex(index);
                mLocalMusicRecyclerView.scrollToPosition(index);
            }
        });
        //设置默认音乐音乐
//        setDefaltMusic();
    }

    //在线初始化音乐  下载音乐   应该是——无音乐
    private void initResourceOnLine() {
        String api = Common.BASE_URL + "/api/res/type/5";
        String category = "?bundleId=" + getActivity().getApplicationInfo().packageName;
        Logger.getDefaultLogger().d("pasterUrl url = " + api + category);
        HttpRequest.get(api + category,
                new StringHttpRequestCallback() {
                    @Override
                    protected void onSuccess(String s) {
                        super.onSuccess(s);
                        JSONSupportImpl jsonSupport = new JSONSupportImpl();

                        try {
                            List<MusicForm> resourceList = jsonSupport.readListValue(s,
                                    new TypeToken<List<MusicForm>>(){}.getType());
                            if (resourceList != null && resourceList.size() > 0) {
                                mMusicList = (ArrayList<MusicForm>) resourceList;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            mMusicList = null;
                        }
                        if(mMusicList != null){
                            MusicForm empty = new MusicForm();
                            mMusicList.add(0,empty);
                        }
                        mOnlineAudioMixAdapter.setData(mMusicList);
                        int index = getLastSelectIndex(mEditorService.getEffectIndex(UIEditorPage.AUDIO_MIX),mMusicList);
                        mOnlineAudioMixAdapter.setSelectedIndex(index);
                    }

                    @Override
                    public void onFailure(int errorCode, String msg) {
                        super.onFailure(errorCode, msg);
                        if(mMusicList != null){
                            MusicForm empty = new MusicForm();
                            mMusicList.add(0,empty);
                            mOnlineAudioMixAdapter.setData(mMusicList);
                            int index = getLastSelectIndex(mEditorService.getEffectIndex(UIEditorPage.AUDIO_MIX),mMusicList);
                            mOnlineAudioMixAdapter.setSelectedIndex(index);
                        }
                    }
                });
    }
    private int getLastSelectIndex(int id,ArrayList<MusicForm> mMusicList){
        int index = 0;
        if(mMusicList == null){
            return index;
        }
        for(MusicForm musicForm : mMusicList){
            if(musicForm.getId() ==  id){
                break;
            }
            index++;
        }
        return index;
    }

    private int getLocalLaseSelectIndex(int id, ArrayList<MusicQuery.MediaEntity> musics) {
        int index = 0;
        if(musics == null) {
            return index;
        }
        for(MusicQuery.MediaEntity mediaEntity : musics) {
            String displayName = mediaEntity.display_name;
            if(displayName != null && !"".equals(displayName) && displayName.hashCode() == id) {
                break;
            }
            index++;
        }
        return index;
    }

    //设置默认音乐音乐
//    public void setDefaltMusic(){
//        EffectInfo effectInfo = new EffectInfo();
//        effectInfo.type = UIEditorPage.AUDIO_MIX;//返回数据封装
//        effectInfo.setPath("/storage/emulated/0/CCDownload/mp3/乌兰姑娘.mp3");
//        effectInfo.isLocalMusic = true;
//        effectInfo.id = 0;
//
//        //adapter item 点击事件
//        if (mOnEffectChangeListener != null) {//音效的改变，改变视频与音乐的声音比重
//            //音乐与视频音乐的比重
//            effectInfo.musicWeight = 100;
//            mOnEffectChangeListener.onEffectChange(effectInfo);
//        }
//        if(effectInfo.isLocalMusic){
//            mOnlineAudioMixAdapter.clearSelect();//清空选中
//        }else{
//            mLocalMusicAdapter.clearSelect();//清空选中
//        }
//        mEditorService.addTabEffect(UIEditorPage.AUDIO_MIX,effectInfo.id);
//
//    }

    @Override
    public boolean onItemClick(EffectInfo effectInfo, int index) {
        //adapter item 点击事件
        if (mOnEffectChangeListener != null) {//音效的改变，改变视频与音乐的声音比重
            //音乐与视频音乐的比重
            effectInfo.musicWeight = mMusicWeightSeekBar.getMax() - mMusicWeightSeekBar.getProgress();
            mOnEffectChangeListener.onEffectChange(effectInfo);
        }
        if(effectInfo.isLocalMusic){
            mOnlineAudioMixAdapter.clearSelect();//清空选中
        }else{
            mLocalMusicAdapter.clearSelect();//清空选中
        }
        mEditorService.addTabEffect(UIEditorPage.AUDIO_MIX,effectInfo.id);
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.voice_btn) {
            AliyunIPlayer mPlayer = ((EditorActivity)getActivity()).getPlayer();
            if(mPlayer != null) {
                boolean isAudioSilence = mPlayer.isAudioSilence();//是否静音
                mPlayer.setAudioSilence(!isAudioSilence);
                mVoiceBtn.setSelected(!isAudioSilence);
            }
        }
    }

    private class MusicPagerAdapter extends PagerAdapter {
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.my_music);
            } else if (position == 1) {
                return getString(R.string.local_music);
            }
            return "";
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (position == 0) {
                container.addView(mOnlineMusicRecyclerView, params);
                return mOnlineMusicRecyclerView;
            } else if (position == 1) {
                container.addView(mLocalMusicRecyclerView, params);
                return mLocalMusicRecyclerView;
            }
            return null;
        }
    }
}
