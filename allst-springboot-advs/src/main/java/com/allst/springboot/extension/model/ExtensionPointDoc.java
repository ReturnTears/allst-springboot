package com.allst.springboot.extension.model;

/**
 * 扩展点的设计说明。启动后通过 /advs/extension/catalog 查看。
 */
public class ExtensionPointDoc {

    private final int order;
    private final String pathKind;
    private final String phase;
    private final String extensionPoint;
    private final String implementation;
    private final String registration;
    private final String timing;
    private final String capabilities;
    private final String projectScenario;

    public ExtensionPointDoc(int order, String pathKind, String phase, String extensionPoint, String implementation,
                             String registration, String timing, String capabilities, String projectScenario) {
        this.order = order;
        this.pathKind = pathKind;
        this.phase = phase;
        this.extensionPoint = extensionPoint;
        this.implementation = implementation;
        this.registration = registration;
        this.timing = timing;
        this.capabilities = capabilities;
        this.projectScenario = projectScenario;
    }

    public int getOrder() {
        return order;
    }

    public String getPathKind() {
        return pathKind;
    }

    public String getPhase() {
        return phase;
    }

    public String getExtensionPoint() {
        return extensionPoint;
    }

    public String getImplementation() {
        return implementation;
    }

    public String getRegistration() {
        return registration;
    }

    public String getTiming() {
        return timing;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public String getProjectScenario() {
        return projectScenario;
    }
}
