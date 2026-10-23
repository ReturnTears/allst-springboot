package com.allst.boot.k8s.model;

public class ClusterView {

    private final String major;
    private final String minor;
    private final String gitVersion;
    private final String platform;
    private final String masterUrl;

    public ClusterView(String major, String minor, String gitVersion, String platform, String masterUrl) {
        this.major = major;
        this.minor = minor;
        this.gitVersion = gitVersion;
        this.platform = platform;
        this.masterUrl = masterUrl;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
    }

    public String getGitVersion() {
        return gitVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public String getMasterUrl() {
        return masterUrl;
    }
}
