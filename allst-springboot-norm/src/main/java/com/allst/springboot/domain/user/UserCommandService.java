package com.allst.springboot.domain.user;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.guard.SizeGuard;
import com.allst.springboot.common.header.Operators;
import com.allst.springboot.common.tx.AfterCommit;
import com.allst.springboot.domain.notify.NotifyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户写入。创建、改资料、改状态、删除彼此独立，批量操作整批成功或整批回滚。
 * 对应规范：17 批量操作，18 职责单一。
 */
@Service
public class UserCommandService {

    private final UserRepository userRepository;
    private final SizeGuard sizeGuard;
    private final NotifyService notifyService;

    public UserCommandService(UserRepository userRepository, SizeGuard sizeGuard, NotifyService notifyService) {
        this.userRepository = userRepository;
        this.sizeGuard = sizeGuard;
        this.notifyService = notifyService;
    }

    @Transactional
    public UserVO create(UserCreateCommand command, String operator) {
        ensureUsernameAvailable(command.getUsername());
        UserEntity saved = userRepository.save(build(command, operator));
        AfterCommit.run(() -> notifyService.onUserCreated(saved.getId(), saved.getUsername()));
        return UserAssembler.toVO(saved);
    }

    @Transactional
    public UserVO update(Long id, UserUpdateCommand command) {
        UserEntity entity = require(id);
        entity.setRealName(command.getRealName().trim());
        entity.setPhone(command.getPhone());
        entity.setEmail(command.getEmail());
        entity.setIdCard(command.getIdCard());
        return UserAssembler.toVO(userRepository.save(entity));
    }

    @Transactional
    public UserVO changeStatus(Long id, UserStatusCommand command) {
        UserEntity entity = require(id);
        entity.setStatus(command.getStatus());
        return UserAssembler.toVO(userRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(require(id));
    }

    @Transactional
    public List<Long> batchCreate(UserBatchCreateCommand command, String operator) {
        List<UserCreateCommand> users = command.getUsers();
        sizeGuard.batch(users.size());
        long distinct = users.stream().map(UserCreateCommand::getUsername).distinct().count();
        if (distinct != users.size()) {
            throw new BizException(ResultCode.VALIDATION_FAILED, "批量数据中存在重复用户名");
        }
        List<String> names = users.stream().map(UserCreateCommand::getUsername).collect(Collectors.toList());
        List<UserEntity> existed = userRepository.findByUsernameIn(names);
        if (!existed.isEmpty()) {
            throw new BizException(ResultCode.USERNAME_EXISTS, "用户名已存在: " + existed.get(0).getUsername());
        }
        List<UserEntity> entities = new ArrayList<>();
        for (UserCreateCommand item : users) {
            entities.add(build(item, operator));
        }
        List<UserEntity> saved = userRepository.saveAll(entities);
        AfterCommit.run(() -> saved.forEach(user -> notifyService.onUserCreated(user.getId(), user.getUsername())));
        return saved.stream().map(UserEntity::getId).collect(Collectors.toList());
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        sizeGuard.batch(ids.size());
        List<Long> distinct = ids.stream().distinct().collect(Collectors.toList());
        List<UserEntity> found = userRepository.findByIdIn(distinct);
        if (found.size() != distinct.size()) {
            throw new BizException(ResultCode.NOT_FOUND, "存在不存在的用户，批量删除已取消");
        }
        userRepository.deleteAll(found);
    }

    private UserEntity build(UserCreateCommand command, String operator) {
        UserEntity entity = new UserEntity();
        entity.setUsername(command.getUsername());
        entity.setRealName(command.getRealName().trim());
        entity.setPhone(command.getPhone());
        entity.setEmail(command.getEmail());
        entity.setIdCard(command.getIdCard());
        entity.setStatus(1);
        entity.setCreatedBy(Operators.normalize(operator));
        return entity;
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
    }

    private UserEntity require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "用户不存在"));
    }
}
