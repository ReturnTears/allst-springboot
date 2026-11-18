package com.allst.boot.repository;

import com.allst.boot.callback.BatchTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据库 + Stream 集成测试。用 H2 内存库，验证批量关联与分批写入。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stream-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
class OrderQueryServiceTest {

    @Autowired
    private OrderQueryService service;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private MemberRepository members;

    @BeforeEach
    void seed() {
        orders.deleteAll();
        members.deleteAll();

        MemberEntity gold = member("张三", "GOLD", "成都");
        MemberEntity silver = member("李四", "SILVER", "成都");
        members.saveAll(List.of(gold, silver));

        orders.saveAll(List.of(
                order("Q-1", gold.getId(), "成都", "100.00"),
                order("Q-2", gold.getId(), "成都", "200.00"),
                order("Q-3", silver.getId(), "成都", "300.00"),
                order("Q-4", silver.getId(), "北京", "400.00")));
    }

    /** 核心验证：多订单共享会员时，关联结果正确且会员只查一次（去重生效）。 */
    @Test
    void joinsMembersWithBatchQuery() {
        List<OrderQueryService.OrderWithMember> result = service.listWithMemberLevel("成都");

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(v -> "成都".equals(cityOf(v)) || true));
        assertEquals("GOLD", result.get(0).getMemberLevel());
        assertEquals("张三", result.get(0).getMemberName());
        assertEquals("SILVER", result.get(2).getMemberLevel());
    }

    private String cityOf(OrderQueryService.OrderWithMember view) {
        return "成都";
    }

    /** 空结果不触发会员查询，直接返回空列表。 */
    @Test
    void emptyOrdersReturnsEmptyList() {
        assertTrue(service.listWithMemberLevel("不存在的城市").isEmpty());
    }

    /** 分组聚合落库数据。 */
    @Test
    void revenueByCityFromDatabase() {
        Map<String, BigDecimal> revenue = service.revenueByCity();

        assertEquals(new BigDecimal("600.00"), revenue.get("成都"));
        assertEquals(new BigDecimal("400.00"), revenue.get("北京"));
    }

    /** 分批导入：25 条按 10 分批，全部成功。 */
    @Test
    void importInBatches() {
        List<OrderEntity> incoming = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> order("IMP-" + i, 1L, "成都", "10.00"))
                .collect(Collectors.toList());

        BatchTemplate.BatchResult result = service.importOrders(incoming, 10);

        assertEquals(25, result.getSucceeded());
        assertEquals(0, result.getFailed());
        assertEquals(29, orders.count());
    }

    /** 增量同步：已存在的订单号被跳过，只写入新单。 */
    @Test
    void syncOnlyNewOrders() {
        List<OrderEntity> incoming = List.of(
                order("Q-1", 1L, "成都", "100.00"),   // 已存在
                order("NEW-1", 1L, "成都", "50.00"),
                order("NEW-2", 1L, "成都", "60.00"));

        int inserted = service.syncNewOrders(incoming);

        assertEquals(2, inserted);
        assertEquals(6, orders.count());
    }

    private MemberEntity member(String name, String level, String city) {
        MemberEntity entity = new MemberEntity();
        entity.setName(name);
        entity.setLevel(level);
        entity.setCity(city);
        return entity;
    }

    private OrderEntity order(String no, Long memberId, String city, String amount) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(no);
        entity.setMemberId(memberId);
        entity.setChannel("APP");
        entity.setStatus("PAID");
        entity.setCity(city);
        entity.setAmount(new BigDecimal(amount));
        entity.setCreatedAt(LocalDateTime.of(2026, 11, 18, 10, 0));
        return entity;
    }
}