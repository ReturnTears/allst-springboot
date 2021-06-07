package com.allst.springboot.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author June
 * @since 2021年06月
 */
@RestController
@RequestMapping("sc")
@Api(tags = "Swagger测试相关接口")
public class SwaggerController {

    @GetMapping("/getStr/{id}")
    @ApiOperation("获取String接口")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "用户id", defaultValue = "2", required = true)})
    public String getUserById(@PathVariable("id") Integer id) {
        return "this params is " + id;
    }
}
