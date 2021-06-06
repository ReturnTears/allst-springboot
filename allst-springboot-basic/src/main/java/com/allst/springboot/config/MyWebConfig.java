package com.allst.springboot.config;

import com.allst.springboot.component.LoginHandlerInterceptor;
import com.allst.springboot.component.MyLocaleResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * 快捷键：CTRL + O 打开重写方法列表面板
 * 使用WebMvcConfigurerAdapter来扩展SpringMVC的功能
 * @author June 2018-01-19 下午 02:40
 * @version 1.0
 */
@Configuration
public class MyWebConfig extends WebMvcConfigurerAdapter {

//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        super.addViewControllers(registry);
//        // 浏览器发送/suc请求来到success
//        registry.addViewController("/suc").setViewName("success");
//    }

    /**
     * 所有的WebMvcConfigurerAdapter组件都会一起起作用
     *  将组件注入到容器中
     *
     *  to do 为了测试restTemplate 先注释掉登录拦截
     */
    /*@Bean
    public WebMvcConfigurerAdapter webMvcConfigurerAdapter() {
        WebMvcConfigurerAdapter adapter = new WebMvcConfigurerAdapter() {

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/").setViewName("login");
                registry.addViewController("/index.html").setViewName("login");
                registry.addViewController("/main.html").setViewName("dashboard");
            }
            // 注册拦截器
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new LoginHandlerInterceptor()).addPathPatterns("/**")
                        .excludePathPatterns("/static/asserts/**")
                        .excludePathPatterns("/i18n/**")
                        .excludePathPatterns("/resources/**")
                        .excludePathPatterns("/", "/index.html", "/user/login");
            }
        };
        return adapter;
    }*/

    @Bean
    public LocaleResolver localeResolver() {
        return new MyLocaleResolver();
    }
}
