package com.allst.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规范开关和阈值。密钥只作为本地演示默认值，生产用环境变量覆盖。
 */
@Data
@ConfigurationProperties(prefix = "norm")
public class NormProperties {

    /**
     * 为 true 时开放 /api/dev。该接口能用服务端密钥签名，生产必须关闭。
     */
    private boolean devToolsEnabled = true;

    private final Sign sign = new Sign();
    private final Ip ip = new Ip();
    private final Crypto crypto = new Crypto();
    private final Page page = new Page();
    private final Batch batch = new Batch();
    private final Idempotent idempotent = new Idempotent();
    private final AccessLog accessLog = new AccessLog();
    private final Http http = new Http();

    @Data
    public static class Sign {
        private boolean enabled = true;
        private long expireSeconds = 300;
        private Map<String, String> apps = new LinkedHashMap<>();
    }

    @Data
    public static class Ip {
        private boolean enabled = true;
        /** 默认不信任转发头，避免调用方伪造 X-Forwarded-For 绕过白名单。 */
        private boolean trustForwarded = false;
        private List<String> whitelist = new ArrayList<>();
    }

    @Data
    public static class Crypto {
        private String aesKey = "";
    }

    @Data
    public static class Page {
        private int maxSize = 50;
        private int defaultSize = 10;
        private int maxPage = 1000;
    }

    @Data
    public static class Batch {
        private int maxSize = 100;
    }

    @Data
    public static class Idempotent {
        private long processingTimeoutSeconds = 120;
    }

    @Data
    public static class AccessLog {
        private boolean persist = true;
        private int maxBodyLength = 2000;
        private List<String> excludePaths = new ArrayList<>();
    }

    @Data
    public static class Http {
        private int maxBodyBytes = 1048576;
    }
}
