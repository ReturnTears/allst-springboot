package com.allst.springboot.security.auth;

import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.SecUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 连续口令错误达到阈值后锁定一段时间。锁定窗口内继续失败不会把时间往后推。见 docs/03-attack-protection.md。
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
