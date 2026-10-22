package com.allst.springboot.domain.order;

import com.allst.springboot.common.api.PageData;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.guard.SizeGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单查询。不负责创建和取消。
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final SizeGuard sizeGuard;

    public OrderQueryService(OrderRepository orderRepository, SizeGuard sizeGuard) {
        this.orderRepository = orderRepository;
        this.sizeGuard = sizeGuard;
    }

    public PageData<OrderVO> page(OrderPageQuery query) {
        int page = sizeGuard.pageIndex(query.getPage());
        int size = sizeGuard.pageSize(query.getSize());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<OrderEntity> result = query.getUserId() == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByUserId(query.getUserId(), pageable);
        List<OrderVO> records = result.getContent().stream().map(OrderAssembler::toVO).collect(Collectors.toList());
        return new PageData<>(result.getTotalElements(), page, size, records);
    }

    public OrderVO get(Long id) {
        return OrderAssembler.toVO(orderRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "订单不存在")));
    }
}
