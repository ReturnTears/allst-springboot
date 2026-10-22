package com.allst.springboot.job;

import com.allst.springboot.domain.repo.RefreshTokenRepository;
import com.allst.springboot.domain.repo.TokenBlacklistRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 清掉已经过期的刷新令牌和注销名单，避免两张表只增不减。
 */
@Component
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokens;
    private final TokenBlacklistRepository blacklist;

    public TokenCleanupJob(RefreshTokenRepository refreshTokens, TokenBlacklistRepository blacklist) {
        this.refreshTokens = refreshTokens;
        this.blacklist = blacklist;
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void clean() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokens.deleteExpired(now);
        blacklist.deleteExpired(now);
    }
}
