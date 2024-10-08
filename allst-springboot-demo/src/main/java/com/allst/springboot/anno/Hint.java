package com.allst.springboot.anno;

import java.lang.annotation.*;

/**
 * @author Hutu
 * @since 2024-10-08 下午 11:17
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MyHints.class)
public @interface Hint {
    String value();
}
