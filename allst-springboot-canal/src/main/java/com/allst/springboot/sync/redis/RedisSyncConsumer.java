package com.allst.springboot.sync.redis;

import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.mq.RabbitNames;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisSyncConsumer {

    private final RedisSyncService redisSyncService;

    public RedisSyncConsumer(RedisSyncService redisSyncService) {
        this.redisSyncService = redisSyncService;
    }

    @RabbitListener(queues = RabbitNames.REDIS_QUEUE)
    public void onMessage(DataChangeEvent event) {
        redisSyncService.apply(event);
    }
}
