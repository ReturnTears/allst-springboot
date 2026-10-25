package com.allst.springboot.extension.web;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 只作用于扩展点查询接口，给响应一个可识别的标记。
 */
public class StartupTraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("X-Advs-Extension", "ready");
        return true;
    }
}
