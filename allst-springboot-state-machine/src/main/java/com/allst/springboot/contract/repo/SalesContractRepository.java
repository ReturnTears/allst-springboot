package com.allst.springboot.contract.repo;

import com.allst.springboot.contract.domain.SalesContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesContractRepository extends JpaRepository<SalesContract, Long> {

    Optional<SalesContract> findByContractNo(String contractNo);

    boolean existsByContractNo(String contractNo);
}
