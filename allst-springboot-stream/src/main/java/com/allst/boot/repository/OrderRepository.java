package com.allst.boot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCity(String city);

    List<OrderEntity> findByStatus(String status);
}