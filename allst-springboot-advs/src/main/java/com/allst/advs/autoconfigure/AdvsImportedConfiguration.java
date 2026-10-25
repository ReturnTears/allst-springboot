package com.allst.advs.autoconfigure;

import com.allst.springboot.extension.imports.AdvsImportedMarker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 放在启动类扫描包之外。只有 {@code @EnableAdvsExtension} 的 ImportSelector 会把它装进容器。
 */
@Configuration
public class AdvsImportedConfiguration {

    @Bean
    public AdvsImportedMarker advsImportedMarker() {
        return new AdvsImportedMarker(
                "ImportSelector 把扫描范围外的模块装配进了容器，当前项目用它承载启动扩展说明。");
    }
}
