package com.allst.exercise.config;

import com.allst.exercise.anno.EmailFormat;
import com.allst.exercise.enums.EmailEnum;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 可以按字段类型或注解配置字段格式。要将注解绑定到格式化程序，需要实现AnnotationFormatterFactory接口。
 *
 * @author Hutu
 * @since 2024-07-28 上午 09:21
 */
public class EmailFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<EmailFormat> {
    @Override
    public Set<Class<?>> getFieldTypes() {
        Set<Class<?>> types = new HashSet<>();
        types.add(String.class);
        return types;
    }

    @Override
    public Printer<?> getPrinter(EmailFormat annotation, Class<?> fieldType) {
        return new EmailFormatter();
    }

    @Override
    public Parser<?> getParser(EmailFormat annotation, Class<?> fieldType) {
        return new EmailFormatter();
    }

    private static class EmailFormatter implements Formatter<String> {

        @Override
        public String parse(String text, Locale locale) throws ParseException {
            if (text.trim().isEmpty()) {
                return "name@qq.com";
            }
            String[] s = text.split("_");
            return s[1] + EmailEnum.getSuffixByCode(Integer.valueOf(s[0]));
        }

        @Override
        public String print(String object, Locale locale) {
            return object;
        }
    }
}
