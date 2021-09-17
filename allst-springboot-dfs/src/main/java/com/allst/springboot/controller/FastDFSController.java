package com.allst.springboot.controller;

import com.allst.springboot.service.FastDFSClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author June
 * @since 2021年09月
 */
@RestController
@RequestMapping("/fastdfs")
public class FastDFSController {

    @Autowired
    private FastDFSClientService fastDFSClientService;

    /**
     * 长传文件
     *
     * @param file 文件
     *
     * @return 结果
     */
    @RequestMapping("/upload")
    public String uploadFile(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        String name = file.getName();
        long size = file.getSize();
        System.out.println("name: " + name + ", size : " + size);
        return fastDFSClientService.uploadFile(bytes, size, extension);
    }

    /**
     * 下载文件
     *
     * @param fileUrl  文件路径
     * @param response 响应
     */
    public void downloadFile(String fileUrl, HttpServletResponse response) throws IOException {
        byte[] bytes = fastDFSClientService.downloadFile(fileUrl);
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileUrl, "UTF-8"));
        response.setCharacterEncoding("UTF-8");
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert outputStream != null;
                outputStream.flush();
                outputStream.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
