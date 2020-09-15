package com.allst.springboot;

import com.allst.springboot.mapper.UserMapper;
import com.allst.springboot.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author YiYa
 * @since 2020-09-15 上午 12:57
 */
@SpringBootTest
class MybatisPlusApplicationTests {

    /**
     * 继承了BaseMapper，所有的方法都继承至父类
     */
    @Autowired
    private UserMapper userMapper;

    @Test
    void contextLoads() {
        List<User> users = userMapper.selectList(null);
        users.forEach(System.out::println);
    }

    @Test
    void contextLoads2() {
        User user = new User();
        user.setName("KangKang");
        user.setAge(18);
        user.setEmail("709844757@qq.com");
        int insert = userMapper.insert(user);
        System.out.println(insert);
        System.out.println(user);
    }
}
