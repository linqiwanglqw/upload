package com.lin.service;

import com.lin.param.MultipartFileParam;

import java.io.IOException;

/**
 * 存储操作的service
 */
public interface StorageService {

    /**
     * 上传文件方法
     *
     * @param param
     * @throws IOException
     */
    void uploadFileRandomAccessFile(MultipartFileParam param) throws IOException;


}
