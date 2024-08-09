package com.allst.boot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Hutu
 * @since 2024-08-09 下午 10:37
 */
@Data
@Component
public class BinLogConfig {
    @Value("${binlog.datasource.host}")
    private String host;

    @Value("${binlog.datasource.port}")
    private int port;

    @Value("${binlog.datasource.username}")
    private String username;

    @Value("${binlog.datasource.passwd}")
    private String passwd;

    @Value("${binlog.db}")
    private String db;

    @Value("${binlog.table}")
    private String table;

    public static final int consumerThreads = 5;

    public static final long queueSleep = 1000;
}
