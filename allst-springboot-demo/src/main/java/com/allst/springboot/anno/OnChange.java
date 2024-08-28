package com.allst.springboot.anno;

import java.lang.annotation.*;

/**
 * @author Hutu
 * @since 2024-08-28 下午 09:50
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnChange {
}
