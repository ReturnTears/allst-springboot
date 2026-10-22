package com.allst.springboot.bootstrap;

import com.allst.springboot.domain.user.UserEntity;
import com.allst.springboot.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 空库时放入两条演示用户。手机号和身份证号会经过加密转换器再落库。
 */
@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(user("demo01", "张三", "13800000001", "demo01@example.com", "110101199001011234"));
        userRepository.save(user("demo02", "李四", "13800000002", "demo02@example.com", "110101199002021234"));
        log.info("已初始化演示用户 demo01、demo02");
    }

    private UserEntity user(String username, String realName, String phone, String email, String idCard) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setRealName(realName);
        entity.setPhone(phone);
        entity.setEmail(email);
        entity.setIdCard(idCard);
        entity.setStatus(1);
        entity.setCreatedBy("seed");
        return entity;
    }
}
