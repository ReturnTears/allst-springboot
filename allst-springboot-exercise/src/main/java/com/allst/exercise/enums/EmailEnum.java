package com.allst.exercise.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Hutu
 * @since 2024-07-28 上午 09:36
 */
@Getter
@AllArgsConstructor
public enum EmailEnum {
    QQ(1, "@qq.com"),
    Net(2, "@163.com"),
    GMAIL(3, "@gmail.com"),
    FOXMAIL(4, "@foxmail.com"),
    ALIYUN(5, "@aliyun.com"),
    ;

    private final Integer code;
    private final String suffix;

    public static String getSuffixByCode(Integer code) {
        for (EmailEnum value : EmailEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getSuffix();
            }
        }
        return EmailEnum.QQ.getSuffix();
    }
}
