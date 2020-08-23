package com.allst.springboot.controller;

import com.allst.springboot.bean.Department;
import com.allst.springboot.mapper.DepartmentMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author YiYa
 * @since 2020-08-23 上午 01:15
 */
@RestController
public class DepartmentController {

    @Resource
    DepartmentMapper departmentMapper;

    @GetMapping("/dept/{id}")
    public Department getDepartment(@PathVariable("id") Integer id) {
        return departmentMapper.getDepartmentById(id);
    }

    @GetMapping("/dept")
    public Department insertDepartment(Department department) {
        departmentMapper.insertDepartment(department);
        return department;
    }

    @GetMapping("/dept/del/{id}")
    public Integer deleteDepartment(@PathVariable("id") Integer id) {
        return departmentMapper.deleteDepartmentById(id);
    }

    @GetMapping("/dept/update")
    public Integer updateDepartment(Department department) {
        return departmentMapper.updateDepartment(department);
    }
}
