/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.importer;

import com.aliyun.struct.common.ScaleMode;

/**
 * Created by Administrator on 2017/3/20.
 */

public class CropKey {
    public static final String VIDEO_PATH = "video_path";
    public static final String VIDEO_DURATION = "video_duration";
    public static final String VIDEO_RESOLUTION = "video_resolution";
    public static final String VIDEO_SCALE = "video_scale";
    public static final String VIDEO_QUALITY = "video_quality";
    public static final String VIDEO_FRAMERATE = "video_framerate";
    public static final String VIDEO_GOP = "video_gop";
    public static final ScaleMode SCALE_CROP = ScaleMode.PS;
    public static final ScaleMode SCALE_FILL = ScaleMode.LB;
    public static final String RESULT_KEY_CROP_PATH = "crop_path";
    public static final String RESULT_KEY_DURATION = "duration";
    public static final int RESOLUTION_3_4 = 1;
}
