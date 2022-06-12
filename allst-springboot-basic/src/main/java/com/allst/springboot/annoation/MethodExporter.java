package com.allst.springboot.annoation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解, 它是一个标识符，不需要有任何实现
 *
 * @author June
 * @since 2022-06-12
 */
@Target(ElementType.METHOD) // 表示注解用在方法上
@Retention(RetentionPolicy.RUNTIME) // 表示定义被它所注解的注解保留多久，RUNTIME表示运行时
public @interface MethodExporter {
}
