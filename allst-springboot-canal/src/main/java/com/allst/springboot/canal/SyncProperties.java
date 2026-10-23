package com.allst.springboot.canal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {

    private Redis redis = new Redis();
    private Elasticsearch elasticsearch = new Elasticsearch();
    private Mongodb mongodb = new Mongodb();

    @Data
    public static class Redis {
        private boolean enabled = true;
        private String keyPrefix = "canal";
    }

    @Data
    public static class Elasticsearch {
        private boolean enabled = true;
    }

    @Data
    public static class Mongodb {
        private boolean enabled = true;
    }
}
