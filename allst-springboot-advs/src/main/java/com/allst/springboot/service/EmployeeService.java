package com.allst.springboot.service;

import com.allst.springboot.bean.Employee;

import java.util.List;

/**
 * @author YiYa
 * @since 2020-10-21 下午 11:45
 */
public interface EmployeeService {
    List<Employee> getEmployeeList();
}
