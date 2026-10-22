package com.allst.springboot.domain.repo;

import com.allst.springboot.domain.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
