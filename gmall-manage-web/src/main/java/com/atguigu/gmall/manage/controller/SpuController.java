package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.manage.test.Test1;
import com.atguigu.gmall.service.SpuService;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class SpuController {

    @Reference
    SpuService spuService;

    @Value("${fileName}")
    String fileName;

    @RequestMapping(value = "fileUpload", method = RequestMethod.POST)
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile file) {

        String path = fileName;
        // 上传图片
        String fileStr = Test1.class.getClassLoader().getResource("tracker.properties").getFile();
        try {
            ClientGlobal.init(fileStr);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StorageClient storageClient = new StorageClient(trackerServer, null);
        // 测试一下fdfs上传

        try {
            String originalFilename = file.getOriginalFilename();
            String[] abcs = storageClient.upload_file(file.getBytes(), StringUtils.substringAfterLast(originalFilename, "."), null);
            // 返回文件路径
            for (int i = 0; i < abcs.length; i++) {
                path += "/" + abcs[i];
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        return path;
    }

    @RequestMapping("saveSpu")
    @ResponseBody
    public String saveSpu(SpuInfo spuInfo) {

        // 调用service的保存服务
        spuService.saveSpu(spuInfo);
        return "success";
    }

    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttrList(@RequestParam Map<String, String> map) {
        List<BaseSaleAttr> baseSaleAttrList = spuService.baseSaleAttrList();
        return baseSaleAttrList;
    }

    @RequestMapping("getSpuList")
    @ResponseBody
    public List<SpuInfo> getSpuList(@RequestParam Map<String, String> map) {

        String catalog3Id = map.get("catalog3Id");
        List<SpuInfo> spuList = spuService.getSpuList(catalog3Id);

        return spuList;
    }
}
