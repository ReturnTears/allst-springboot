package com.allst.springboot.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author Hutu
 * @since 2024-10-07 下午 04:50
 */
@Target(ElementType.TYPE_PARAMETER)
public @interface What {
    String description() default "";
}
