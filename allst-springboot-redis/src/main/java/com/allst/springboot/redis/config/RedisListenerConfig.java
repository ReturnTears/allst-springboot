package com.allst.springboot.redis.config;

import com.allst.springboot.redis.expire.KeyExpireListener;
import com.allst.springboot.redis.pubsub.NotifyHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisListenerConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            NotifyHub notifyHub,
            KeyExpireListener keyExpireListener,
            @Value("${spring.redis.database:0}") int database) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        for (String channel : NotifyHub.CHANNELS) {
            container.addMessageListener(notifyHub, new ChannelTopic(channel));
        }
        container.addMessageListener(keyExpireListener, new PatternTopic("__keyevent@" + database + "__:expired"));
        return container;
    }
}
