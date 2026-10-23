package com.allst.springboot.mq;

import com.allst.springboot.model.DataChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterLogger {

    @RabbitListener(queues = {RabbitNames.REDIS_DLQ, RabbitNames.ES_DLQ, RabbitNames.MONGO_DLQ})
    public void onDeadLetter(DataChangeEvent event, Message message) {
        log.error("同步失败进入死信队列, queue={}, messageId={}, event={}, pk={}, logfile={}@{}",
                message.getMessageProperties().getConsumerQueue(),
                event.getMessageId(),
                event.getEventType(),
                event.getPrimaryValue(),
                event.getLogfile(),
                event.getLogfileOffset());
    }
}
