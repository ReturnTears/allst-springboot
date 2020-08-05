package com.allst.springboot.config;

import com.allst.springboot.service.AnnoStartService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义配置类
 * @author YiYa
 * @since 2020-08-05 下午 11:21
 */
@Configuration
public class AppConfig {
    /**
     * 将方法的返回值添加到容器中，容器中这个组件的id默认是方法名
     * @return AnnoStartService
     */
    @Bean
    public AnnoStartService getAnnoStartService() {
        return new AnnoStartService();
    }

}
