package com.allst.boot.k8s;

import com.allst.boot.config.K8sProperties;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * 持有已经建好的客户端。client 为空表示启动时没有可用的集群凭据，应用本身仍然可以运行。
 */
public final class K8sClientProvider implements AutoCloseable {

    private final KubernetesClient client;
    private final String mode;
    private final String error;
    private final K8sProperties properties;

    public K8sClientProvider(KubernetesClient client, String mode, String error, K8sProperties properties) {
        this.client = client;
        this.mode = mode;
        this.error = error;
        this.properties = properties;
    }

    public KubernetesClient client() {
        return client;
    }

    public KubernetesClient require() {
        if (client == null) {
            String message = error == null ? "Kubernetes 客户端未初始化" : error;
            throw new K8sUnavailableException(message);
        }
        return client;
    }

    public String mode() {
        return mode;
    }

    public String error() {
        return error;
    }

    public String namespace() {
        return properties.getNamespace();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
