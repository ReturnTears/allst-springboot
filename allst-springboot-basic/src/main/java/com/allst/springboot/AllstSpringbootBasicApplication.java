package com.allst.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ImportResource(locations = {"classpath:beans.xml"})
@SpringBootApplication
public class AllstSpringbootBasicApplication {

    public static void main(String[] args) {
        // 默认开启方式
        // SpringApplication.run(AllstSpringbootBasicApplication.class, args);

        // 加载自定义配置方式开启
        SpringApplication springApplication = new SpringApplication(AllstSpringbootBasicApplication.class);
        // 创建Properties
        Properties properties = new Properties();
        // 通过当前类的ClassLoader, 这里加载app.yml于app.properties的效果是一样的
        InputStream is = AllstSpringbootBasicApplication.class.getClassLoader().getResourceAsStream("app.yml");
        try {
            // 输入流读取properties对象
            properties.load(is);
            springApplication.setDefaultProperties(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // # 自定义的配置文件还是会和约定的配置文件形成互补
        springApplication.run(args);
    }

}
