package com.allst.springboot.controller;

import com.allst.springboot.dao.EmployeeDao;
import com.allst.springboot.entities.Employee;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * @author YiYa
 * @since 2020-08-22 下午 12:34
 */
@Controller
public class EmployeeController {

    @Resource
    private EmployeeDao employeeDao;

    /**
     * 返回所有员工列表
     * @return 结果
     */
    @GetMapping("/emps")
    public String list(Model model) {
        Collection<Employee> employees = employeeDao.getAll();
        model.addAttribute("emps", employees);
        return "emps/list";
    }
}
