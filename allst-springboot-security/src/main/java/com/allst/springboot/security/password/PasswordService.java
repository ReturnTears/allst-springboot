package com.allst.springboot.security.password;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.SecurityProperties;
import com.allst.springboot.domain.entity.PasswordHistory;
import com.allst.springboot.domain.entity.SecUser;
import com.allst.springboot.domain.repo.PasswordHistoryRepository;
import com.allst.springboot.domain.repo.SecUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 改密、重置和历史口令。比较时用 PasswordEncoder.matches，不能把摘要当明文比。
 */
@Service
public class PasswordService {

    private final SecUserRepository users;
    private final PasswordHistoryRepository histories;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public PasswordService(SecUserRepository users, PasswordHistoryRepository histories,
                           PasswordEncoder passwordEncoder, SecurityProperties properties) {
        this.users = users;
        this.histories = histories;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public boolean isExpired(SecUser user) {
        int maxAgeDays = properties.getPassword().getMaxAgeDays();
        if (maxAgeDays <= 0 || user.getPasswordChangedAt() == null) {
            return false;
        }
        return user.getPasswordChangedAt().plusDays(maxAgeDays).isBefore(LocalDateTime.now());
    }

    @Transactional
    public void changeOwn(Long userId, String oldPassword, String newPassword) {
        SecUser user = required(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.BAD_CREDENTIALS, 401, "原密码不正确");
        }
        replace(user, newPassword);
    }

    @Transactional
    public void reset(Long userId, String newPassword) {
        replace(required(userId), newPassword);
    }

    @Transactional
    public void changeWithOldPassword(String username, String oldPassword, String newPassword) {
        SecUser user = users.findByUsername(username)
                .orElseThrow(() -> new BizException(ResultCode.BAD_CREDENTIALS, 401, "用户名或密码错误"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.BAD_CREDENTIALS, 401, "用户名或密码错误");
        }
        replace(user, newPassword);
    }

    private void replace(SecUser user, String newPassword) {
        PasswordPolicy.check(user.getUsername(), newPassword, properties.getPassword().getDenylist());
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_POLICY, 400, "新密码不能与当前密码相同");
        }
        List<PasswordHistory> recent = histories.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (PasswordHistory history : recent) {
            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                throw new BizException(ResultCode.PASSWORD_POLICY, 400, "新密码不能与最近使用过的密码相同");
            }
        }
        PasswordHistory history = new PasswordHistory();
        history.setUserId(user.getId());
        history.setPasswordHash(user.getPassword());
        history.setCreatedAt(LocalDateTime.now());
        histories.save(history);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockUntil(null);
        users.save(user);
        trim(user.getId());
    }

    private void trim(Long userId) {
        int keep = Math.max(properties.getPassword().getHistorySize(), 0);
        List<PasswordHistory> recent = histories.findByUserIdOrderByCreatedAtDesc(userId);
        if (recent.size() <= keep) {
            return;
        }
        histories.deleteAll(recent.subList(keep, recent.size()));
    }

    private SecUser required(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.VALIDATION, 400, "用户不存在"));
    }
}
