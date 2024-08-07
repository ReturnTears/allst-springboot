package com.allst.boot.enums;

/**
 * @author Hutu
 * @since 2024-08-07 下午 09:58
 */
public enum CustomerEnum {
    LOYAL, NEW, DISSATISFIED;

    public String getValue() {
        return this.toString();
    }
}
