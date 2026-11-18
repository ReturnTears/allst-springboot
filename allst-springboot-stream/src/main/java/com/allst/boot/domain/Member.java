package com.allst.boot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会员。用于演示「订单只有 memberId，要补会员信息」这类跨集合关联，
 * 也就是 toMap 建索引替代嵌套循环 / 逐条查库。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private Long id;
    private String name;
    private String level;
    private String city;
    private int points;
}