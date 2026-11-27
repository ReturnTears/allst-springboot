package com.allst.springboot.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义元数据对象处理器
 *
 * @author YiYa
 * @since 2020-09-16 上午 12:04
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    /**
     * 插入时的填充策略
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        System.out.println("start insert fill......");
        this.setFieldValByName("createTime", new Date(), metaObject);
    }

    /**
     * 更新时的填充策略
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        System.out.println("start update fill......");
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }
}
