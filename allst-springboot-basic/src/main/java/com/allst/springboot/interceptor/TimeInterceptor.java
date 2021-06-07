package com.allst.springboot.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author June
 * @since 2021年06月
 */
public class TimeInterceptor implements HandlerInterceptor {

    LocalDateTime begin;

    Logger logger = LoggerFactory.getLogger(TimeInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        begin = LocalDateTime.now();
        logger.info("当前请求: " + request.getRequestURI() + " : 执行时间: " + begin);
        System.out.println("当前请求: " + request.getRequestURI() + " : 执行时间: " + begin);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        LocalDateTime end = LocalDateTime.now();
        Duration between = Duration.between(begin, end);

        long l = between.toMillis();
        logger.info("当前请求: " + request.getRequestURI() + " : 执行了" + l + "ms");
        System.out.println("当前请求: " + request.getRequestURI() + " : 执行了" + l + "ms");
    }
}
