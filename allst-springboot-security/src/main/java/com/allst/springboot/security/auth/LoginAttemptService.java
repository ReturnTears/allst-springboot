package com.allst.springboot.security.auth;

import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.SecUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 连续口令错误的计数器。只在「用户名存在且口令错误」时加一，锁定期间再失败不会把锁定期往后推。
 * 用户不存在时什么都不做，避免用这个接口给不存在的用户名制造记录。
 */
@Service
public class LoginAttemptService {

    private final SecUserRepository users;
    private final SecurityProperties properties;

    public LoginAttemptService(SecUserRepository users, SecurityProperties properties) {
        this.users = users;
        this.properties = properties;
    }

    @Transactional
    public void onBadCredentials(String username) {
        users.findByUsername(username).ifPresent(user -> {
            int fails = user.getFailedAttempts() + 1;
            user.setFailedAttempts(fails);
            if (fails >= properties.getLogin().getMaxFailures()) {
                user.setAccountNonLocked(false);
                user.setLockUntil(LocalDateTime.now().plusMinutes(properties.getLogin().getLockMinutes()));
            }
            users.save(user);
        });
    }

    @Transactional
    public void onSuccess(String username) {
        users.findByUsername(username).ifPresent(this::reset);
    }

    private void reset(SecUser user) {
        if (user.getFailedAttempts() == 0 && user.getLockUntil() == null) {
            return;
        }
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setAccountNonLocked(true);
        users.save(user);
    }
}
