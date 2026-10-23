package com.allst.springboot.mq;

import com.allst.springboot.model.DataChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataChangePublisher {

    private final RabbitTemplate rabbitTemplate;

    public DataChangePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(List<DataChangeEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (DataChangeEvent event : events) {
            String routingKey = "canal.change." + token(event.getSchema()) + "." + token(event.getTable());
            rabbitTemplate.convertAndSend(RabbitNames.EXCHANGE, routingKey, event);
            log.info("已发布到 MQ, routingKey={}, event={}, pk={}, logfile={}@{}",
                    routingKey, event.getEventType(), event.getPrimaryValue(), event.getLogfile(), event.getLogfileOffset());
        }
    }

    private String token(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "_";
        }
        return raw.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
