package com.allst.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * /user/login
 * @author June 2018-01-20 下午 04:34
 * @version 1.0
 */
@Controller
public class LoginController {

    @PostMapping("/user/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        Map<String, Object> map,
                        HttpSession session) {
        if (!StringUtils.isEmpty(username) && "123456".equals(password)) {
            // login success, 放置重复提交，可以使用重定向
            session.setAttribute("loginUser", username);
            return "redirect:/main.html";
        } else {
            // login error
            map.put("msg", "用户名密码错误");
            return "login";
        }
    }
}
