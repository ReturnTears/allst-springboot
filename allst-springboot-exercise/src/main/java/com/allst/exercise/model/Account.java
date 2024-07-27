package com.allst.exercise.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.util.Date;

/**
 * @author Hutu
 * @since 2024-07-27 下午 10:20
 */
@Data
@Table("tb_account")
public class Account {
    // 标识主键为自增
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String userName;
    private Integer age;
    // 这里的局部格式化日期貌似不生效?to_do
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "GMT+8")
    private Date birthday;
}
