package com.allst.springboot.service.impl;

import com.allst.springboot.bean.Department;
import com.allst.springboot.mapper.DepartmentMapper;
import com.allst.springboot.service.DepartmentService;
import org.springframework.cache.annotation.Cacheable;
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
     * @param id 参数
     * @return 结果
     */
    @Cacheable(cacheNames = {"emp"}, key = "#id", condition = "#id>7")
    @Override
    public Department getDepartmentWithCacheById(Integer id) {
        return departmentMapper.getDepartmentById(id);
    }
}
