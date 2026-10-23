package com.allst.boot.k8s.model;

import java.util.List;

public class ServiceView {

    private final String name;
    private final String namespace;
    private final String type;
    private final String clusterIp;
    private final List<PortView> ports;

    public ServiceView(String name, String namespace, String type, String clusterIp, List<PortView> ports) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
        this.clusterIp = clusterIp;
        this.ports = ports;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getType() {
        return type;
    }

    public String getClusterIp() {
        return clusterIp;
    }

    public List<PortView> getPorts() {
        return ports;
    }

    public static class PortView {

        private final Integer port;
        private final String targetPort;
        private final String protocol;

        public PortView(Integer port, String targetPort, String protocol) {
            this.port = port;
            this.targetPort = targetPort;
            this.protocol = protocol;
        }

        public Integer getPort() {
            return port;
        }

        public String getTargetPort() {
            return targetPort;
        }

        public String getProtocol() {
            return protocol;
        }
    }
}
