package com.allst.springboot.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Page<UserEntity> findByUsernameContaining(String username, Pageable pageable);

    List<UserEntity> findByUsernameIn(Collection<String> usernames);

    List<UserEntity> findByIdIn(Collection<Long> ids);
}
