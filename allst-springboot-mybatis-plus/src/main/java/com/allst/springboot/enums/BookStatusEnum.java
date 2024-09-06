package com.allst.springboot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 1-上架，2-在管，3-借出，4-损坏，5-丢失，6-下架
 *
 * @author Hutu
 * @since 2024-09-06 下午 11:18
 */
@Getter
public enum BookStatusEnum {
    up_shelf(1, "上架"),
    on_history(2, "在馆"),
    out_borrow(3, "借出"),
    damaged(4, "损坏"),
    lost(5, "丢失"),
    down_shelf(6, "下架");

    @EnumValue
    private final Integer status;
    private final String message;

    BookStatusEnum(Integer status, String message) {
        this.status = status;
        this.message = message;
    }
}
