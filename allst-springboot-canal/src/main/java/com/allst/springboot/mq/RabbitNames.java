package com.allst.springboot.mq;

public final class RabbitNames {

    public static final String EXCHANGE = "canal.sync.exchange";
    public static final String DLX = "canal.sync.dlx";
    public static final String ROUTING_PATTERN = "canal.change.#";

    public static final String REDIS_QUEUE = "canal.sync.redis";
    public static final String ES_QUEUE = "canal.sync.es";
    public static final String MONGO_QUEUE = "canal.sync.mongo";

    public static final String REDIS_DLQ = "canal.sync.redis.dlq";
    public static final String ES_DLQ = "canal.sync.es.dlq";
    public static final String MONGO_DLQ = "canal.sync.mongo.dlq";

    private RabbitNames() {
    }
}
