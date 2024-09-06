package com.allst.springboot.mapper;

import com.allst.springboot.pojo.Book;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @author YiYa
 * @since 2024-09-06 下午 11:25
 */
@Repository
public interface BookMapper extends BaseMapper<Book> {
    @Select("select a.id,a.name,a.author,a.status,a.create_time from book a where a.id = #{id}")
    Book selectBookById(Long id);
}
