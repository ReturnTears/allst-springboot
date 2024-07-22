package com.allst.springboot;

import com.allst.boot.io.WriterTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.sql.SQLException;

@SpringBootApplication
public class AllstSpringbootDemoApplication {

	public static void main(String[] args) throws IOException, SQLException {
		var ctx = SpringApplication.run(AllstSpringbootDemoApplication.class, args);
		WriterTemplate writerTemplate = ctx.getBean(WriterTemplate.class);
		writerTemplate.write("自动配置其实很简单!");
	}

}
