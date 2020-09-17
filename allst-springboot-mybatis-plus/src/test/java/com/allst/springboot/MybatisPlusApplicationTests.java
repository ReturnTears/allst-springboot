package com.allst.springboot;

import com.allst.springboot.mapper.UserMapper;
import com.allst.springboot.pojo.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        user.setName("XiaoHu");
        user.setAge(22);
        user.setEmail("XiaoHu@qq.com");
        int insert = userMapper.insert(user);
        System.out.println(insert);
        System.out.println(user);
    }

    @Test
    void contextLoad3() {
        User user = new User();
        user.setId(1305890062833913858L);
        user.setAge(18);
        user.setName("小胡子同学");
        user.setEmail("xiaohuzitx@qq.com");
        user.setVersion(1);
        int i = userMapper.updateById(user);
        System.out.println(i);
    }

    /**
     * 测试乐观锁
     */
    @Test
    void contextLoad4() {
        User user = userMapper.selectById(1L);
        user.setName("jiaduobao");
        user.setEmail("jiaduobao@qq.com");
        userMapper.updateById(user);
    }

    /**
     * 批量查询
     */
    @Test
    void contextLoad5() {
        List<User> user = userMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L));
        System.out.println(user.toString());
    }

    /**
     * 条件查询Map
     */
    @Test
    void contextLoad6() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Tom");
        List<User> users = userMapper.selectByMap(map);
        users.forEach(System.out::println);
    }

    /**
     * 测试分页查询
     * 直接使用
     * current： 当前页
     * size： 大小
     */
    @Test
    void contextLoad7() {
        Page<User> userPage = new Page<>(1, 5);
        userMapper.selectPage(userPage, null);
        userPage.getRecords().forEach(System.out::println);
    }

    /**
     * 删除测试
     */
    @Test
    void contextLoad8() {
        //userMapper.deleteById(1L);
        //userMapper.deleteBatchIds(Arrays.asList(1L, 2L, 3L));
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Tom");
        userMapper.deleteByMap(map);
    }

    /**
     * 逻辑删除
     */
    @Test
    void contextLoad9() {
        userMapper.deleteById(1L);
    }
}
