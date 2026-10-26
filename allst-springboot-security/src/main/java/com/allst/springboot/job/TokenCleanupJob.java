package com.allst.springboot.job;

import com.allst.springboot.domain.repo.RefreshTokenRepository;
import com.allst.springboot.domain.repo.TokenBlacklistRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 每小时删掉已经过期的刷新令牌和注销名单。过期记录留着也验不过，只是占空间。
 * 没过期的注销记录不能删：访问令牌在过期前仍要靠这张表判断「已经退出」。
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
