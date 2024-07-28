package com.allst.exercise.anno;

import java.lang.annotation.*;

/**
 * @author Hutu
 * @since 2024-07-28 上午 11:10
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface CustomersFormat {
}
