package com.allst.springboot;

import com.allst.springboot.entity.People;
import com.allst.springboot.entity.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class AllstSpringbootBasicApplicationTests {

    @Autowired
    private Person person;

    @Resource
    private People people;

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
}
