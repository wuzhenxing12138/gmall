package com.atguigu.gmall.manage.test;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Test1 {

    @Test
    public void a() throws IOException, MyException {

        // 1
        String file = Test1.class.getClassLoader().getResource("tracker.properties").getFile();
        ClientGlobal.init(file);

        // 2
        TrackerClient trackerClient = new TrackerClient();

        // 3
        TrackerServer trackerServer = trackerClient.getConnection();

        // 4
        StorageClient storageClient = new StorageClient(trackerServer,null);

        // 测试一下fdfs上传
        String path = "d://a.gif";
        String[] abcs = storageClient.upload_file(path.getBytes(), "gif", null);

        // 返回文件路径
        String fileName = "http://192.168.222.20";
        for (int i = 0 ;i<abcs.length;i++){
            fileName += "/" + abcs[i];
        }

        System.out.println(fileName);
    }
}
