package com.allst.springboot.redis.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT id, name, price, stock, detail, category, updated_at FROM t_product WHERE id = #{id}")
    Product findById(Long id);

    @Select("SELECT id, name, price, stock, detail, category, updated_at FROM t_product ORDER BY id")
    List<Product> findAll();

    @Select("SELECT COUNT(1) FROM t_product")
    int count();

    @Select("SELECT DISTINCT category FROM t_product ORDER BY category")
    List<String> categories();

    @Insert("INSERT INTO t_product(id, name, price, stock, detail, category, updated_at) " +
            "VALUES(#{id}, #{name}, #{price}, #{stock}, #{detail}, #{category}, CURRENT_TIMESTAMP)")
    int insert(Product product);

    @Update("UPDATE t_product SET name = #{name}, price = #{price}, stock = #{stock}, detail = #{detail}, " +
            "category = #{category}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Product product);

    @Update("UPDATE t_product SET stock = stock - #{qty}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);
}
