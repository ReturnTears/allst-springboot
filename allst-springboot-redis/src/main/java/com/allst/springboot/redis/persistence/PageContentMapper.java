package com.allst.springboot.redis.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PageContentMapper {

    @Select("SELECT id, slug, title, content, updated_at FROM t_page WHERE slug = #{slug}")
    PageContent findBySlug(String slug);

    @Select("SELECT COUNT(1) FROM t_page")
    int count();

    @Insert("INSERT INTO t_page(id, slug, title, content, updated_at) " +
            "VALUES(#{id}, #{slug}, #{title}, #{content}, CURRENT_TIMESTAMP)")
    int insert(PageContent page);

    @Update("UPDATE t_page SET title = #{title}, content = #{content}, updated_at = CURRENT_TIMESTAMP WHERE slug = #{slug}")
    int update(PageContent page);
}
