package com.allst.springboot.service;

import com.allst.springboot.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author June
 * @since 2021年06月
 */
@Service
public class UserService {

    private static final Map<Integer, User> users = new HashMap<>();

    static {
        users.put(1, new User(1, "ZhangSan", "BeiJing"));
        users.put(2, new User(2, "Lisi", "HangZHou"));
        users.put(3, new User(3, "Wangwu", "ShangHai"));
        users.put(4, new User(4, "XUs", "chengdu"));
        users.put(5, new User(5, "maliu", "xian"));
    }

    public User getUserById(Integer id) {
        return users.get(id);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public void update(User user) {
        users.replace(user.getId(), user);
    }

    public void add(User user) {
        Integer newId = users.size() + 1;
        user.setId(newId);
        users.put(newId, user);
    }

    public void delete(Integer id) {
        users.keySet().removeIf(key -> key.equals(id));
    }

    public String getStringBy(Integer id) {
        User user = users.get(id);
        return user == null ? "this params is not suitable username..." : user.getUsername() ;
    }

    public User getUserBy(Integer id) {
        User user = users.get(id);
        return user == null ? new User("unknown username...", "unknown address...") : user;
    }
}
