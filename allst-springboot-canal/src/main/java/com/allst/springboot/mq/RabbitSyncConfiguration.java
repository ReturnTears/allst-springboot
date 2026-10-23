package com.allst.springboot.mq;

import com.allst.springboot.canal.CanalProperties;
import com.allst.springboot.canal.SyncProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties({CanalProperties.class, SyncProperties.class})
public class RabbitSyncConfiguration {

    @Bean
    public TopicExchange canalSyncExchange() {
        return new TopicExchange(RabbitNames.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange canalDeadLetterExchange() {
        return new DirectExchange(RabbitNames.DLX, true, false);
    }

    @Bean
    public Queue redisQueue() {
        return workQueue(RabbitNames.REDIS_QUEUE, RabbitNames.REDIS_DLQ);
    }

    @Bean
    public Queue esQueue() {
        return workQueue(RabbitNames.ES_QUEUE, RabbitNames.ES_DLQ);
    }

    @Bean
    public Queue mongoQueue() {
        return workQueue(RabbitNames.MONGO_QUEUE, RabbitNames.MONGO_DLQ);
    }

    @Bean
    public Queue redisDeadQueue() {
        return QueueBuilder.durable(RabbitNames.REDIS_DLQ).build();
    }

    @Bean
    public Queue esDeadQueue() {
        return QueueBuilder.durable(RabbitNames.ES_DLQ).build();
    }

    @Bean
    public Queue mongoDeadQueue() {
        return QueueBuilder.durable(RabbitNames.MONGO_DLQ).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Binding redisBinding(Queue redisQueue, TopicExchange canalSyncExchange) {
        return BindingBuilder.bind(redisQueue).to(canalSyncExchange).with(RabbitNames.ROUTING_PATTERN);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Binding esBinding(Queue esQueue, TopicExchange canalSyncExchange) {
        return BindingBuilder.bind(esQueue).to(canalSyncExchange).with(RabbitNames.ROUTING_PATTERN);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.mongodb", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Binding mongoBinding(Queue mongoQueue, TopicExchange canalSyncExchange) {
        return BindingBuilder.bind(mongoQueue).to(canalSyncExchange).with(RabbitNames.ROUTING_PATTERN);
    }

    @Bean
    public Binding redisDeadBinding(Queue redisDeadQueue, DirectExchange canalDeadLetterExchange) {
        return BindingBuilder.bind(redisDeadQueue).to(canalDeadLetterExchange).with(RabbitNames.REDIS_DLQ);
    }

    @Bean
    public Binding esDeadBinding(Queue esDeadQueue, DirectExchange canalDeadLetterExchange) {
        return BindingBuilder.bind(esDeadQueue).to(canalDeadLetterExchange).with(RabbitNames.ES_DLQ);
    }

    @Bean
    public Binding mongoDeadBinding(Queue mongoDeadQueue, DirectExchange canalDeadLetterExchange) {
        return BindingBuilder.bind(mongoDeadQueue).to(canalDeadLetterExchange).with(RabbitNames.MONGO_DLQ);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(1);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 8000)
                .build());
        return factory;
    }

    private Queue workQueue(String name, String deadRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(RabbitNames.DLX)
                .deadLetterRoutingKey(deadRoutingKey)
                .build();
    }
}
