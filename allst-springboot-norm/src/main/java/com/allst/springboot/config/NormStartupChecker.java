package com.allst.springboot.config;

import com.allst.springboot.crypto.AesKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 启动时把密钥交给 JPA 转换器，并拒绝明显不可用的配置。
 */
@Slf4j
@Component
public class NormStartupChecker {

    private final NormProperties properties;

    public NormStartupChecker(NormProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void check() {
        if (properties.getCrypto().getAesKey() == null || properties.getCrypto().getAesKey().isBlank()) {
            throw new IllegalStateException("norm.crypto.aes-key 不能为空");
        }
        AesKeys.set(properties.getCrypto().getAesKey());
        if (properties.getSign().isEnabled() && properties.getSign().getApps().isEmpty()) {
            throw new IllegalStateException("开启签名时必须配置 norm.sign.apps");
        }
        if (properties.getIp().isEnabled() && properties.getIp().getWhitelist().isEmpty()) {
            throw new IllegalStateException("开启 IP 白名单时列表不能为空");
        }
        if (properties.getPage().getMaxSize() < 1 || properties.getBatch().getMaxSize() < 1) {
            throw new IllegalStateException("分页和批量上限必须大于 0");
        }
        if (properties.isDevToolsEnabled()) {
            log.warn("开发辅助接口 /api/dev 已开启，可使用服务端密钥计算签名。生产环境必须设置 norm.dev-tools-enabled=false");
        }
    }
}
