package com.allst.exercise.service;

import com.allst.exercise.mapper.CustomersMapper;
import com.allst.exercise.model.Customers;
import com.google.gson.Gson;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static com.allst.exercise.model.table.AccountTableDef.ACCOUNT;

/**
 * @author Hutu
 * @since 2024-07-28 上午 12:01
 */
@Service
@RequiredArgsConstructor
public class CustomersServiceImpl implements CustomersService {

    private final TransactionTemplate transactionTemplate;

    private final CustomersMapper customersMapper;

    @Override
    public String save(Customers customers) {
        transactionTemplate.execute((TransactionCallback<Void>) status -> {
            int insert = customersMapper.insert(customers);
            System.out.println("Success num = " + insert);
            return null;
        });
        return new Gson().toJson(customers);
    }

    @Override
    public String queryOne(Integer id) {
        Customers customers = new Customers();
        customers.setId(id);
        customers = customersMapper.selectOneByEntityId(customers);
        return new Gson().toJson(customers);
    }
}
