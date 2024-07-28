package com.allst.exercise.mapper;

import com.allst.exercise.model.Person;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PersonMapper extends BaseMapper<Person> {
    @Select("select * from person a where a.id = #{id}")
    Person selectById(@Param("id") Long id);
}
