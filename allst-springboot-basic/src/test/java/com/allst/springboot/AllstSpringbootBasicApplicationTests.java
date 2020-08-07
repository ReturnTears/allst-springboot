package com.allst.springboot;

import com.allst.springboot.entity.Human;
import com.allst.springboot.entity.People;
import com.allst.springboot.entity.Person;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

@SpringBootTest
class AllstSpringbootBasicApplicationTests {
    /**
     * 记录器
     */
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Person person;

    @Resource
    private People people;

    @Resource
    private Human human;

    @Autowired
    ApplicationContext ioc;

    /**
     * Person(name=SpringBoot, age=18, birth=null, maps={key=value}, lists=[wahaha, xixi])
     */
    @Test
    void contextLoads() {
        System.out.println(person);
    }

    /**
     * People(name=SpringBoot, age=18, tenAge=28)
     */
    @Test
    void peopleLoads() {
        System.out.println(people);
    }

    /**
     * 测试@PropertySource
     */
    @Test
    public void humanLoads() {
        System.out.println(human);
    }

    /**
     * 添加注解@ImportResource即可让配置文件生效
     */
    @Test
    public void contextLoad() {
        boolean result = ioc.containsBean("helloService");
        System.out.println(result);
    }

    /**
     * 测试配置类添加组件到容器
     */
    @Test
    public void getStartServiceLoads() {
        boolean result = ioc.containsBean("getAnnoStartService");
        System.out.println(result);
    }

    /**
     * 日志级别由低到高
     */
    @Test
    public void logLoads() {
        logger.trace("this is trace log.");
        logger.debug("this is debug log.");
        logger.info("this is info log.");
        logger.warn("this is warn log.");
        logger.error("this is error log.");
    }
}
