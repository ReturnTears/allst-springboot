package com.allst.springboot.extension.catalog;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * 目录对象通过 FactoryBean 生产，调用方只依赖 {@link ExtensionCatalog}。
 */
@Component
public class AdvsExtensionCatalogFactoryBean implements FactoryBean<ExtensionCatalog> {

    @Override
    public ExtensionCatalog getObject() {
        ExtensionCatalog catalog = ExtensionCatalog.create();
        StartupTraceLog.record(StartupPhase.FACTORY_BEAN,
                "组装 ExtensionCatalog，条目 " + catalog.getPoints().size()
                        + "。注入 ExtensionCatalog 得到的是产品对象，FactoryBean 自身的名字要加 &。");
        return catalog;
    }

    @Override
    public Class<?> getObjectType() {
        return ExtensionCatalog.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
