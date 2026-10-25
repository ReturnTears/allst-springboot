package com.allst.springboot.extension.imports;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 在配置类解析阶段选择要导入的模块。
 * 目标配置位于 {@code com.allst.advs.autoconfigure}，不在启动类的组件扫描范围内。
 */
public class AdvsExtensionImportSelector implements ImportSelector {

    static final String IMPORTED_CONFIGURATION = "com.allst.advs.autoconfigure.AdvsImportedConfiguration";

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        StartupTraceLog.record(StartupPhase.IMPORT_SELECTOR,
                "selectImports 选择 " + IMPORTED_CONFIGURATION + "，来源注解="
                        + importingClassMetadata.getClassName());
        return new String[]{IMPORTED_CONFIGURATION};
    }
}
