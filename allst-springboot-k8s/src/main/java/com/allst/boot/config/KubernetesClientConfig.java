package com.allst.boot.config;

import com.allst.boot.k8s.K8sClientProvider;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 客户端创建方式和 fabric8io/kubernetes-client 的示例一致：
 * {@code new KubernetesClientBuilder().build()} 会自动选择集群内 ServiceAccount 或本地 kubeconfig。
 * 创建失败时不阻止应用启动，调用集群的接口会返回 503。
 */
@Configuration
public class KubernetesClientConfig {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Bean(destroyMethod = "close")
    public K8sClientProvider k8sClientProvider(K8sProperties properties) {
        String mode = clientMode(properties);
        try {
            KubernetesClient client = new KubernetesClientBuilder()
                    .withConfig(buildConfig(properties))
                    .build();
            log.info("Kubernetes client ready, mode={}, namespace={}", mode, properties.getNamespace());
            return new K8sClientProvider(client, mode, null, properties);
        } catch (RuntimeException ex) {
            log.warn("Kubernetes client was not created, mode={}: {}", mode, ex.getMessage());
            return new K8sClientProvider(null, mode, ex.getMessage(), properties);
        }
    }

    private static String clientMode(K8sProperties properties) {
        if (StringUtils.hasText(properties.getMasterUrl())) {
            return "master-url";
        }
        if (StringUtils.hasText(System.getenv("KUBERNETES_SERVICE_HOST"))) {
            return "in-cluster";
        }
        return "kubeconfig";
    }

    private static Config buildConfig(K8sProperties properties) {
        Config config = loadBaseConfig(properties);
        if (StringUtils.hasText(properties.getNamespace())) {
            config.setNamespace(properties.getNamespace());
        }
        if (properties.isTrustCerts()) {
            config.setTrustCerts(true);
        }
        if (StringUtils.hasText(properties.getMasterUrl())) {
            config.setMasterUrl(properties.getMasterUrl().trim());
        }
        return config;
    }

    private static Config loadBaseConfig(K8sProperties properties) {
        try {
            return Config.autoConfigure(null);
        } catch (RuntimeException ex) {
            if (!StringUtils.hasText(properties.getMasterUrl())) {
                throw ex;
            }
            return new ConfigBuilder().withMasterUrl(properties.getMasterUrl().trim()).build();
        }
    }
}
