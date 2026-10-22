package com.allst.springboot.domain.repo;

import com.allst.springboot.domain.entity.SecUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecUserRepository extends JpaRepository<SecUser, Long> {

    Optional<SecUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
