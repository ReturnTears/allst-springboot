package com.allst.boot.model;

import com.allst.boot.enums.CustomerEnum;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-08-07 下午 09:57
 */
@Data
public class OrderBO {
    /**
     * 客户号
     */
    private String customerNumber;
    /**
     * 年龄
     */
    private Integer age;
    /**
     * 订单金额
     */
    private Integer amount;
    /**
     * 客户类型
     */
    private CustomerEnum customerEnum;
}
