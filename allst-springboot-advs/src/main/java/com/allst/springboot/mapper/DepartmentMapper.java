package com.allst.springboot.mapper;

import com.allst.springboot.bean.Department;
import org.apache.ibatis.annotations.*;

/**
 * 定义操作数据库的mapper,
 * 如果使用注解的方式获取数据,则需要在启动类中配置注解@MapperScan("xxx")
 * 如果有很多的*Mapper文件，那么可以在主配置类中指定注解@MapperScan("com.allst.springboot.mapper")
 * @author YiYa
 * @since 2020-08-23 上午 01:10
 */
@Mapper
public interface DepartmentMapper {

    @Select("select * from department where id = #{id}")
    Department getDepartmentById(Integer id);

    @Delete("delete from department where id = #{id}")
    int deleteDepartmentById(Integer id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into department (departmentName, employee_nums) values (#{departmentName}, #{employeeNums})")
    int insertDepartment(Department department);

    @Update("update department set departmentName = #{departmentName}, employee_nums = #{employeeNums} where id = #{id}")
    int updateDepartment(Department department);

    @Select("select * from department where departmentName = #{name}")
    Department getDepartmentWithCacheByName(String name);
}
