package com.allst.springboot.repository;

import com.allst.springboot.entity.TUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA完成对数据库的操作
 * @author YiYa
 * @since 2020-08-23 下午 02:09
 */
public interface TUserRepository extends JpaRepository<TUser, Integer> {

}
