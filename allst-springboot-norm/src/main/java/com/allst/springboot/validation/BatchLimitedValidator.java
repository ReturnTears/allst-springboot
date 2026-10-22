package com.allst.springboot.validation;

import com.allst.springboot.annotation.BatchLimited;
import com.allst.springboot.config.NormProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collection;

/**
 * 读取配置中的批量上限。
 */
@Component
public class BatchLimitedValidator implements ConstraintValidator<BatchLimited, Collection<?>> {

    private int max = 100;

    @Autowired(required = false)
    public void setNormProperties(NormProperties properties) {
        if (properties != null) {
            this.max = properties.getBatch().getMaxSize();
        }
    }

    @Override
    public boolean isValid(Collection<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.size() <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("批量条数不能超过 " + max).addConstraintViolation();
        return false;
    }
}
