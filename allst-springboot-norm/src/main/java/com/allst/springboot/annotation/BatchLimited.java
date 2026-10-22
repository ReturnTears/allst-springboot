package com.allst.springboot.annotation;

import com.allst.springboot.validation.BatchLimitedValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 集合长度不能超过配置 norm.batch.max-size。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BatchLimitedValidator.class)
public @interface BatchLimited {

    String message() default "批量条数超出限制";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
