package com.allst.springboot.dfs;

import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * FastDFS工具类
 *
 * @author June
 * @since 2021年09月
 */
public class FastDFSUtils {
    static {
        try {
            ClientGlobal.initByProperties("fastdfs-client.properties");
        } catch (IOException | MyException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传文件
     * <p>
     * 访问文件：
     * http://192.168.0.200:8888/group1/M00/00/00/wKgAyGE3WI6AcG_LAAFJmxuu7hE667.png
     */
    public void uploadFile() {
        TrackerClient trackerClient = new TrackerClient();
        try {
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageServer storageServer = null;
            StorageClient1 client1 = new StorageClient1(trackerServer, storageServer);
            String s = client1.upload_file1("docker2.png", "png", null);
            // fileId: group1/M00/00/00/wKgAyGE3WI6AcG_LAAFJmxuu7hE667.png
            System.out.println("fileId: " + s);
        } catch (IOException | MyException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查看文件信息
     * fileInfo: source_ip_addr = 192.168.0.200, file_size = 84379, create_timestamp = 2021-09-07 20:18:22, crc32 = 464449041
     */
    public void queryFile() {
        // 创建tracker客户端
        TrackerClient tc = new TrackerClient();
        // 根据tracker客户端创建连接 获取到跟踪服务器对象
        TrackerServer ts = null;
        try {
            ts = tc.getConnection();
            StorageServer ss = null;
            // 定义storage客户端
            StorageClient1 client = new StorageClient1(ts, ss);
            // 查询文件信息
            FileInfo fileInfo = client.query_file_info1("group1/M00/00/00/wKgAyGE3WI6AcG_LAAFJmxuu7hE667.png");
            System.out.println("fileInfo: " + fileInfo);
            // 获取元数据信息
            NameValuePair[] metadata1 = client.get_metadata1("group1/M00/00/00/wKgAyGE3WI6AcG_LAAFJmxuu7hE667.png");
            for (NameValuePair nameValuePair : metadata1) {
                System.out.println(nameValuePair);
            }
        } catch (IOException | MyException e) {
            e.printStackTrace();
        }
    }

    public void downloadFile() {
        // 创建tracker客户端
        TrackerClient tc = new TrackerClient();
        // 根据tracker客户端创建连接
        TrackerServer ts = null;
        try {
            ts = tc.getConnection();
            StorageServer ss = null;
            // 定义storage客户端
            StorageClient1 client = new StorageClient1(ts, ss);
            // 下载
            byte[] bs = client.download_file1("group1/M00/00/00/wKgAyGE3WI6AcG_LAAFJmxuu7hE667.png");
            FileOutputStream fos = new FileOutputStream(new File("down_docker.png"));
            fos.write(bs);
            fos.close();
            System.out.println("download success ... ");
        } catch (IOException | MyException e) {
            e.printStackTrace();
        }
    }
}
