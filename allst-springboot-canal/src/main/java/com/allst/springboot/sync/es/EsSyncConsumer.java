package com.allst.springboot.sync.es;

import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.mq.RabbitNames;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EsSyncConsumer {

    private final EsSyncService esSyncService;

    public EsSyncConsumer(EsSyncService esSyncService) {
        this.esSyncService = esSyncService;
    }

    @RabbitListener(queues = RabbitNames.ES_QUEUE)
    public void onMessage(DataChangeEvent event) {
        esSyncService.apply(event);
    }
}
