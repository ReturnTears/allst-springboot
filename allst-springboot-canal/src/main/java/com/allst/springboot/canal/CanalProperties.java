package com.allst.springboot.canal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "canal.client")
public class CanalProperties {

    /** 关闭后应用只消费 MQ，不再连接 Canal Server。 */
    private boolean enabled = true;

    private String host = "127.0.0.1";
    private int port = 11111;
    private String destination = "example";
    private String username = "";
    private String password = "";
    private String subscribe = "demo\\..*";
    private int batchSize = 100;
    private long retryIntervalMs = 3000;
}
