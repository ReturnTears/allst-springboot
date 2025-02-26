package com.allst.springboot.service;

import com.allst.springboot.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Hutu
 * @since 2025-02-26 下午 11:21
 */
public interface UserService {

    List<User> selectUsers();

    default List<User> getUsers() {
        return List.of(new User("Zhang", "123456"));
    }
}
