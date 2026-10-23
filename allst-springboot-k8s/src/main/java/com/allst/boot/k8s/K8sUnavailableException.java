package com.allst.boot.k8s;

/**
 * 客户端没建起来。常见原因是本机没有 kubeconfig，并且也不在 Pod 里。
 */
public class K8sUnavailableException extends RuntimeException {

    public K8sUnavailableException(String message) {
        super(message);
    }
}
