package com.allst.springboot;

import com.allst.springboot.mapper.UserMapper;
import com.allst.springboot.pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Wrapper 测试
 * @author YiYa
 * @since 2020-09-17 下午 11:41
 */
@SpringBootTest
public class WrapperTest {

    @Resource
    private UserMapper userMapper;

    @Test
    void contextLoads() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .lt(User::getAge, 20));
        users.forEach(System.out::println);
    }

    @Test
    void contextLoads2() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper
                .isNotNull("name")
                .gt("age", 20);
        userMapper.selectList(wrapper).forEach(System.out::println);
    }

    @Test
    void contextLoads3() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.between("age", 20, 26);
        Integer integer = userMapper.selectCount(wrapper);
        System.out.println(integer);
    }

    @Test
    void contextLoads4() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.notLike("name", "T")
                .likeRight("email", "t");
        List<Map<String, Object>> list = userMapper.selectMaps(wrapper);
        list.forEach(System.out::println);
    }

    @Test
    void contextLoads5() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        // 子查询
        wrapper.inSql("id", "select id from user where id < 4");
        List<Object> objects = userMapper.selectObjs(wrapper);
        objects.forEach(System.out::println);
    }
}
