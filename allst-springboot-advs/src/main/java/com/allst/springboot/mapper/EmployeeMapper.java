package com.allst.springboot.mapper;

import com.allst.springboot.bean.Employee;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author YiYa
 * @since 2020-08-23 下午 01:18
 */
@Mapper
public interface EmployeeMapper {

    Employee getEmployeeById(Integer id);

    Integer insertEmployee(Employee employee);
}
