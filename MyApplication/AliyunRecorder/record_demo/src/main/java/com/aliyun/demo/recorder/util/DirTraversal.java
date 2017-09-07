package com.aliyun.demo.recorder.util;

/**
 * Created by Administrator on 2017/6/9.
 */


import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 文件夹遍历
 *
 * @author once
 */
public class DirTraversal {

    //no recursion
    public static LinkedList<String> listLinkedFiles(String strPath) {
        LinkedList<String> list = new LinkedList<String>();
        File dir = new File(strPath);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (!file.isDirectory())
                list.add(file.getAbsolutePath());
        }
        return list;
    }

}
