package com.allst.springboot.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 仓储：把「怎么查」收在接口后面。外观、迭代器、备忘录都只依赖它，不写 SQL。
 */
public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {

    Optional<OrderRecord> findByOrderNo(String orderNo);

    Optional<OrderRecord> findByRequestId(String requestId);
}
