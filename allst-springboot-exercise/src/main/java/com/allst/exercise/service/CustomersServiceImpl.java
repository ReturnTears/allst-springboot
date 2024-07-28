package com.allst.exercise.service;

import com.allst.exercise.mapper.CustomersMapper;
import com.allst.exercise.model.Customers;
import com.allst.exercise.model.table.CustomersTableDef;
import com.google.gson.Gson;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
        transactionTemplate.execute((TransactionCallback<Customers>) status -> {
            try {
                int insert = customersMapper.insert(customers);
                System.out.println("Success num = " + insert);
            } catch (Exception e) {
                status.setRollbackOnly() ;
            }
            return null ;
        });
        return new Gson().toJson(customers);
    }

    @Override
    public String queryOne(Long id) {
        QueryWrapper queryWrapper = QueryWrapper.create().select().where(CustomersTableDef.CUSTOMERS.ID.eq(id));
        Customers customers = customersMapper.selectOneByQuery(queryWrapper);
        return new Gson().toJson(customers);
    }

    @Override
    public Customers selectById(Long id) {
        return customersMapper.selectById(id);
    }
}
