package com.lin.service.impl;

import com.lin.param.MultipartFileParam;
import com.lin.service.StorageService;
import com.lin.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 存储操作的service, 具体实现可以由redis, mysql, mongodb等来实现
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {
    // 保存文件的根目录
    private Path rootPath;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 这个必须与前端设定的值一致
    @Value("${breakpoint.upload.chunkSize}")
    private long CHUNK_SIZE;

    @Value("${breakpoint.upload.dir}")
    private String finalDirPath;

    @Autowired
    public StorageServiceImpl(@Value("${breakpoint.upload.dir}") String location) {
        this.rootPath = Paths.get(location);
    }


    @Override
    public void uploadFileRandomAccessFile(MultipartFileParam param) throws IOException {
        //文件名
        String fileName = param.getName();
        //文件保存路径+md5
        String tempDirPath = finalDirPath + param.getMd5();
        //临时文件名，将分片数据全部覆盖这个临时文件
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(tempDirPath);
        //File第一个参数是目录，第二个参数是文件名
        File tmpFile = new File(tempDirPath, tempFileName);
        //如果目录不存在则创建
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        //创建一个随机访问文件流,开放阅读和写作。如果该文件尚不存在，则将尝试创建它。
        RandomAccessFile accessTmpFile = new RandomAccessFile(tmpFile, "rw");
        long offset = CHUNK_SIZE * param.getChunk();
        // 定位到该分片的偏移量
        accessTmpFile.seek(offset);
        // 写入该分片数据
        accessTmpFile.write(param.getFile().getBytes());
        // 释放
        accessTmpFile.close();

        //检查并修改文件上传进度
        boolean isOk = checkAndSetUploadProgress(param, tempDirPath);
        if (isOk) {
            //修改文件名
            boolean flag = renameFile(tmpFile, fileName);
            System.out.println("upload complete !!" + flag + " name=" + fileName);
        }
    }

    /**
     * 检查并修改文件上传进度
     *
     * @param param
     * @param uploadDirPath
     * @return
     * @throws IOException
     */
    private boolean checkAndSetUploadProgress(MultipartFileParam param, String uploadDirPath) throws IOException {
        //文件名
        String fileName = param.getName();
        //创建.conf对象
        File confFile = new File(uploadDirPath, fileName + ".conf");
        RandomAccessFile accessConfFile = new RandomAccessFile(confFile, "rw");
        // 把该分段标记为 true 表示完成
        System.out.println("当前上传第：" + param.getChunk() + "个，总数："+param.getChunks());
        //设置文件长度
        accessConfFile.setLength(param.getChunks());
        //设置文件指针偏移量，从该文件的开头开始测量，下一次读取或写入发生在该位置。
        accessConfFile.seek(param.getChunk());
        // 写入该分片数据，并设置为127
        accessConfFile.write(Byte.MAX_VALUE);

        // completeList 检查是否全部完成,如果数组里是否全部都是(全部分片都成功上传)
        byte[] completeList = FileUtils.readFileToByteArray(confFile);
        byte isComplete = Byte.MAX_VALUE;
        for (int i = 0; i < completeList.length && isComplete == Byte.MAX_VALUE; i++) {
            // 与运算, 如果有部分没有完成则 isComplete 不是 Byte.MAX_VALUE，如果相同就是Byte.MAX_VALUE
            isComplete = (byte) (isComplete & completeList[i]);
            System.out.println("当前文件片段：" + i + "上传情况（127正常）" + completeList[i]);
        }
        // 释放
        accessConfFile.close();
        //全部上传完成是值是127
        if (isComplete == Byte.MAX_VALUE) {
            stringRedisTemplate.opsForHash().put(Constants.FILE_UPLOAD_STATUS, param.getMd5(), "true");
            stringRedisTemplate.opsForValue().set(Constants.FILE_MD5_KEY + param.getMd5(), uploadDirPath + "/" + fileName);
            return true;
        } else {
            //判断redis中是否有判断的标识符值，如果没有，则存入false
            if (!stringRedisTemplate.opsForHash().hasKey(Constants.FILE_UPLOAD_STATUS, param.getMd5())) {
                stringRedisTemplate.opsForHash().put(Constants.FILE_UPLOAD_STATUS, param.getMd5(), "false");
            }
            //判断redis中是否有值，如果有，则存入.conf
            if (stringRedisTemplate.hasKey(Constants.FILE_MD5_KEY + param.getMd5())) {
                stringRedisTemplate.opsForValue().set(Constants.FILE_MD5_KEY + param.getMd5(), uploadDirPath + "/" + fileName + ".conf");
            }
            return false;
        }
    }

    /**
     * 文件重命名
     *
     * @param toBeRenamed   将要修改名字的文件
     * @param toFileNewName 新的名字
     * @return
     */
    public boolean renameFile(File toBeRenamed, String toFileNewName) {
        // 检查要重命名的文件是否存在，是否是文件
        if (!toBeRenamed.exists() || toBeRenamed.isDirectory()) {
            log.info("文件不存在: " + toBeRenamed.getName());
            return false;
        }
        //获取路径
        String p = toBeRenamed.getParent();
        //File.separatorChar：分隔符
        File newFile = new File(p + File.separatorChar + toFileNewName);
        // 修改文件名
        return toBeRenamed.renameTo(newFile);
    }

}
