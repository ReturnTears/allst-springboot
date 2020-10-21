package com.allst.springboot.controller;

import com.allst.springboot.bean.Employee;
import com.allst.springboot.mapper.EmployeeMapper;
import com.allst.springboot.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author YiYa
 * @since 2020-08-23 下午 01:38
 */
@RestController
public class EmployeeController {

    @Resource
    EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/emp/{id}")
    public Employee getEmployeeById(@PathVariable("id") Integer id) {
        return employeeMapper.getEmployeeById(id);
    }

    @GetMapping("/emp/insert")
    public Integer insertEmployee(Employee employee) {
        return employeeMapper.insertEmployee(employee);
    }

    @GetMapping("/emps")
    public List<Employee> getAllEmployees() {
        return employeeService.getEmployeeList();
    }

    @GetMapping("/empBy/{id}")
    public Employee getEmployeeBy(@PathVariable("id") Integer id) {
        return employeeService.getEmployeeBy(id);
    }
}
