package com.allst.springboot.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 只承担两件事：可重入分布式锁（看门狗续期）和布隆过滤器。
 * 普通键值仍然用 Spring Data Redis，避免两套客户端写出不同编码的值。
 * StringCodec 让布隆过滤器的成员以字符串保存。lazyInitialization 让 Redis 没启动时应用仍能起来，真正用到锁或布隆时才连接。
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.password:}") String password,
            @Value("${spring.redis.database:0}") int database) {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.setLazyInitialization(true);
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setTimeout(3000)
                .setConnectTimeout(3000)
                .setRetryAttempts(1)
                .setRetryInterval(1000)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(8);
        if (StringUtils.hasText(password)) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }
}
