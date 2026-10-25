package com.allst.springboot.extension.web;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 装配期扩展。只拦截新的回溯接口，不改已有部门、员工和 Druid 路径。
 */
@Configuration
public class AdvsWebMvcExtension implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new StartupTraceInterceptor()).addPathPatterns("/extension", "/extension/**");
        StartupTraceLog.record(StartupPhase.WEB_MVC_CONFIGURER,
                "已为 /extension/** 注册拦截器，响应头 X-Advs-Extension=ready。");
    }
}
