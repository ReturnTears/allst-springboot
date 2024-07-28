package com.allst.exercise.mapper;

import com.allst.exercise.model.Customers;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * 使用Mybatis的原生Xml查询数据
     */
    Customers selectByName(@Param("name") String name);

    Page<Customers> selectPageList();

    //List<Customers> selectCursorList();
}
