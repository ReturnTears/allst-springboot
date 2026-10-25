package com.allst.springboot.extension.imports;

import org.springframework.stereotype.Component;

/**
 * 说明对象。标成组件后，IDE 能识别构造器注入。
 * 若定义还不存在，ImportBeanDefinitionRegistrar 会再补注册一次。
 */
@Component
public class AdvsDynamicNotice {

    public String getMessage() {
        return "ImportBeanDefinitionRegistrar 在配置类解析完成后注册了这个 Bean，可按模块开关装配可选能力。";
    }
}
