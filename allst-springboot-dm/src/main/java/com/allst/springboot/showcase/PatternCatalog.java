package com.allst.springboot.showcase;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 机器可读的「场景 → 模式 → 解决什么问题 → 对应类」速查表。
 * /api/dm/patterns 直接返回它，和 docs/02-scenario-map.md 是同一份内容。
 */
public final class PatternCatalog {

    private PatternCatalog() {
    }

    @Getter
    @AllArgsConstructor
    public static class PatternInfo {
        private final String category;
        private final String pattern;
        private final String scenario;
        private final String problem;
        private final String solved;
        private final String classes;
        private final String reference;
    }

    private static PatternInfo p(String category, String pattern, String scenario,
                                 String problem, String solved, String classes, String reference) {
        return new PatternInfo(category, pattern, scenario, problem, solved, classes, reference);
    }

    public static List<PatternInfo> all() {
        return List.of(
                p("创建型", "单例 Singleton", "促销/风控规则字典、配置缓存",
                        "每个请求都查一次库；手写 static getInstance 绕过 IoC",
                        "容器单例 + 并发容器 + 整体替换，读取无需加锁",
                        "creational.singleton.RuleRegistry", "alibaba/nacos, alibaba/Sentinel"),
                p("创建型", "工厂方法 Factory Method", "支付宝/微信/余额支付命令创建",
                        "支付流程里到处 switch 渠道，签名和流水号规则混在一起",
                        "创建哪种命令下放给 Creator，主流程只写一份",
                        "creational.factorymethod.PayChannelFactory", "apache/dubbo SPI, macrozheng/mall"),
                p("创建型", "抽象工厂 Abstract Factory", "国内站 / 跨境站成套组件",
                        "订单号、结算、合规说明各自 if，容易拼出跨渠道的非法组合",
                        "一次给一族组件，保证配套使用",
                        "creational.abstractfactory.ChannelKitFactory", "apache/shardingsphere"),
                p("创建型", "建造者 Builder", "运营后台配置复杂促销规则",
                        "十几个可选字段导致构造器不可读，setter 产生半成品对象",
                        "校验集中在 build()，产出不可变对象",
                        "creational.builder.PromotionRule", "mybatis-3 SQL/Example Builder"),
                p("创建型", "原型 Prototype", "大促活动模板复制、请求内购物车草稿",
                        "重新装配慢且易漏；浅拷贝会串改白名单；prototype 注入单例失效",
                        "深拷贝产出独立副本；作用域对象用 ObjectProvider 现取",
                        "creational.prototype.PromotionTemplate, CartDraftProvider", "通用业务场景"),
                p("结构型", "适配器 Adapter", "对接顺丰/京东等第三方 SDK",
                        "方法名、参数顺序、返回类型各异，源码不可改",
                        "统一接口，第三方类型不外泄，新增渠道只加一个 bean",
                        "structural.adapter.LogisticsClient, LogisticsAdapters", "Spring MVC HandlerAdapter, macrozheng/mall"),
                p("结构型", "桥接 Bridge", "订单/告警消息 × 短信/邮件/推送",
                        "两个维度用继承组合会 N×M 类爆炸",
                        "抽象与实现分离，各自独立扩展、运行时组合",
                        "structural.bridge.NotificationBridge", "JDBC-SLF4J 绑定（通用推断）"),
                p("结构型", "组合 Composite", "优惠券规则树、类目树、组织树",
                        "每层括号都要改 if 嵌套，叶子和组合不能统一处理",
                        "叶子与组合实现同一接口，调用方只 evaluate 一次",
                        "structural.composite.PromotionRuleTree", "Sentinel/规则树（合理推断）"),
                p("结构型", "装饰器 Decorator", "报价服务叠加缓存、耗时统计",
                        "加横切能力就改原类，一个类承担定价+缓存+埋点",
                        "按层套装饰，顺序可调，原实现不动",
                        "structural.decorator.PriceQuoteService", "mybatis-3 CachingExecutor, Spring @Cacheable"),
                p("结构型", "外观 Facade", "一次下单编排二十多个步骤",
                        "Controller 里堆满服务调用、事务和顺序细节",
                        "一个入口挡住复杂度，统一事务边界",
                        "showcase.PlaceOrderFacade", "macrozheng/mall OmsPortalOrderService"),
                p("结构型", "享元 Flyweight", "SKU 规格/字典项被大量订单行重复引用",
                        "每行都 new 一份不变元数据，或每行查一次库",
                        "按 key 共享不可变内部状态，外部状态由调用方传入",
                        "structural.flyweight.SkuSpecRegistry", "通用业务场景"),
                p("结构型", "代理 Proxy", "库存调用审计、事务、远程调用",
                        "改不了目标类又要加前后置逻辑；自调用让注解失效",
                        "JDK/CGLIB 代理拦截，横切逻辑集中",
                        "structural.proxy.JdkAuditProxy, ServiceTimingAspect", "Spring @Transactional, mybatis-3 Mapper"),
                p("行为型", "责任链 Chain of Responsibility", "下单前置校验串",
                        "几十行 if 嵌套，失败原因和顺序散落",
                        "每关只回答放过/拒绝，失败短路并留下位置",
                        "behavioral.chain.OrderValidationChain", "alibaba/Sentinel slot chain, Servlet Filter"),
                p("行为型", "命令 Command", "支付回调/扣库存/发券的重试与撤销",
                        "多入口重复、失败无处重试、回滚靠手写补偿",
                        "动作带 id 与 undo，统一幂等/重试/撤销栈",
                        "behavioral.command.OrderCommand, CommandInvoker", "apache/rocketmq, seata undo log（推断）"),
                p("行为型", "解释器 Interpreter", "运营配置促销门槛表达式",
                        "改一条规则就发版；自己写解析器维护成本高",
                        "语法树可组合求值；生产直接用 SpEL/规则引擎",
                        "behavioral.interpreter.PromotionExpression", "Spring SpEL, alibaba/COLA, Drools"),
                p("行为型", "迭代器 Iterator", "批量对账/导出遍历海量订单",
                        "一次 findAll 打爆内存，每个任务重复写分页样板",
                        "Iterator 背后按批拉取，调用方只写 while",
                        "behavioral.iterator.PagedOrderCursor", "mybatis-3 Cursor, Spring Batch ItemReader"),
                p("行为型", "中介者 Mediator", "支付成功后库存/积分/通知/审计协作",
                        "多个服务互相直接调用，织成网，顺序与容错散落",
                        "中介者集中编排，同事之间互不引用",
                        "behavioral.mediator.OrderMediator", "通用业务场景"),
                p("行为型", "备忘录 Memento", "客服改单后撤销、留痕",
                        "暴露内部字段给外部保存，快照会跟着对象变",
                        "原发器自己产生/消费不可变快照，负责人只保管",
                        "behavioral.memento.CartSnapshot, SnapshotCareTaker", "seata undo log（推断）"),
                p("行为型", "观察者 Observer", "支付成功后发积分/短信/审计/仓库通知",
                        "每加一个后续动作就改主流程，回滚了消息却已发出",
                        "发布者只发事件；AFTER_COMMIT 监听器保证提交后再通知",
                        "behavioral.observer.OrderPlacedEvent, OrderEventListeners", "Spring ApplicationEvent, macrozheng/mall"),
                p("行为型", "状态 State", "订单状态与合法事件",
                        "状态判断散落在每个方法开头，新增状态要翻遍代码",
                        "转移表集中定义，非法事件到不了业务动作",
                        "behavioral.state.OrderStateMachine", "alibaba/COLA, newbee-mall 订单状态"),
                p("行为型", "策略 Strategy", "满减/会员价/新人券多种算价",
                        "算价方法里堆 if-else，上新活动就改主流程",
                        "算法各自成类，按 type 路由，新增只加一个实现",
                        "behavioral.strategy.DiscountStrategyRouter", "apache/shardingsphere 分片策略, macrozheng/mall 促销"),
                p("行为型", "模板方法 Template Method", "国内/跨境回执、批量导入、对账骨架",
                        "同一套步骤被复制多份，改一处校验漏一处",
                        "final 固定骨架顺序，子类只填差异步骤和钩子",
                        "behavioral.template.ReceiptTemplate, ReceiptService", "Spring JdbcTemplate, xxl-job IJobHandler（推断）"),
                p("行为型", "访问者 Visitor", "实物/虚拟/礼品卡行的多套计算口径",
                        "每个实体堆计算字段，或外层满屏 instanceof",
                        "双分派让新增一套计算只加一个 Visitor",
                        "behavioral.visitor.OrderLineVisitorDemo", "通用业务场景（ASM/ANTLR 同类结构）"),
                p("Spring 机制", "依赖注入 / IoC", "策略、校验器、适配器统一装配",
                        "手 new 实现导致替换不了、测试要起全容器",
                        "容器按类型/名字注入集合，扩展点变成配置",
                        "LogisticsRouter, DiscountStrategyRouter", "spring-projects/spring-framework"),
                p("Spring 机制", "AOP 代理", "事务、日志、限流、耗时",
                        "横切逻辑混进业务方法；同类自调用注解不生效",
                        "代理统一拦截，业务类保持干净",
                        "structural.proxy.ServiceTimingAspect", "spring-projects/spring-framework"),
                p("Spring 机制", "模板回调 Callback", "JdbcTemplate / RestTemplate",
                        "资源获取、异常处理、释放被每个 DAO 抄一遍",
                        "模板管流程，回调只写变化的那一行",
                        "（见 06-spring-patterns.md）", "Spring JdbcTemplate")
        );
    }
}
