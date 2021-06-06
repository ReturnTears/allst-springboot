package com.allst.springboot.controller;

import com.allst.springboot.entity.Result;
import com.allst.springboot.entity.User;
import com.allst.springboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author June
 * @since 2021年06月
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable("id") Integer id) {
        User userById = userService.getUserById(id);
        return new Result<User>().setCode(200).setMessage("成功").setData(userById);
    }

    @GetMapping("/str/{id}")
    public String getString(@PathVariable("id") Integer id) {
        return userService.getStringBy(id);
    }

    @GetMapping("/user/{id}")
    public User getUserString(@PathVariable("id") Integer id) {
        return userService.getUserBy(id);
    }

    @DeleteMapping("/{id}")
    public Result<User> delUser(@PathVariable("id") Integer id) {
        userService.delete(id);
        return new Result<User>(200, "删除成功");
    }
}
