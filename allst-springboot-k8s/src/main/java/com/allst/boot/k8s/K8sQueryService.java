package com.allst.boot.k8s;

import com.allst.boot.k8s.model.ClientStatus;
import com.allst.boot.k8s.model.ClusterView;
import com.allst.boot.k8s.model.ConfigMapView;
import com.allst.boot.k8s.model.DeploymentView;
import com.allst.boot.k8s.model.NamespaceView;
import com.allst.boot.k8s.model.PodView;
import com.allst.boot.k8s.model.ServiceView;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 只读接口覆盖 Pod、Deployment、Service、ConfigMap、Namespace。
 * 会改集群的只有 ConfigMap 写入，并且只能写 {@code k8s.namespace}。
 */
@org.springframework.stereotype.Service
public class K8sQueryService {

    private static final Pattern DNS_LABEL = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
    private static final Pattern CONFIG_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,252}");
    private static final int MAX_LOG_LINES = 500;

    private final K8sClientProvider provider;

    public K8sQueryService(K8sClientProvider provider) {
        this.provider = provider;
    }

    public ClientStatus clientStatus() {
        KubernetesClient client = provider.client();
        if (client == null) {
            return new ClientStatus(false, provider.mode(), provider.namespace(), null, provider.error());
        }
        String masterUrl = null;
        try {
            masterUrl = client.getConfiguration().getMasterUrl();
        } catch (RuntimeException ex) {
            return new ClientStatus(false, provider.mode(), provider.namespace(), null, ex.getMessage());
        }
        return new ClientStatus(true, provider.mode(), provider.namespace(), masterUrl, null);
    }

    public ClusterView cluster() {
        KubernetesClient client = provider.require();
        VersionInfo version = client.getKubernetesVersion();
        return new ClusterView(
                version.getMajor(),
                version.getMinor(),
                version.getGitVersion(),
                version.getPlatform(),
                client.getConfiguration().getMasterUrl());
    }

    public List<NamespaceView> listNamespaces() {
        List<NamespaceView> views = new ArrayList<>();
        for (Namespace namespace : provider.require().namespaces().list().getItems()) {
            String phase = namespace.getStatus() == null ? null : namespace.getStatus().getPhase();
            String name = namespace.getMetadata() == null ? null : namespace.getMetadata().getName();
            views.add(new NamespaceView(name, phase));
        }
        return views;
    }

    public List<PodView> listPods(String namespace) {
        String resolved = namespace(namespace);
        List<PodView> views = new ArrayList<>();
        for (Pod pod : provider.require().pods().inNamespace(resolved).list().getItems()) {
            views.add(toPodView(pod));
        }
        return views;
    }

    public String podLog(String namespace, String name, Integer tailLines) {
        String resolved = namespace(namespace);
        requireWritableNamespace(resolved);
        requireDnsLabel(name, "Pod 名称");
        int lines = tailLines == null ? 100 : tailLines;
        if (lines < 1 || lines > MAX_LOG_LINES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tailLines 必须在 1 到 " + MAX_LOG_LINES + " 之间");
        }
        String log = provider.require().pods().inNamespace(resolved).withName(name).tailingLines(lines).getLog();
        return log == null ? "" : log;
    }

    public List<DeploymentView> listDeployments(String namespace) {
        String resolved = namespace(namespace);
        List<DeploymentView> views = new ArrayList<>();
        for (Deployment deployment : provider.require().apps().deployments().inNamespace(resolved).list().getItems()) {
            views.add(toDeploymentView(deployment));
        }
        return views;
    }

    public List<ServiceView> listServices(String namespace) {
        String resolved = namespace(namespace);
        List<ServiceView> views = new ArrayList<>();
        for (Service service : provider.require().services().inNamespace(resolved).list().getItems()) {
            views.add(toServiceView(service));
        }
        return views;
    }

    public List<ConfigMapView> listConfigMaps(String namespace) {
        String resolved = namespace(namespace);
        List<ConfigMapView> views = new ArrayList<>();
        for (ConfigMap configMap : provider.require().configMaps().inNamespace(resolved).list().getItems()) {
            views.add(toConfigMapView(configMap, false));
        }
        return views;
    }

    public ConfigMapView getConfigMap(String namespace, String name) {
        String resolved = namespace(namespace);
        requireDnsLabel(name, "ConfigMap 名称");
        ConfigMap configMap = provider.require().configMaps().inNamespace(resolved).withName(name).get();
        if (configMap == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ConfigMap 不存在: " + resolved + "/" + name);
        }
        return toConfigMapView(configMap, true);
    }

    public ConfigMapView upsertConfigMap(String namespace, String name, Map<String, String> data) {
        String resolved = namespace(namespace);
        requireWritableNamespace(resolved);
        requireDnsLabel(name, "ConfigMap 名称");
        validateData(data);
        ConfigMap desired = new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(resolved)
                    .addToLabels("app", "allst-springboot-k8s")
                .endMetadata()
                .withData(new LinkedHashMap<>(data))
                .build();
        ConfigMap saved = provider.require().configMaps()
                .inNamespace(resolved)
                .resource(desired)
                .createOr(NonDeletingOperation::update);
        return toConfigMapView(saved, true);
    }

    public boolean deleteConfigMap(String namespace, String name) {
        String resolved = namespace(namespace);
        requireWritableNamespace(resolved);
        requireDnsLabel(name, "ConfigMap 名称");
        List<StatusDetails> deleted = provider.require().configMaps().inNamespace(resolved).withName(name).delete();
        return deleted != null && !deleted.isEmpty();
    }

    private void requireWritableNamespace(String namespace) {
        if (!provider.namespace().equals(namespace)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "写操作和 Pod 日志只允许命名空间 " + provider.namespace());
        }
    }

    private String namespace(String requested) {
        String value = requested == null || requested.isBlank() ? provider.namespace() : requested.trim();
        requireDnsLabel(value, "命名空间");
        return value;
    }

    private static void requireDnsLabel(String value, String label) {
        if (value == null || value.length() > 63 || !DNS_LABEL.matcher(value).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不合法: " + value);
        }
    }

    private static void validateData(Map<String, String> data) {
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "data 不能为空");
        }
        if (data.size() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "data 最多 32 个键");
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key == null || !CONFIG_KEY.matcher(key).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConfigMap 键不合法: " + key);
            }
            String value = entry.getValue();
            if (value == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConfigMap 值不能为 null: " + key);
            }
            if (value.length() > 8192) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConfigMap 值过长: " + key);
            }
        }
    }

    static PodView toPodView(Pod pod) {
        String name = pod.getMetadata() == null ? null : pod.getMetadata().getName();
        String namespace = pod.getMetadata() == null ? null : pod.getMetadata().getNamespace();
        String phase = null;
        String podIp = null;
        String startTime = null;
        int ready = 0;
        int restarts = 0;
        if (pod.getStatus() != null) {
            phase = pod.getStatus().getPhase();
            podIp = pod.getStatus().getPodIP();
            startTime = pod.getStatus().getStartTime();
            List<ContainerStatus> statuses = pod.getStatus().getContainerStatuses();
            if (statuses != null) {
                for (ContainerStatus status : statuses) {
                    if (Boolean.TRUE.equals(status.getReady())) {
                        ready++;
                    }
                    if (status.getRestartCount() != null) {
                        restarts += status.getRestartCount();
                    }
                }
            }
        }
        String nodeName = pod.getSpec() == null ? null : pod.getSpec().getNodeName();
        return new PodView(name, namespace, phase, nodeName, podIp, ready, restarts, startTime);
    }

    private static DeploymentView toDeploymentView(Deployment deployment) {
        String name = deployment.getMetadata() == null ? null : deployment.getMetadata().getName();
        String namespace = deployment.getMetadata() == null ? null : deployment.getMetadata().getNamespace();
        Integer replicas = null;
        Integer readyReplicas = null;
        Integer availableReplicas = null;
        if (deployment.getStatus() != null) {
            replicas = deployment.getStatus().getReplicas();
            readyReplicas = deployment.getStatus().getReadyReplicas();
            availableReplicas = deployment.getStatus().getAvailableReplicas();
        }
        if (replicas == null && deployment.getSpec() != null) {
            replicas = deployment.getSpec().getReplicas();
        }
        List<String> images = new ArrayList<>();
        if (deployment.getSpec() != null
                && deployment.getSpec().getTemplate() != null
                && deployment.getSpec().getTemplate().getSpec() != null
                && deployment.getSpec().getTemplate().getSpec().getContainers() != null) {
            for (Container container : deployment.getSpec().getTemplate().getSpec().getContainers()) {
                images.add(container.getImage());
            }
        }
        return new DeploymentView(name, namespace, replicas, readyReplicas, availableReplicas, images);
    }

    private static ServiceView toServiceView(Service service) {
        String name = service.getMetadata() == null ? null : service.getMetadata().getName();
        String namespace = service.getMetadata() == null ? null : service.getMetadata().getNamespace();
        String type = null;
        String clusterIp = null;
        List<ServiceView.PortView> ports = new ArrayList<>();
        if (service.getSpec() != null) {
            type = service.getSpec().getType();
            clusterIp = service.getSpec().getClusterIP();
            if (service.getSpec().getPorts() != null) {
                for (ServicePort port : service.getSpec().getPorts()) {
                    ports.add(new ServiceView.PortView(port.getPort(), targetPort(port), port.getProtocol()));
                }
            }
        }
        return new ServiceView(name, namespace, type, clusterIp, ports);
    }

    private static String targetPort(ServicePort port) {
        if (port.getTargetPort() == null) {
            return null;
        }
        if (port.getTargetPort().getIntVal() != null) {
            return String.valueOf(port.getTargetPort().getIntVal());
        }
        return port.getTargetPort().getStrVal();
    }

    private static ConfigMapView toConfigMapView(ConfigMap configMap, boolean includeData) {
        String name = configMap.getMetadata() == null ? null : configMap.getMetadata().getName();
        String namespace = configMap.getMetadata() == null ? null : configMap.getMetadata().getNamespace();
        Map<String, String> source = configMap.getData() == null
                ? new LinkedHashMap<>()
                : configMap.getData();
        List<String> keys = new ArrayList<>(source.keySet());
        Map<String, String> data = includeData ? new LinkedHashMap<>(source) : null;
        return new ConfigMapView(name, namespace, keys, data);
    }
}
