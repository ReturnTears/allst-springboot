package com.allst.exercise.config;

import com.allst.exercise.anno.CustomersFormat;
import com.allst.exercise.model.Customers;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.util.HashSet;
import java.util.Set;

/**
 * 可以按字段类型或注解配置字段格式。要将注解绑定到格式化程序，需要实现AnnotationFormatterFactory接口。
 *
 * @author Hutu
 * @since 2024-07-28 上午 09:21
 */
public final class CustomersFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<CustomersFormat> {
    @Override
    public Set<Class<?>> getFieldTypes() {
        Set<Class<?>> types = new HashSet<>();
        types.add(Customers.class);
        return types;
    }

    @Override
    public Printer<?> getPrinter(CustomersFormat annotation, Class<?> fieldType) {
        return new CustomersFormatter();
    }

    @Override
    public Parser<?> getParser(CustomersFormat annotation, Class<?> fieldType) {
        return new CustomersFormatter();
    }

}
