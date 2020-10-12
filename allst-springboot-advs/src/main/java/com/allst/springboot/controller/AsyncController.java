package com.allst.springboot.controller;

import com.allst.springboot.service.AsyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 异步任务Controller
 * @author YiYa
 * @since 2020-10-13 上午 12:06
 */
@RestController
public class AsyncController {

    @Resource
    private AsyncService asyncService;

    @GetMapping("/noAsync")
    public String getNoAsyncHandleTask() {
        asyncService.noAsyncHandleTask();
        return "-----getNoAsyncHandleTask-----";
    }

    @GetMapping("/inAsync")
    public String getInAsyncHandleTask() {
        asyncService.inAsyncHandleTask();
        return "-----getInAsyncHandleTask-----";
    }
}