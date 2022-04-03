package com.lin.service;

import com.lin.param.MultipartFileParam;

import java.io.IOException;

/**
 * 存储操作的service
 */
public interface StorageService {
//
//    /**
//     * 删除全部数据
//     */
//    void deleteAll();
//
//    /**
//     * 初始化方法
//     */
//    void init();

    /**
     * 上传文件方法1
     *
     * @param param
     * @throws IOException
     */
    void uploadFileRandomAccessFile(MultipartFileParam param) throws IOException;


}
