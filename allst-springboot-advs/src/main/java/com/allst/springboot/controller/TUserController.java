package com.allst.springboot.controller;

import com.allst.springboot.entity.TUser;
import com.allst.springboot.repository.TUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author YiYa
 * @since 2020-08-23 下午 02:19
 */
@RestController
public class TUserController {

    @Resource
    TUserRepository tUserRepository;

    @GetMapping("/user/{id}")
    public TUser getTUserById(@PathVariable("id") Integer id) {
        return tUserRepository.getOne(id);
    }

    @GetMapping("/user/insert")
    public TUser insertTUser(TUser tUser) {
        return tUserRepository.save(tUser);
    }
}
