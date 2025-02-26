package com.allst.springboot.service.impl;

import com.allst.springboot.entity.User;
import com.allst.springboot.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Hutu
 * @since 2025-02-26 下午 11:28
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public List<User> selectUsers() {
        return getUsers();
    }
}
