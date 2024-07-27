package com.allst.exercise.service;

import com.allst.exercise.mapper.AccountMapper;
import com.allst.exercise.model.Account;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.allst.exercise.model.table.AccountTableDef.ACCOUNT;

/**
 * @author Hutu
 * @since 2024-07-27 下午 10:23
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;

    @Override
    public Account getAccountOne(Long id) {
        QueryWrapper queryWrapper = QueryWrapper.create().select().where(ACCOUNT.ID.eq(id));
        Account account = accountMapper.selectOneByQuery(queryWrapper);
        if (ObjectUtil.areNull(account)) {
            return new Account();
        }
        System.out.println(account);
        return account;
    }
}
