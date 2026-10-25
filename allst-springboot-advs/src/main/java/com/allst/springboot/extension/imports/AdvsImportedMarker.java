package com.allst.springboot.extension.imports;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 导入模块标记。标成组件后，IDE 能识别构造器注入。
 * 扫描包外的 {@code AdvsImportedConfiguration} 仍由 ImportSelector 装入。
 */
@Component
public class AdvsImportedMarker {

    private final String message;

    public AdvsImportedMarker(@Value("${advs.extension.banner:ALLST Spring Extension Points}") String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
