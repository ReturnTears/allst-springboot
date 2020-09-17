package com.allst.springboot.config;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.injector.methods.additional.LogicDeleteByIdWithFill;
import com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author YiYa
 * @since 2020-09-17 下午 09:43
 */

@EnableTransactionManagement    // 开启事务管理(默认是自动开启的， 不添加该注解也可以)
@MapperScan("com.allst.springboot") // 扫描mapper文件
@Configuration  // 配置类
public class MybatisPlusConfig {

    /**
     * 注册乐观锁插件，新版本
     * 乐观锁插件: OptimisticLockerInnerInterceptor
     * 说明:
     * 支持的数据类型只有:int,Integer,long,Long,Date,Timestamp,LocalDateTime
     * 整数类型下 newVersion = oldVersion + 1
     * newVersion 会回写到 entity 中
     * 仅支持 updateById(id) 与 update(entity, wrapper) 方法
     * 在 update(entity, wrapper) 方法下, wrapper 不能复用!!!
     */
    /*@Bean
    public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }*/

    /**
     * 乐观锁插件
     * old版本
     */
    @Bean
    public OptimisticLockerInterceptor optimisticLockerInterceptor() {
        return new OptimisticLockerInterceptor();
    }

    /**
     * 分页插件
     * old版本
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

    /**
     * 逻辑删除插件
     * 删除的时候走的不是delete语句， 而是update语句(UPDATE user SET deleted=1 WHERE id=? AND deleted=0)
     * 查询的时候自动过滤被逻辑删除的字段
     */
    @Bean
    public LogicDeleteByIdWithFill sqlInjector() {
        return new LogicDeleteByIdWithFill();
    }

//    @Bean
//    public ISqlInjector iSqlInjector() {
//        return new LogicSqlInector();
//    }

    /**
     * SQL执行效率插件
     */
    @Bean
    @Profile({"dev", "test"})
    public PerformanceMonitorInterceptor performanceMonitorInterceptor() {
        PerformanceMonitorInterceptor performanceMonitorInterceptor = new PerformanceMonitorInterceptor();
        performanceMonitorInterceptor.setLogTargetClassInvocation(true);
        performanceMonitorInterceptor.setLoggerName("User-");
        return performanceMonitorInterceptor;
    }
}
