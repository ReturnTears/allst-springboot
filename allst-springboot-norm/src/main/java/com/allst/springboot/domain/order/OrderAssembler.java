package com.allst.springboot.domain.order;

public final class OrderAssembler {

    private OrderAssembler() {
    }

    public static OrderVO toVO(OrderEntity entity) {
        OrderVO vo = new OrderVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setUserId(entity.getUserId());
        vo.setAmount(entity.getAmount());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
