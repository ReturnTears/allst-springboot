package com.allst.exercise.service;

import com.allst.exercise.mapper.CustomersMapper;
import com.allst.exercise.model.Customers;
import com.allst.exercise.model.table.CustomersTableDef;
import com.google.gson.Gson;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

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

    @Override
    public Customers selectByName(String name) {
        Customers customers = customersMapper.selectByName(name);
        System.out.println("customers : " +customers);
        return customers;
    }

    @Override
    public Page<Customers> selectPageList(Integer pageSize, Integer pageNumber) {
        QueryWrapper qw = QueryWrapper.create().where(Customers::getCountry).eq("china").and(Customers::getId).ge(0);
        return customersMapper.xmlPaginate("selectPageList", Page.of(pageNumber, pageSize), qw);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customers> selectCursorList() {
        QueryWrapper qw = QueryWrapper.create().where(Customers::getId).ge(0);
        List<Customers> result = List.of();
        Db.tx(() -> {
            Cursor<Customers> customers = customersMapper.selectCursorByQuery(qw);
            int currentIndex = customers.getCurrentIndex();
            System.out.println("currentIndex : " + currentIndex);
            if (customers.isOpen()) {
                for (Customers customer : customers) {
                    System.out.println(customer);
                    result.add(customer);
                }
            }
            return true;
        });
        return result;
    }
}
