package com.allst.springboot;

import com.allst.boot.io.WriterTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.IOException;
import java.sql.SQLException;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 启用AOP
public class AllstSpringbootDemoApplication {
	public static void main(String[] args) throws IOException, SQLException {
		var ctx = SpringApplication.run(AllstSpringbootDemoApplication.class, args);
		WriterTemplate writerTemplate = ctx.getBean(WriterTemplate.class);
		writerTemplate.write("自动配置其实很简单!");
	}
}
