package com.allst.springboot.service.impl;

import com.allst.springboot.bean.Department;
import com.allst.springboot.mapper.DepartmentMapper;
import com.allst.springboot.service.DepartmentService;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author YiYa
 * @since 2020-08-23 下午 03:39
 */
@Service
@CacheConfig(cacheNames = "emp", cacheManager = "redisCacheManager")
public class DepartmentServiceImpl implements DepartmentService {

    @Resource
    private DepartmentMapper departmentMapper;

    /**
     * 为方法添加缓存
     * key可以使用自定义的myKeyGenerator
     *
     * @param id 参数
     * @return 结果
     */
    @Cacheable(/*cacheNames = {"emp"},*/ key = "#id", condition = "#id>7")
    @Override
    public Department getDepartmentWithCacheById(Integer id) {
        return departmentMapper.getDepartmentById(id);
    }


    @CacheEvict(/*value = "emp",*/ key = "#id")
    @Override
    public void deleteDepartment(Integer id) {
        departmentMapper.deleteDepartmentById(id);
    }

    /**
     * 加了put每次都会先调用目标方法，再将结果缓存起来
     *
     * @param name 参数
     * @return 结果
     */
    @Caching(
            cacheable = {
                    @Cacheable(value = "empName", key = "#root.args[0]")
            },
            put = {
                    @CachePut(value = "empName", key = "#result.id")
            }
    )
    @Override
    public Department getDepartmentWithCacheByName(String name) {
        return departmentMapper.getDepartmentWithCacheByName(name);
    }
}
