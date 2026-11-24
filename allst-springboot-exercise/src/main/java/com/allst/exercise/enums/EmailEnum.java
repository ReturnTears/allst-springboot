package com.allst.exercise.enums;

/**
 * @author Hutu
 * @since 2024-07-28 上午 09:36
 */
public enum EmailEnum {
    QQ(1, "@qq.com"),
    Net(2, "@163.com"),
    GMAIL(3, "@gmail.com"),
    FOXMAIL(4, "@foxmail.com"),
    ALIYUN(5, "@aliyun.com"),
    ;

    private final Integer code;
    private final String suffix;

    EmailEnum(Integer code, String suffix) {
        this.code = code;
        this.suffix = suffix;
    }

    public Integer getCode() {
        return code;
    }

    public String getSuffix() {
        return suffix;
    }

    public static String getSuffixByCode(Integer code) {
        for (EmailEnum value : EmailEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getSuffix();
            }
        }
        return EmailEnum.QQ.getSuffix();
    }
}
