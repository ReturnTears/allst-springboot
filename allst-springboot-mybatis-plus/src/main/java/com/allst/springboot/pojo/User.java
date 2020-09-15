package com.allst.springboot.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对应数据库中主键的常用生成策略：
 * uuid、自增id、雪花算法、redis、zookeeper、
 *
 * 注解TableId(type = IdType.ID_WORKER)
 * IdType:auto、none、input、id_worker、uuid、id_worker_str
 * 数据库id自增
 * none: 表示未设置主键
 * input: 表示手动输入、需自己配置id
 * 默认的全局唯一id
 * 全局唯一id uuid
 * id_worker的字符串表示法
 *
 * @author YiYa
 * @since 2020-09-15 上午 12:53
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer age;
    private String email;
    // 字段添加填充内容
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
