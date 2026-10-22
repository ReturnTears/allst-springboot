package com.allst.springboot.redis.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 业务读写统一走 {@link StringRedisTemplate}，键和值都是字符串，redis-cli 里可以直接看懂。
 * 关闭 Lettuce 共享连接后，阻塞命令不会卡住同一条连接上的其它请求。当前消费者用非阻塞轮询，这里仍关掉共享，避免以后加 BRPOP 时踩坑。
 */
@Configuration
public class RedisConfig {

    @Bean
    public static BeanPostProcessor lettuceShareConnectionCustomizer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof LettuceConnectionFactory) {
                    ((LettuceConnectionFactory) bean).setShareNativeConnection(false);
                }
                return bean;
            }
        };
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        return template;
    }
}
