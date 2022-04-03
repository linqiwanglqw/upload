package com.lin.controller;

import com.lin.param.MultipartFileParam;
import com.lin.service.StorageService;
import com.lin.utils.Constants;
import com.lin.vo.ResultStatus;
import com.lin.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping(value = "/index")
@Slf4j
public class IndexController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private StorageService storageService;

    /**
     * 秒传判断, 断点判断
     *
     * @param md5
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "checkFileMd5", method = RequestMethod.POST)
    @ResponseBody
    public Object checkFileMd5(String md5) throws IOException {
        // 从redis中获取值hash中的value（key——》field——》value）
        Object processingObj = stringRedisTemplate.opsForHash().get(Constants.FILE_UPLOAD_STATUS, md5);
        // 如果为空则返回null认为该文件没有上传
        if (processingObj == null) {
            return new ResultVo(ResultStatus.NO_HAVE);
        }
        String processingStr = processingObj.toString();
        //转换为boolean类型，是true（不区分大小写）则是true，其他都是false
        boolean processing = Boolean.parseBoolean(processingStr);
        //获取value为文件的路径名
        String value = stringRedisTemplate.opsForValue().get(Constants.FILE_MD5_KEY + md5);
        //已经上传了
        if (processing) {
            return new ResultVo(ResultStatus.IS_HAVE, value);
        } else {
            File confFile = new File(value);
            //将文件的内容读入字节数组
            byte[] completeList = FileUtils.readFileToByteArray(confFile);
            List<String> missChunkList = new LinkedList<>();
            for (int i = 0; i < completeList.length; i++) {
                //Byte.MAX_VALUE=127
                if (completeList[i] != Byte.MAX_VALUE) {
                    missChunkList.add(i + "");
                }
            }
            return new ResultVo<>(ResultStatus.ING_HAVE, missChunkList);
        }
    }


    /**
     * 上传文件
     *
     * @param param
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/fileUpload", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity fileUpload(MultipartFileParam param, HttpServletRequest request) {
        //返回值为true—带文件上传的表单；返回值false—普通表单。
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {
            log.info("上传文件start!");
            try {
                 storageService.uploadFileRandomAccessFile(param);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("文件上传失败, {}", param.toString());
            }
            log.info("上传文件end!");
        }
        //在响应的boby里面加上了上传成功
        return ResponseEntity.ok().body("上传成功!");
    }

}
