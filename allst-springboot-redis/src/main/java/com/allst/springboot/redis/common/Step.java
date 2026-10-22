package com.allst.springboot.redis.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据结构示例里的一步：实际执行的命令、这一步在教什么、Redis 返回值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step {
    private String command;
    private String comment;
    private Object result;
}
