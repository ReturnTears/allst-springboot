package com.allst.springboot.extension.imports;

/**
 * 由注册器动态放入容器的说明对象，用来证明 ImportBeanDefinitionRegistrar 生效。
 */
public class AdvsDynamicNotice {

    public String getMessage() {
        return "ImportBeanDefinitionRegistrar 在配置类解析完成后注册了这个 Bean，可按模块开关装配可选能力。";
    }
}
