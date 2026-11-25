package com.allst.springboot;

import org.allst.boot.io.WriterTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.IOException;
import java.sql.SQLException;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 启用AOP
public class DemoApplication {
	public static void main(String[] args) throws IOException, SQLException {
		var ctx = SpringApplication.run(DemoApplication.class, args);
		WriterTemplate writerTemplate = ctx.getBean(WriterTemplate.class);
		writerTemplate.write("自动配置其实很简单!");
	}
}
