package com.allst.springboot.domain.repo;

import com.allst.springboot.domain.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {

    @Modifying
    @Query("delete from TokenBlacklist t where t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
