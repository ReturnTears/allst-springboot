package com.allst.springboot.controller;

import com.allst.springboot.annoation.MethodExporter;
import com.google.common.collect.Maps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * 简单测试自定义自定义注解
 *
 * @author June
 * @since 2022-06-12
 */
@RestController
@RequestMapping("/simple")
public class SimpleController {

    /**
     * 目标方法上增加自定义注解
     *
     */
    @MethodExporter
    @GetMapping("/list")
    public Map<String, Object> list(int page, int rows) {
        Map<String, Object> result = Maps.newLinkedHashMap();
        result.put("code", "0");
        result.put("message", "success");

        try {
            Thread.sleep(new Random().nextInt(2000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
