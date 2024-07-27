package com.allst.exercise.service;

import com.allst.exercise.mapper.CustomersMapper;
import com.allst.exercise.model.Customers;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Hutu
 * @since 2024-07-28 上午 12:01
 */
@Service
@RequiredArgsConstructor
public class CustomersServiceImpl implements CustomersService {

    private final CustomersMapper customersMapper;

    @Override
    public String save(Customers customers) {
        int insert = customersMapper.insert(customers);
        System.out.println("Success num = " + insert);
        return new Gson().toJson(customers);
    }
}
