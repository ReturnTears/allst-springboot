package com.allst.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * @author YiYa
 * @since 2020-08-19 下午 09:59
 */
@Controller
public class ThymeleafController {
    @RequestMapping("/success")
    public ModelAndView successMessage() {
        ModelAndView modelAndView = new ModelAndView("success");
        modelAndView.addObject("hello", "world ~ ");
        modelAndView.addObject("h3", "<h3>this is h3 tag</h3>");
        modelAndView.addObject("language", Arrays.asList("Java", "Scala", "JavaScript"));
        return modelAndView;
    }
}
