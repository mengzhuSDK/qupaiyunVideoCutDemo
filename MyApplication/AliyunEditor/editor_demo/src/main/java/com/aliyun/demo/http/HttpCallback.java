/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.demo.http;

/**
 * Created by apple on 2017/3/13.
 */

public interface HttpCallback<T> {
    void onSuccess(T result);
    void onFailure(Throwable e);
}
