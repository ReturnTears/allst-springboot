package com.allst.springboot.service.impl;

import com.allst.springboot.bean.Department;
import com.allst.springboot.mapper.DepartmentMapper;
import com.allst.springboot.service.DepartmentService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author YiYa
 * @since 2020-08-23 下午 03:39
 */
@Service
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
    @Cacheable(cacheNames = {"emp"}, key = "#id", condition = "#id>7")
    @Override
    public Department getDepartmentWithCacheById(Integer id) {
        return departmentMapper.getDepartmentById(id);
    }


    @CacheEvict(value = "emp", key = "#id")
    @Override
    public void deleteDepartment(Integer id) {
        departmentMapper.deleteDepartmentById(id);
    }

    @Caching(
            cacheable = {
                    @Cacheable(value = "emp", key = "#name")
            },
            put = {
                    @CachePut(value = "emp", key = "#result.id")
            }
    )
    @Override
    public Department getDepartmentWithCacheByName(String name) {
        return departmentMapper.getDepartmentWithCacheByName(name);
    }
}
