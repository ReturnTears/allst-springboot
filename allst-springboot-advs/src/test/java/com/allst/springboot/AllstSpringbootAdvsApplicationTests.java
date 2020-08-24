package com.allst.springboot;

import com.allst.springboot.bean.Department;
import com.allst.springboot.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
class AllstSpringbootAdvsApplicationTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    private DepartmentService departmentService;

    @Test
    void contextLoads() {
        stringRedisTemplate.opsForValue().set("hello", "world");
        String result = stringRedisTemplate.opsForValue().get("user");
        System.out.println(result);
    }

    @Test
    void contextCacheLoads() {
        final Integer id = 9;
        Department department = departmentService.getDepartmentWithCacheById(id);
        redisTemplate.opsForValue().set("department::" + id, department.toString());

        Object result = redisTemplate.opsForValue().get("department::" + id);
        assert result != null;
        System.out.println(result.toString());
    }

}
