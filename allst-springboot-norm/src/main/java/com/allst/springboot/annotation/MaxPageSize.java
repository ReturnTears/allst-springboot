package com.allst.springboot.annotation;

import com.allst.springboot.validation.MaxPageSizeValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 每页条数不能超过配置 norm.page.max-size。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxPageSizeValidator.class)
public @interface MaxPageSize {

    String message() default "每页条数超出限制";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
