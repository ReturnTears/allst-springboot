package com.allst.springboot.redis.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BehaviorMapper {

    @Insert("INSERT INTO t_behavior(user_id, item_id, action, score, created_at) " +
            "VALUES(#{userId}, #{itemId}, #{action}, #{score}, CURRENT_TIMESTAMP)")
    int insert(BehaviorEvent event);

    @Select("SELECT id, user_id, item_id, action, score, created_at FROM t_behavior " +
            "WHERE user_id = #{userId} ORDER BY id DESC LIMIT 20")
    List<BehaviorEvent> latest(String userId);
}
