/* 
 * @(#)BrokeUploadAction.java    Created on 2015-6-16
 * Copyright (c) 2015 ZDSoft Networks, Inc. All rights reserved.
 * $Id$
 */
package wanpeng.action;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.springframework.web.multipart.MultipartFile;

import wanpeng.entity.FileInfo;
import wanpeng.service.impl.webUploaderImpl;

import com.opensymphony.xwork2.ActionSupport;

/**
 * @author Administrator
 * @version $Revision: 1.0 $, $Date: 2015-6-16 上午10:37:12 $
 */
public class BrokeUploadAction extends ActionSupport {

    private static final long serialVersionUID = 1L;
    private String status;
    private FileInfo info;
    private MultipartFile file;
    private webUploaderImpl wu = new webUploaderImpl();
    private String uploadFolder;

    public String brokeUpload() {
        HttpServletRequest request = ServletActionContext.getRequest();
        if (status == null) { // 文件上传
            if (file != null && !file.isEmpty()) { // 验证请求不会包含数据上传，所以避免NullPoint这里要检查一下file变量是否为null
                try {
                    File target = wu.getReadySpace(info, this.uploadFolder); // 为上传的文件准备好对应的位置
                    if (target == null) {
                        return "{\"status\": 0, \"message\": \"" + wu.getErrorMsg() + "\"}";
                    }

                    file.transferTo(target); // 保存上传文件

                    // 将MD5签名和合并后的文件path存入持久层，注意这里这个需求导致需要修改webuploader.js源码3170行
                    // 因为原始webuploader.js不支持为formData设置函数类型参数，这将导致不能在控件初始化后修改该参数
                    if (info.getChunks() <= 0) {
                        if (!wu.saveMd52FileMap(info.getMd5(), target.getName())) {
                            System.out.println("文件[" + info.getMd5() + "=>" + target.getName()
                                    + "]保存关系到持久成失败，但并不影响文件上传，只会导致日后该文件可能被重复上传而已");
                        }
                    }

                    return "{\"status\": 1, \"path\": \"" + target.getName() + "\"}";

                }
                catch (IOException ex) {
                    System.out.println("数据上传失败");
                    return "{\"status\": 0, \"message\": \"数据上传失败\"}";
                }
            }
        }
        else {
            if (status.equals("md5Check")) { // 秒传验证

                String path = wu.md5Check(info.getMd5());

                if (path == null) {
                    return "{\"ifExist\": 0}";
                }
                else {
                    return "{\"ifExist\": 1, \"path\": \"" + path + "\"}";
                }

            }
            else if (status.equals("chunkCheck")) { // 分块验证

                // 检查目标分片是否存在且完整
                if (wu.chunkCheck(this.uploadFolder + "/" + info.getName() + "/" + info.getChunkIndex(),
                        Long.valueOf(info.getSize()))) {
                    return "{\"ifExist\": 1}";
                }
                else {
                    return "{\"ifExist\": 0}";
                }

            }
            else if (status.equals("chunksMerge")) { // 分块合并

                String path = wu.chunksMerge(info.getName(), info.getExt(), info.getChunks(), info.getMd5(),
                        this.uploadFolder);
                if (path == null) {
                    return "{\"status\": 0, \"message\": \"" + wu.getErrorMsg() + "\"}";
                }

                return "{\"status\": 1, \"path\": \"" + path + "\", \"message\": \"中文测试\"}";
            }
        }
        System.out.println(123);
        return status;
    }

    public void whetherBrokeUpload() {
        System.out.println("whetherBrokeUpload");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public FileInfo getInfo() {
        return info;
    }

    public void setInfo(FileInfo info) {
        this.info = info;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getUploadFolder() {
        return uploadFolder;
    }

    public void setUploadFolder(String uploadFolder) {
        this.uploadFolder = uploadFolder;
    }

}
