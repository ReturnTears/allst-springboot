package com.allst.springboot.structural.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理（Proxy）——在不改目标类的前提下，给库存服务加审计和异常记录。
 *
 * 场景：外部库存服务接口已经定好，调用方要在每次扣减前后留痕、统一记录失败原因。
 * JDK 动态代理要求目标有接口；没有接口时 Spring AOP 会用 CGLIB 生成子类代理。
 *
 * Spring 里的 @Transactional、@Async、@Cacheable、@FeignClient、MyBatis 的 Mapper
 * 都是代理。共同的坑是「自调用」：同一个类内部 this.method() 不走代理，注解不生效。
 */
public final class JdkAuditProxy {

    private JdkAuditProxy() {
    }

    public interface StockService {
        int available(String sku);

        void deduct(String sku, int quantity);
    }

    public static final class DefaultStockService implements StockService {
        private final Map<String, Integer> stock = new ConcurrentHashMap<>();

        public DefaultStockService() {
            stock.put("SKU-PHONE", 100);
            stock.put("SKU-GIFT", 5);
        }

        @Override
        public int available(String sku) {
            return stock.getOrDefault(sku, 0);
        }

        @Override
        public void deduct(String sku, int quantity) {
            int left = available(sku);
            if (left < quantity) {
                throw new IllegalStateException("库存不足: " + sku);
            }
            stock.put(sku, left - quantity);
        }
    }

    public static StockService wrap(StockService target, List<String> auditLog) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                auditLog.add("BEFORE " + method.getName());
                try {
                    Object result = method.invoke(target, args);
                    auditLog.add("AFTER " + method.getName());
                    return result;
                } catch (java.lang.reflect.InvocationTargetException e) {
                    auditLog.add("FAILED " + method.getName() + " -> " + e.getCause().getMessage());
                    throw e.getCause();
                }
            }
        };
        return (StockService) Proxy.newProxyInstance(
                JdkAuditProxy.class.getClassLoader(),
                new Class<?>[]{StockService.class},
                handler);
    }
}
