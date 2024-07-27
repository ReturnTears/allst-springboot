package com.allst.boot.controller;

import com.allst.boot.service.InfoService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.time.LocalDateTime;

/**
 * @author Hutu
 * @since 2024-07-27 上午 10:02
 */
@RestController
public class InfoController {

    @Resource
    private InfoService infoService;

    @GetMapping("/info")
    public void download(HttpServletResponse response) {
        try {
            XWPFDocument document = infoService.doc2Word();
            response.reset();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-disposition", "attachment;filename=info_world_" + LocalDateTime.now().getNano() + ".docx");
            OutputStream os = response.getOutputStream();
            document.write(os);
            os.close();
        } catch (Exception ignored) {}
    }
}
