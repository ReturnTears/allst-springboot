package com.allst.springboot.controller;

import cn.hutool.core.util.IdUtil;
import com.allst.springboot.client.SseClient;
import com.allst.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/**
 * @author Hutu
 * @since 2025-02-26 下午 11:18
 */
@Controller
@RequestMapping("/sse")
public class SSEController {

    @Autowired
    private UserService userService;

    @Autowired
    private SseClient sseClient;

    @GetMapping("/html")
    public String index(ModelMap model) {
        //String uid = IdUtil.fastUUID();
        model.put("uid", "101");
        return "sse";
    }

    @CrossOrigin
    @GetMapping("/createSse")
    public SseEmitter createConnect(String uid) {
        return sseClient.createSse(uid);
    }

    @CrossOrigin
    @GetMapping("/sendMsg")
    @ResponseBody
    public String sseChat(String uid) {
        for (int i = 0; i < 10; i++) {
            sseClient.sendMessage(uid, "no" + i, IdUtil.fastUUID());
        }
        return "ok";
    }

    /**
     * 关闭连接
     */
    @CrossOrigin
    @GetMapping("/closeSse")
    public void closeConnect(String uid) {

        sseClient.closeSse(uid);
    }
}
