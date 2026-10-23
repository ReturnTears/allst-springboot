package com.allst.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 客户端连接参数。默认值让本地没有集群时也能把应用启动起来。
 */
@Component
@ConfigurationProperties(prefix = "k8s")
public class K8sProperties {

    private String namespace = "allst-demo";

    private String masterUrl = "";

    private boolean trustCerts = false;

    public String getNamespace() {
        if (namespace == null || namespace.isBlank()) {
            return "allst-demo";
        }
        return namespace.trim();
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
    }

    public boolean isTrustCerts() {
        return trustCerts;
    }

    public void setTrustCerts(boolean trustCerts) {
        this.trustCerts = trustCerts;
    }
}
