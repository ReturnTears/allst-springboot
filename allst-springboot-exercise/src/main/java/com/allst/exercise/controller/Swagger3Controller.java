package com.allst.exercise.controller;

import com.allst.exercise.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2024-08-02 下午 09:53
 */
@RestController
@RequestMapping("/swag")
@Api(tags = "Swagger2测试")
public class Swagger3Controller {
    @RequestMapping("/hello")
    @ApiOperation(value="获取Swagger消息", notes="根据XXX对象获取YYY信息")
    @ApiImplicitParam(name = "user", value = "XXX实体YYY", required = true, dataType = "User")
    public User hello() {
        System.out.println("hello swagger3");
        User user = new User();
        user.setId(1L);
        user.setName("kang");
        user.setAge(18);
        return user;
    }
}
