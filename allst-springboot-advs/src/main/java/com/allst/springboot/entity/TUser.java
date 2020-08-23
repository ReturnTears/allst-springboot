package com.allst.springboot.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.format.annotation.NumberFormat;

import javax.persistence.*;
import java.io.Serializable;

/**
 * TUser实体类， 使用JPA注解配置映射关系
 * @author YiYa
 * @since 2020-08-23 下午 02:01
 *
 * 备注: 如果报错如下
 * com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
 * 添加注解@JsonIgnoreProperties即可
 */
@Entity
@Table(name = "t_user")
@Data
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
public class TUser implements Serializable {
    @Id // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键
    private Integer id;

    @Column(name = "name", length = 20)
    private String name;

    /**
     * 报错转换器错误就需要添加如下注解， 其他数据类型的添加相应注解即可
     */
    @NumberFormat(style = NumberFormat.Style.CURRENCY)
    @Column(name = "age", length = 11)
    private Integer age;
    /**
     * Column 注解省略默认列名就是属性名
     */
    @Column(name = "email", length = 50)
    private String email;
}
