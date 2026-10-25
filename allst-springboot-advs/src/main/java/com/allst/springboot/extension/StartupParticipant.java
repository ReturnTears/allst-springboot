package com.allst.springboot.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要被实例化回调单独跟踪的示例 Bean。
 * 后置处理器只跟踪这些 Bean，避免改动 MyBatis、Druid、Redis 等已有组件的创建过程。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface StartupParticipant {
}
