package com.allst.springboot.domain.repo;

import com.allst.springboot.domain.entity.SecPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecPermissionRepository extends JpaRepository<SecPermission, Long> {

    Optional<SecPermission> findByCode(String code);
}
