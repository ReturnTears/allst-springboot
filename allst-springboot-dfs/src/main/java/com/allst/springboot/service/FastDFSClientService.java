package com.allst.springboot.service;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * @author June
 * @since 2021年09月
 */
@Component
public class FastDFSClientService {

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    /**
     * 上传文件
     * @param bytes bytes
     * @param fileSize fileSize
     * @param extension extension
     * @return 结果
     */
    public String uploadFile(byte[] bytes, long fileSize, String extension) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        StorePath storePath = fastFileStorageClient.uploadFile(byteArrayInputStream, fileSize, extension, null);
        System.out.println(storePath.getGroup() + " : " + storePath.getPath() + " : " + storePath.getFullPath());
        return storePath.getFullPath();
    }

    /**
     * 下载文件
     * @param fileUrl 文件路径
     * @return 结果
     */
    public byte[] downloadFile(String fileUrl) {
        String group = fileUrl.substring(0, fileUrl.indexOf("/"));
        String path = fileUrl.substring(fileUrl.indexOf("/") + 1);
        DownloadByteArray downloadByteArray = new DownloadByteArray();
        byte[] bytes = fastFileStorageClient.downloadFile(group, path, downloadByteArray);
        System.out.println(bytes.length);
        return bytes;
    }
}
