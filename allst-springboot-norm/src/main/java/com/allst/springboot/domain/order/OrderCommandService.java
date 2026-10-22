package com.allst.springboot.domain.order;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.guard.SizeGuard;
import com.allst.springboot.common.header.Operators;
import com.allst.springboot.common.tx.AfterCommit;
import com.allst.springboot.domain.notify.NotifyService;
import com.allst.springboot.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单写入。创建和取消分开；批量创建在一个事务里，任一用户不存在则整批回滚。
 */
@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SizeGuard sizeGuard;
    private final NotifyService notifyService;

    public OrderCommandService(OrderRepository orderRepository, UserRepository userRepository,
                               SizeGuard sizeGuard, NotifyService notifyService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.sizeGuard = sizeGuard;
        this.notifyService = notifyService;
    }

    @Transactional
    public OrderVO create(OrderCreateCommand command, String operator) {
        requireUsers(Set.of(command.getUserId()));
        OrderEntity saved = orderRepository.save(build(command, operator));
        AfterCommit.run(() -> notifyService.onOrderCreated(saved.getId(), saved.getOrderNo()));
        return OrderAssembler.toVO(saved);
    }

    @Transactional
    public List<Long> batchCreate(OrderBatchCreateCommand command, String operator) {
        sizeGuard.batch(command.getOrders().size());
        Set<Long> userIds = command.getOrders().stream().map(OrderCreateCommand::getUserId).collect(Collectors.toSet());
        requireUsers(userIds);
        List<OrderEntity> entities = new ArrayList<>();
        for (OrderCreateCommand item : command.getOrders()) {
            entities.add(build(item, operator));
        }
        List<OrderEntity> saved = orderRepository.saveAll(entities);
        AfterCommit.run(() -> saved.forEach(order -> notifyService.onOrderCreated(order.getId(), order.getOrderNo())));
        return saved.stream().map(OrderEntity::getId).collect(Collectors.toList());
    }

    /**
     * 只负责取消。重复取消返回当前订单，不报错，方便调用方重试。
     */
    @Transactional
    public OrderVO cancel(Long id) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "订单不存在"));
        if (entity.getStatus() != OrderStatus.CANCELED) {
            entity.setStatus(OrderStatus.CANCELED);
            entity = orderRepository.save(entity);
        }
        return OrderAssembler.toVO(entity);
    }

    private void requireUsers(Set<Long> userIds) {
        long found = userRepository.findByIdIn(userIds).size();
        if (found != userIds.size()) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在，订单未创建");
        }
    }

    private OrderEntity build(OrderCreateCommand command, String operator) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(newOrderNo());
        entity.setUserId(command.getUserId());
        entity.setAmount(command.getAmount());
        entity.setRemark(command.getRemark());
        entity.setStatus(OrderStatus.CREATED);
        entity.setCreatedBy(Operators.normalize(operator));
        return entity;
    }

    private String newOrderNo() {
        return "O" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
