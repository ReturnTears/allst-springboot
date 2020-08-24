package com.allst.springboot.config;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.allst.springboot.bean.Department;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.*;

/**
 * 自定义Redis配置类
 *
 * @author YiYa
 * @since 2020/8/6-10:20
 */
@ConditionalOnClass(RedisOperations.class)
@EnableConfigurationProperties(RedisProperties.class)
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Department> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Department> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // 序列化配置
        // 使用fastjson序列化
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
        // value值的序列化采用fastJsonRedisSerializer
        template.setValueSerializer(fastJsonRedisSerializer);
        template.setHashValueSerializer(fastJsonRedisSerializer);
        // key的序列化采用StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisTemplate<String, Department> redisTemplate) {
        FastJsonRedisSerializer<Department> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Department.class);
        //spring cache注解序列化配置
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())) //key序列化方式
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(fastJsonRedisSerializer)) //value序列化方式
                .disableCachingNullValues() //不缓存null值
                .entryTtl(Duration.ofSeconds(600)); //默认缓存过期时间

        // 设置一个初始化的缓存名称set集合
        Set<String> cacheNames = new HashSet<>();
        cacheNames.add("emp");

        // 对每个缓存名称应用不同的配置，自定义过期时间
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put("emp", redisCacheConfiguration.entryTtl(Duration.ofSeconds(1200)));

        return RedisCacheManager.builder(Objects.requireNonNull(redisTemplate.getConnectionFactory()))
                .cacheDefaults(redisCacheConfiguration)
                .transactionAware()
                .initialCacheNames(cacheNames) // 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
                .withInitialCacheConfigurations(configMap)
                .build();
    }
}
