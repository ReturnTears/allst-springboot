package com.allst.springboot.crypto;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * 入库加密、读出解密。实体在内存中是明文，接口展示前再做脱敏。
 */
@Converter
public class AesStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return AesCipher.encrypt(attribute, AesKeys.get());
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return AesCipher.decrypt(dbData, AesKeys.get());
    }
}
