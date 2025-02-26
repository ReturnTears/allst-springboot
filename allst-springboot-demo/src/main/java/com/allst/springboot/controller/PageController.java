package com.allst.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Hutu
 * @since 2025-02-26 下午 11:51
 */
@Controller
@RequestMapping("/page")
public class PageController {
    @RequestMapping("/sse")
    public ModelAndView getPage() {
        ModelAndView view = new ModelAndView();
        view.setViewName("sse");
        return view;
    }
}
