package com.allst.springboot.controller;

import com.allst.springboot.entity.Result;
import com.allst.springboot.entity.User;
import com.allst.springboot.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author June
 * @since 2021年06月
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    @PostMapping("/add")
    public Result<Object> addUser(@RequestBody User user) {
        userService.add(user);
        return Result.ok("添加成功", userService.getAllUsers());
    }

    @PutMapping("/update")
    public Result<List<User>> updateUser(@RequestBody User user) {
        userService.update(user);
        return Result.ok("修改成功", userService.getAllUsers());
    }

    @DeleteMapping("/{id}")
    public Result<Object> delUser(@PathVariable("id") Integer id) {
        userService.delete(id);
        return Result.ok("删除成功", userService.getAllUsers());
    }
}
