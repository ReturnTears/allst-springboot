package com.allst.springboot.controller;

import org.springframework.web.bind.annotation.*;

/**
 * @author Hutu
 * @since 2024-08-23 下午 09:42
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello Demo~";
    }

    /**
     * 多个映射
     */
    @GetMapping({"/multi_1", "/multi_2", "/multi_*", "/more_uri"})
    public String multiMapping() {
        return "Hello Multi Mapping~";
    }

    /**
     * 处理地址中的参数
     */
    @GetMapping("/path/{id}")
    public String pathVariable(@PathVariable("id") Integer paramId) {
        System.out.println("id = " + paramId);
        return "Hello Path Variable 1~";
    }

    @GetMapping("/get/id")
    public String getId(@RequestParam("id") Integer paramId) {
        System.out.println("id = " + paramId);
        return "Hello Path Variable 2~";
    }

    @GetMapping("/getVal/id")
    public String getValueId(@RequestParam Integer id) {
        System.out.println("id = " + id);
        return "Hello Path Variable 3~";
    }

    @GetMapping("/name")
    public String getName(@RequestParam(value = "username", required = false) String name) {
        System.out.println("name = " + name);
        return "name ~ " + name;
    }

    /**
     * 实现动态URL
     */
    @RequestMapping(value = "/fetch/{id:[a-z]+}/{name}", method = RequestMethod.GET)
    public String getDynamicUrl(@PathVariable(value = "name") String name, @PathVariable(value = "id") String id) {
        System.out.println("name = " + name);
        System.out.println("id = " + id);
        return "DynamicUrl ~ " + name + " , " + id;
    }
}
