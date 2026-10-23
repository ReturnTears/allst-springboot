package com.allst.springboot.sync.mongo;

import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.mq.RabbitNames;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.mongodb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MongoSyncConsumer {

    private final MongoSyncService mongoSyncService;

    public MongoSyncConsumer(MongoSyncService mongoSyncService) {
        this.mongoSyncService = mongoSyncService;
    }

    @RabbitListener(queues = RabbitNames.MONGO_QUEUE)
    public void onMessage(DataChangeEvent event) {
        mongoSyncService.apply(event);
    }
}
