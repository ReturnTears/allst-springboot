package com.allst.springboot.extension.imports;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 打开启动扩展演示模块。
 * ImportSelector 负责装载扫描包之外的配置，Registrar 负责按定义注册 Bean。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({AdvsExtensionImportSelector.class, AdvsExtensionRegistrar.class})
public @interface EnableAdvsExtension {
}
