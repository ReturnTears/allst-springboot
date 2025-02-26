package com.allst.springboot.entity;

import lombok.Getter;

/**
 * @author Hutu
 * @since 2025-02-26 下午 11:19
 */
@Getter
public class User {
    private String username;
    private String password;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
