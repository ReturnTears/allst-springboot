package com.allst.springboot.tradeorder.repo;

import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.domain.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByOrderNo(String orderNo);

    boolean existsByOrderNo(String orderNo);

    List<TradeOrder> findByStatusInAndUpdatedAtBefore(Collection<OrderState> statuses, LocalDateTime deadline);
}
