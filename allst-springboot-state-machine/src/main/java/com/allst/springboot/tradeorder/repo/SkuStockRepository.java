package com.allst.springboot.tradeorder.repo;

import com.allst.springboot.tradeorder.domain.SkuStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SkuStockRepository extends JpaRepository<SkuStock, Long> {

    Optional<SkuStock> findBySkuCode(String skuCode);

    boolean existsBySkuCode(String skuCode);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("update SkuStock s set s.available = s.available - :qty "
            + "where s.skuCode = :sku and s.available >= :qty")
    int deductIfEnough(@Param("sku") String sku, @Param("qty") int qty);
}
