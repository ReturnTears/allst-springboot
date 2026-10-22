package com.allst.springboot.crypto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 按字段上的 {@link Desensitize} 选择掩码规则。
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final DesensitizeType type;

    public DesensitizeSerializer() {
        this.type = null;
    }

    public DesensitizeSerializer(DesensitizeType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(type == null ? value : DesensitizeUtils.mask(value, type));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property == null) {
            return this;
        }
        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Desensitize.class);
        }
        if (annotation == null) {
            return this;
        }
        return new DesensitizeSerializer(annotation.value());
    }
}
