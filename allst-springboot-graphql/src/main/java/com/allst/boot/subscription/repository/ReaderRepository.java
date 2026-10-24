package com.allst.boot.subscription.repository;

import com.allst.boot.subscription.model.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReaderRepository extends JpaRepository<Reader, Integer>, JpaSpecificationExecutor<Reader> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Integer id);

    boolean existsByEmailAndIdNot(String email, Integer id);
}
