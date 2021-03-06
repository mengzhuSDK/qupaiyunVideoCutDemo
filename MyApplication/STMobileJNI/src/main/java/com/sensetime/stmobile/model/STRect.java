package com.sensetime.stmobile.model;

public class STRect {
    private int left;   ///< 矩形最左边的坐标
    private int top;    ///< 矩形最上边的坐标
    private int right;  ///< 矩形最右边的坐标
    private int bottom; ///< 矩形最下边的坐标

    public STRect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}
