package com.allst.springboot.domain.repo;

import com.allst.springboot.domain.entity.SecRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecRoleRepository extends JpaRepository<SecRole, Long> {

    Optional<SecRole> findByCode(String code);
}
