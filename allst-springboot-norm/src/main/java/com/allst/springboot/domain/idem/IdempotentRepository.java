package com.allst.springboot.domain.idem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotentRepository extends JpaRepository<IdempotentRecord, Long> {

    Optional<IdempotentRecord> findByIdemKey(String idemKey);
}
