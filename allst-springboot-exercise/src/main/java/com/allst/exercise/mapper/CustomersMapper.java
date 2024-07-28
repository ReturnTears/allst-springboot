package com.allst.exercise.mapper;

import com.allst.exercise.model.Customers;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author Hutu
 * @since 2024-07-28 上午 12:02
 */
public interface CustomersMapper extends BaseMapper<Customers>  {
    /**
     * 使用Mybatis的原生注解Select
     */
    @Select("select * from customers where id = #{id}")
    Customers selectById(@Param("id") Long id);
}
