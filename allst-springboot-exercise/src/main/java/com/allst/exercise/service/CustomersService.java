package com.allst.exercise.service;

import com.allst.exercise.model.Customers;
import com.mybatisflex.core.paginate.Page;

/**
 * @author Hutu
 * @since 2024-07-28 上午 12:00
 */
public interface CustomersService {
    String save(Customers customers);

    String queryOne(Long id);

    Customers selectById(Long id);

    Customers selectByName(String name);

    Page<Customers> selectPageList(Integer pageSize, Integer pageNumber);
}
