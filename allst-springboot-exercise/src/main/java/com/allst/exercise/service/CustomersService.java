package com.allst.exercise.service;

import com.allst.exercise.model.Customers;

/**
 * @author Hutu
 * @since 2024-07-28 上午 12:00
 */
public interface CustomersService {
    String save(Customers customers);

    String queryOne(Long id);
}
