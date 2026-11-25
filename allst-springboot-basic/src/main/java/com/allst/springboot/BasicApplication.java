package com.allst.springboot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@ImportResource(locations = {"classpath:beans.xml"})
@SpringBootApplication
public class BasicApplication {

    public static void main(String[] args) {
        // 默认开启方式
        // SpringApplication.run(BasicApplication.class, args);

        // 加载自定义配置方式开启
        SpringApplication springApplication = new SpringApplication(BasicApplication.class);
        // 创建Properties
        Properties properties = new Properties();
        // 通过当前类的ClassLoader, 这里加载app.yml于app.properties的效果是一样的
        InputStream is = BasicApplication.class.getClassLoader().getResourceAsStream("app.yml");
        try {
            // 输入流读取properties对象
            properties.load(is);
            springApplication.setDefaultProperties(properties);
            // 设置启用延迟初始化
            // springApplication.setLazyInitialization(true);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        // # 自定义的配置文件还是会和约定的配置文件形成互补
        springApplication.run(args);
    }

}
