package com.allst.springboot.extension.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 扩展点的设计说明。启动后通过 /advs/extension/catalog 查看。
 */
@Getter
@AllArgsConstructor
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
}
