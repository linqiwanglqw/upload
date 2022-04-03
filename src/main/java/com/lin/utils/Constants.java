package com.lin.utils;

/**
 * 常量表
 */
public interface Constants {
    /**
     * 保存文件所在路径会作用于redis中key
     */
    String FILE_MD5_KEY = "FILE_MD5:";

    /**
     * 保存上传文件的状态会作用于redis中key
     */
    String FILE_UPLOAD_STATUS = "FILE_UPLOAD_STATUS";

}
