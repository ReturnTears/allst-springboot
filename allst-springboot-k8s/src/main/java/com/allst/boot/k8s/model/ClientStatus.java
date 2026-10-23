package com.allst.boot.k8s.model;

public class ClientStatus {

    private final boolean clientReady;
    private final String mode;
    private final String namespace;
    private final String masterUrl;
    private final String message;

    public ClientStatus(boolean clientReady, String mode, String namespace, String masterUrl, String message) {
        this.clientReady = clientReady;
        this.mode = mode;
        this.namespace = namespace;
        this.masterUrl = masterUrl;
        this.message = message;
    }

    public boolean isClientReady() {
        return clientReady;
    }

    public String getMode() {
        return mode;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public String getMessage() {
        return message;
    }
}
