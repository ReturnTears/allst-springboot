package com.allst.springboot.service.impl;

import com.allst.springboot.bean.Employee;
import com.allst.springboot.mapper.EmployeeMapper;
import com.allst.springboot.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author YiYa
 * @since 2020-10-21 下午 11:47
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 将方法的结果进行缓存
     * @return 结果
     */
    @Override
    @Cacheable(cacheNames = "emps", key = "#methodName")
    public List<Employee> getEmployeeList() {
        return employeeMapper.getEmployeeList();
    }
}
