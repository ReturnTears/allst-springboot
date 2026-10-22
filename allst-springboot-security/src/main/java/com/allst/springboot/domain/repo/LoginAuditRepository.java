package com.allst.springboot.domain.repo;

import com.allst.springboot.domain.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    List<LoginAudit> findTop50ByOrderByCreatedAtDesc();
}
