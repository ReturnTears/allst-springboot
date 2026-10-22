package com.allst.springboot.validation;

import com.allst.springboot.annotation.MaxPageSize;
import com.allst.springboot.config.NormProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 读取配置中的分页上限，保证注解和配置是同一个数。
 */
@Component
public class MaxPageSizeValidator implements ConstraintValidator<MaxPageSize, Integer> {

    private int max = 50;

    @Autowired(required = false)
    public void setNormProperties(NormProperties properties) {
        if (properties != null) {
            this.max = properties.getPage().getMaxSize();
        }
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value >= 1 && value <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("每页条数需在 1 到 " + max + " 之间").addConstraintViolation();
        return false;
    }
}
