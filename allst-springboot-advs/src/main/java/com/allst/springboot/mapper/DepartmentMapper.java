package com.allst.springboot.mapper;

import com.allst.springboot.bean.Department;
import org.apache.ibatis.annotations.*;

/**
 * 定义操作数据库的mapper
 * @author YiYa
 * @since 2020-08-23 上午 01:10
 */
@Mapper
public interface DepartmentMapper {

    @Select("select * from department where id = #{id}")
    Department getDepartmentById(Integer id);

    @Delete("delete from department where id = #{id}")
    int deleteDepartmentById(Integer id);

    @Insert("insert into department (departmentName) values (#{departmentName} )")
    int insertDepartment(Department department);

    @Update("update department set departmentName = #{departmentName} where id = #{id}")
    int updateDepartment(Department department);
}
