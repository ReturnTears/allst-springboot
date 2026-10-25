package com.allst.advs.autoconfigure;

import org.springframework.context.annotation.Configuration;

/**
 * 放在启动类扫描包之外。只有 {@code @EnableAdvsExtension} 的 ImportSelector 会把它装进容器。
 * 标记对象本身在扫描包内声明为组件，避免和这里再定义一次导致重复 Bean。
 */
@Configuration
public class AdvsImportedConfiguration {
}
