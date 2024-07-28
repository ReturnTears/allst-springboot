package com.allst.exercise.anno;

import java.lang.annotation.*;

/**
 * email格式化注解
 *
 * @author Hutu
 * @since 2024-07-28 上午 09:20
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface EmailFormat {
}
